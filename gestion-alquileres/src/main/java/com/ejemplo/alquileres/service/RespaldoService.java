package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.AccionAuditoria;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.springframework.stereotype.Service;

/** Respaldos consistentes con VACUUM INTO y restauración validada. */
@Service
@RequiredArgsConstructor
public class RespaldoService {
    private static final Logger log = LoggerFactory.getLogger(RespaldoService.class);
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Set<String> TABLAS_ESPERADAS = Set.of(
            "inmueble", "persona", "contrato", "cuota", "pago", "secuencia");

    private final DataSource dataSource;
    private final AppProperties propiedades;
    private final Permisos permisos;
    private final AuditoriaService auditoria;
    private final SesionActual sesion;
    private final ConfiguracionService configuracion;
    private final Clock clock;

    public record Respaldo(Path ruta, LocalDateTime fecha, long bytes) {
        public String nombre() { return ruta.getFileName().toString(); }
    }

    public record Restauracion(Path destino, Path respaldoSeguridad) { }

    /** Respaldo manual en la carpeta elegida por el usuario. */
    public Path crearRespaldo(Path carpeta) {
        permisos.exigirAdmin();
        if (carpeta == null) throw new NegocioException("Selecciona una carpeta para el respaldo.");
        Path destino = crearArchivo(carpeta, "alquileres");
        auditoria.registrar(AccionAuditoria.RESPALDO, "BaseDatos", null,
                "Respaldo manual: " + destino.getFileName());
        return destino;
    }

    /** Respaldo al cerrar la aplicación; conserva los últimos diez. */
    public Path respaldoAutomatico() {
        Path carpeta = rutaRespaldos();
        Path destino = crearArchivo(carpeta, "alquileres");
        rotar(carpeta, 10);
        auditoria.registrar(AccionAuditoria.RESPALDO, "BaseDatos", null,
                "Respaldo automático: " + destino.getFileName());
        return destino;
    }

    public List<Respaldo> listarAutomaticos() {
        permisos.exigirAdmin();
        Path carpeta = rutaRespaldos();
        if (!Files.isDirectory(carpeta)) return List.of();
        try (var flujo = Files.list(carpeta)) {
            List<Respaldo> lista = new ArrayList<>();
            flujo.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.getFileName().toString().startsWith("alquileres-")
                            && ruta.getFileName().toString().endsWith(".db"))
                    .forEach(ruta -> {
                        try {
                            LocalDateTime fecha = LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(ruta).toInstant(), clock.getZone());
                            lista.add(new Respaldo(ruta, fecha, Files.size(ruta)));
                        } catch (IOException ignorada) {
                            // Un archivo ilegible simplemente no se muestra.
                        }
                    });
            lista.sort(Comparator.comparing(Respaldo::fecha).reversed());
            return lista;
        } catch (IOException e) {
            throw new NegocioException("No se pudo leer la carpeta de respaldos.");
        }
    }

    public Path rutaRespaldos() { return rutaDatos().resolve("respaldos"); }

    /** Valida el archivo, guarda un respaldo de seguridad y sustituye la base actual. */
    public Restauracion restaurar(Path archivo) {
        permisos.exigirAdmin();
        validar(archivo);
        Path carpeta = rutaRespaldos();
        Path seguridad = crearArchivo(carpeta, "seguridad");
        Path destino = archivoBase();
        try {
            if (dataSource instanceof HikariDataSource hikari) hikari.close();
            Files.createDirectories(destino.getParent());
            Files.copy(archivo, destino, StandardCopyOption.REPLACE_EXISTING);
            configuracion.invalidarCache();
            insertarEventoRestauracion(destino, archivo.getFileName().toString());
            log.info("Base de datos restaurada desde {}", archivo);
            return new Restauracion(destino, seguridad);
        } catch (IOException e) {
            throw new NegocioException("No se pudo sustituir la base de datos. Se conserva el respaldo de seguridad.");
        }
    }

    /** Validación previa sin modificar nada; se expone para pruebas y para la UI. */
    public void validar(Path archivo) {
        if (archivo == null || !Files.isRegularFile(archivo) || !archivo.toString().toLowerCase().endsWith(".db")) {
            throw new NegocioException("Selecciona un archivo de respaldo .db.");
        }
        try {
            if (Files.size(archivo) < 100) throw new NegocioException("El archivo de respaldo está vacío o incompleto.");
            if (!cabeceraSqlite(archivo)) throw new NegocioException("El archivo no es una base de datos SQLite válida.");
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            SQLiteDataSource fuente = new SQLiteDataSource(config);
            fuente.setUrl("jdbc:sqlite:" + archivo.toAbsolutePath());
            try (Connection conexion = fuente.getConnection()) {
                Set<String> tablas = new HashSet<>();
                try (Statement sentencia = conexion.createStatement();
                     ResultSet filas = sentencia.executeQuery("select name from sqlite_master where type = 'table'")) {
                    while (filas.next()) tablas.add(filas.getString(1).toLowerCase());
                }
                if (!tablas.containsAll(TABLAS_ESPERADAS)) {
                    throw new NegocioException("El archivo no contiene las tablas esperadas de alquileres.");
                }
                try (Statement sentencia = conexion.createStatement();
                     ResultSet filas = sentencia.executeQuery("pragma integrity_check")) {
                    if (!filas.next() || !"ok".equalsIgnoreCase(filas.getString(1))) {
                        throw new NegocioException("La base de datos del respaldo no supera la verificación de integridad.");
                    }
                }
            }
        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new NegocioException("No se pudo leer el archivo de respaldo.");
        }
    }

    private Path crearArchivo(Path carpeta, String prefijo) {
        try {
            Files.createDirectories(carpeta);
            String base = prefijo + "-" + LocalDateTime.now(clock).format(SELLO);
            Path destino = carpeta.resolve(base + ".db");
            int consecutivo = 1;
            while (Files.exists(destino)) {
                destino = carpeta.resolve(base + "-" + consecutivo++ + ".db");
            }
            vaciarHacia(destino);
            return destino;
        } catch (IOException e) {
            throw new NegocioException("No se pudo crear el respaldo en la carpeta indicada.");
        }
    }

    private void vaciarHacia(Path destino) {
        String ruta = destino.toAbsolutePath().toString().replace("'", "''");
        try (Connection conexion = dataSource.getConnection(); Statement sentencia = conexion.createStatement()) {
            sentencia.execute("VACUUM INTO '" + ruta + "'");
        } catch (Exception e) {
            try {
                Files.copy(archivoBase(), destino, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException copia) {
                throw new NegocioException("No se pudo generar el respaldo de la base de datos.");
            }
        }
    }

    private void rotar(Path carpeta, int maximo) {
        if (!Files.isDirectory(carpeta)) return;
        try (var flujo = Files.list(carpeta)) {
            List<Path> archivos = flujo.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.getFileName().toString().startsWith("alquileres-")
                            && ruta.getFileName().toString().endsWith(".db"))
                    .sorted(Comparator.comparingLong(this::modificado))
                    .toList();
            for (int i = 0; i < archivos.size() - maximo; i++) {
                Files.deleteIfExists(archivos.get(i));
            }
        } catch (IOException e) {
            log.warn("No se pudo rotar los respaldos automáticos.", e);
        }
    }

    private long modificado(Path ruta) {
        try { return Files.getLastModifiedTime(ruta).toMillis(); }
        catch (IOException e) { return 0L; }
    }

    private boolean cabeceraSqlite(Path archivo) throws IOException {
        byte[] esperado = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
        try (InputStream entrada = Files.newInputStream(archivo)) {
            byte[] cabecera = entrada.readNBytes(esperado.length);
            if (cabecera.length != esperado.length) return false;
            for (int i = 0; i < esperado.length; i++) if (cabecera[i] != esperado[i]) return false;
            return true;
        }
    }

    private void insertarEventoRestauracion(Path base, String archivo) {
        try (Connection conexion = java.sql.DriverManager.getConnection("jdbc:sqlite:" + base.toAbsolutePath());
             PreparedStatement sentencia = conexion.prepareStatement(
                     "insert into auditoria_evento (fecha, usuario, accion, entidad, entidad_id, detalle) values (?,?,?,?,?,?)")) {
            sentencia.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now(clock)));
            sentencia.setString(2, sesion.username() == null ? "sistema" : sesion.username());
            sentencia.setString(3, AccionAuditoria.RESTAURACION.name());
            sentencia.setString(4, "BaseDatos");
            sentencia.setObject(5, null);
            sentencia.setString(6, "Restauración desde " + archivo);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            log.warn("No se pudo registrar la restauración en la bitácora restaurada.", e);
        }
    }

    private Path rutaDatos() {
        String configurado = propiedades.getDataDir();
        return Path.of(configurado == null || configurado.isBlank() ? "." : configurado).toAbsolutePath();
    }

    public Path archivoBase() { return rutaDatos().resolve("alquileres.db"); }
}
