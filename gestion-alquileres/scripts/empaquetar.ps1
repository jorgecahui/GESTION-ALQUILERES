# Empaqueta la aplicación como app-image y, si WiX está instalado, como instalador MSI.
param(
    [switch]$SoloAppImage
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Push-Location $raiz
try {
    Write-Host "Compilando y ejecutando pruebas..." -ForegroundColor Cyan
    & .\mvnw.cmd clean package
    if ($LASTEXITCODE -ne 0) { throw "Maven terminó con errores." }

    $jar = Join-Path $raiz "target\gestion-alquileres-1.0.0.jar"
    $paquete = Join-Path $raiz "target\paquete"
    $destino = Join-Path $raiz "target\instalador"
    if (-not (Test-Path $jar)) { throw "No se encontró el jar delgado en $jar" }
    New-Item -ItemType Directory -Force -Path $paquete | Out-Null
    New-Item -ItemType Directory -Force -Path $destino | Out-Null
    Copy-Item $jar $paquete -Force

    $jpackage = Get-Command jpackage -ErrorAction SilentlyContinue
    if (-not $jpackage) {
        throw "jpackage no está en PATH. Instala un JDK 17 o superior y vuelve a intentar."
    }

    $icono = Join-Path $raiz "src\main\resources\icons\app.ico"
    $argumentos = @(
        "--type", "app-image",
        "--name", "Gestión de Alquileres",
        "--app-version", "1.0.0",
        "--vendor", "Sistema de Gestión de Alquileres",
        "--input", $paquete,
        "--main-jar", "gestion-alquileres-1.0.0.jar",
        "--main-class", "com.ejemplo.alquileres.Launcher",
        "--dest", $destino,
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "-Dapp.data-dir=$env:USERPROFILE\.gestion-alquileres"
    )
    if (Test-Path $icono) { $argumentos += @("--icon", $icono) }

    $appImage = Join-Path $destino "Gestión de Alquileres"
    if (Test-Path $appImage) { Remove-Item -Recurse -Force $appImage }
    Write-Host "Generando app-image..." -ForegroundColor Cyan
    & jpackage @argumentos
    if ($LASTEXITCODE -ne 0) { throw "jpackage falló al generar el app-image." }
    $ejecutable = Join-Path $appImage "Gestión de Alquileres.exe"
    Write-Host "App-image listo: $appImage" -ForegroundColor Green
    Write-Host "Ejecutable: $ejecutable" -ForegroundColor Green

    if (-not $SoloAppImage) {
        $wix = Get-Command candle.exe -ErrorAction SilentlyContinue
        if (-not $wix) {
            Write-Host "WiX no está instalado: se omite el instalador MSI. Instala WiX Toolset y ejecuta de nuevo para obtenerlo." -ForegroundColor Yellow
        } else {
            Write-Host "Generando instalador MSI..." -ForegroundColor Cyan
            $msi = @(
                "--type", "msi",
                "--name", "Gestión de Alquileres",
                "--app-version", "1.0.0",
                "--vendor", "Sistema de Gestión de Alquileres",
                "--input", $paquete,
                "--main-jar", "gestion-alquileres-1.0.0.jar",
                "--main-class", "com.ejemplo.alquileres.Launcher",
                "--dest", $destino,
                "--win-menu",
                "--win-shortcut",
                "--win-dir-chooser",
                "--java-options", "-Dfile.encoding=UTF-8",
                "--java-options", "-Dapp.data-dir=$env:USERPROFILE\.gestion-alquileres"
            )
            if (Test-Path $icono) { $msi += @("--icon", $icono) }
            & jpackage @msi
            if ($LASTEXITCODE -ne 0) { throw "jpackage falló al generar el MSI." }
            Write-Host "Instalador MSI listo en $destino" -ForegroundColor Green
        }
    }
} finally {
    Pop-Location
}
