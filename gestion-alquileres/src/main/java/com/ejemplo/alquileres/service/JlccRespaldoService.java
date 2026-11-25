package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.AccionAuditoria;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

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

@Service
@RequiredArgsConstructor
public class JlccRespaldoService {
    private static final Logger JLCC_log = LoggerFactory.getLogger(JlccRespaldoService.class);
    private static final DateTimeFormatter JLCC_SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Set<String> JLCC_TABLAS_ESPERADAS =
            Set.of("inmueble", "persona", "contrato", "cuota", "pago", "secuencia");

    private final DataSource jlccDataSource;
    private final AppProperties jlccPropiedades;
    private final JlccPermisos jlccPermisos;
    private final JlccAuditoriaService jlccAuditoria;
    private final JlccSesionActual jlccSesion;
    private final JlccConfiguracionService jlccConfiguracion;
    private final Clock jlccClock;

    public record Respaldo(Path ruta, LocalDateTime fecha, long bytes) {
        public String nombre() {
            return ruta.getFileName().toString();
        }
    }

    public record Restauracion(Path destino, Path respaldoSeguridad) {}

    public Path jlccCrearRespaldo(Path jlccCarpeta) {
        jlccPermisos.jlccExigirAdmin();
        if (jlccCarpeta == null) throw new JlccNegocioException("Selecciona una carpeta para el respaldo.");
        Path jlccDestino = jlccCrearArchivo(jlccCarpeta, "alquileres");
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.RESPALDO,
                "BaseDatos",
                null,
                "Respaldo manual: " + jlccDestino.getFileName());
        return jlccDestino;
    }

    public Path jlccRespaldoAutomatico() {
        Path jlccCarpeta = jlccRutaRespaldos();
        Path jlccDestino = jlccCrearArchivo(jlccCarpeta, "alquileres");
        jlccRotar(jlccCarpeta, 10);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.RESPALDO,
                "BaseDatos",
                null,
                "Respaldo automático: " + jlccDestino.getFileName());
        return jlccDestino;
    }

    public List<Respaldo> jlccListarAutomaticos() {
        jlccPermisos.jlccExigirAdmin();
        Path jlccCarpeta = jlccRutaRespaldos();
        if (!Files.isDirectory(jlccCarpeta)) return List.of();
        try (var jlccFlujo = Files.list(jlccCarpeta)) {
            List<Respaldo> jlccLista = new ArrayList<>();
            jlccFlujo.filter(Files::isRegularFile)
                    .filter(
                            ruta ->
                                    ruta.getFileName().toString().startsWith("alquileres-")
                                            && ruta.getFileName().toString().endsWith(".db"))
                    .forEach(
                            ruta -> {
                                try {
                                    LocalDateTime jlccFecha =
                                            LocalDateTime.ofInstant(
                                                    Files.getLastModifiedTime(ruta).toInstant(),
                                                    jlccClock.getZone());
                                    jlccLista.add(new Respaldo(ruta, jlccFecha, Files.size(ruta)));
                                } catch (IOException jlccIgnorada) {
                                }
                            });
            jlccLista.sort(Comparator.comparing(Respaldo::fecha).reversed());
            return jlccLista;
        } catch (IOException e) {
            throw new JlccNegocioException("No se pudo leer la carpeta de respaldos.");
        }
    }

    public Path jlccRutaRespaldos() {
        return jlccRutaDatos().resolve("respaldos");
    }

    public Restauracion jlccRestaurar(Path jlccArchivo) {
        jlccPermisos.jlccExigirAdmin();
        jlccValidar(jlccArchivo);
        Path jlccCarpeta = jlccRutaRespaldos();
        Path jlccSeguridad = jlccCrearArchivo(jlccCarpeta, "seguridad");
        Path jlccDestino = jlccArchivoBase();
        try {
            if (jlccDataSource instanceof HikariDataSource jlccHikari) jlccHikari.close();
            Files.createDirectories(jlccDestino.getParent());
            Files.copy(jlccArchivo, jlccDestino, StandardCopyOption.REPLACE_EXISTING);
            jlccConfiguracion.jlccInvalidarCache();
            jlccInsertarEventoRestauracion(jlccDestino, jlccArchivo.getFileName().toString());
            JLCC_log.info("Base de datos restaurada desde {}", jlccArchivo);
            return new Restauracion(jlccDestino, jlccSeguridad);
        } catch (IOException e) {
            throw new JlccNegocioException(
                    "No se pudo sustituir la base de datos. Se conserva el respaldo de seguridad.");
        }
    }

    public void jlccValidar(Path jlccArchivo) {
        if (jlccArchivo == null
                || !Files.isRegularFile(jlccArchivo)
                || !jlccArchivo.toString().toLowerCase().endsWith(".db")) {
            throw new JlccNegocioException("Selecciona un archivo de respaldo .db.");
        }
        try {
            if (Files.size(jlccArchivo) < 100)
                throw new JlccNegocioException("El archivo de respaldo está vacío o incompleto.");
            if (!jlccCabeceraSqlite(jlccArchivo))
                throw new JlccNegocioException("El archivo no es una base de datos SQLite válida.");
            SQLiteConfig jlccConfig = new SQLiteConfig();
            jlccConfig.setReadOnly(true);
            SQLiteDataSource jlccFuente = new SQLiteDataSource(jlccConfig);
            jlccFuente.setUrl("jdbc:sqlite:" + jlccArchivo.toAbsolutePath());
            try (Connection jlccConexion = jlccFuente.getConnection()) {
                Set<String> jlccTablas = new HashSet<>();
                try (Statement jlccSentencia = jlccConexion.createStatement();
                        ResultSet jlccFilas =
                                jlccSentencia.executeQuery(
                                        "select name from sqlite_master where type = 'table'")) {
                    while (jlccFilas.next()) jlccTablas.add(jlccFilas.getString(1).toLowerCase());
                }
                if (!jlccTablas.containsAll(JLCC_TABLAS_ESPERADAS)) {
                    throw new JlccNegocioException(
                            "El archivo no contiene las tablas esperadas de alquileres.");
                }
                try (Statement jlccSentencia = jlccConexion.createStatement();
                        ResultSet jlccFilas = jlccSentencia.executeQuery("pragma integrity_check")) {
                    if (!jlccFilas.next() || !"ok".equalsIgnoreCase(jlccFilas.getString(1))) {
                        throw new JlccNegocioException(
                                "La base de datos del respaldo no supera la verificación de"
                                    + " integridad.");
                    }
                }
            }
        } catch (JlccNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new JlccNegocioException("No se pudo leer el archivo de respaldo.");
        }
    }

    private Path jlccCrearArchivo(Path jlccCarpeta, String jlccPrefijo) {
        try {
            Files.createDirectories(jlccCarpeta);
            String jlccBase = jlccPrefijo + "-" + LocalDateTime.now(jlccClock).format(JLCC_SELLO);
            Path jlccDestino = jlccCarpeta.resolve(jlccBase + ".db");
            int jlccConsecutivo = 1;
            while (Files.exists(jlccDestino)) {
                jlccDestino = jlccCarpeta.resolve(jlccBase + "-" + jlccConsecutivo++ + ".db");
            }
            jlccVaciarHacia(jlccDestino);
            return jlccDestino;
        } catch (IOException e) {
            throw new JlccNegocioException("No se pudo crear el respaldo en la carpeta indicada.");
        }
    }

    private void jlccVaciarHacia(Path jlccDestino) {
        String jlccRuta = jlccDestino.toAbsolutePath().toString().replace("'", "''");
        try (Connection jlccConexion = jlccDataSource.getConnection();
                Statement jlccSentencia = jlccConexion.createStatement()) {
            jlccSentencia.execute("VACUUM INTO '" + jlccRuta + "'");
        } catch (Exception e) {
            try {
                Files.copy(jlccArchivoBase(), jlccDestino, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException jlccCopia) {
                throw new JlccNegocioException("No se pudo generar el respaldo de la base de datos.");
            }
        }
    }

    private void jlccRotar(Path jlccCarpeta, int jlccMaximo) {
        if (!Files.isDirectory(jlccCarpeta)) return;
        try (var jlccFlujo = Files.list(jlccCarpeta)) {
            List<Path> jlccArchivos =
                    jlccFlujo.filter(Files::isRegularFile)
                            .filter(
                                    ruta ->
                                            ruta.getFileName().toString().startsWith("alquileres-")
                                                    && ruta.getFileName()
                                                            .toString()
                                                            .endsWith(".db"))
                            .sorted(Comparator.comparingLong(this::jlccModificado))
                            .toList();
            for (int i = 0; i < jlccArchivos.size() - jlccMaximo; i++) {
                Files.deleteIfExists(jlccArchivos.get(i));
            }
        } catch (IOException e) {
            JLCC_log.warn("No se pudo rotar los respaldos automáticos.", e);
        }
    }

    private long jlccModificado(Path jlccRuta) {
        try {
            return Files.getLastModifiedTime(jlccRuta).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private boolean jlccCabeceraSqlite(Path jlccArchivo) throws IOException {
        byte[] jlccEsperado = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
        try (InputStream jlccEntrada = Files.newInputStream(jlccArchivo)) {
            byte[] jlccCabecera = jlccEntrada.readNBytes(jlccEsperado.length);
            if (jlccCabecera.length != jlccEsperado.length) return false;
            for (int i = 0; i < jlccEsperado.length; i++) if (jlccCabecera[i] != jlccEsperado[i]) return false;
            return true;
        }
    }

    private void jlccInsertarEventoRestauracion(Path jlccBase, String jlccArchivo) {
        try (Connection jlccConexion =
                        java.sql.DriverManager.getConnection(
                                "jdbc:sqlite:" + jlccBase.toAbsolutePath());
                PreparedStatement jlccSentencia =
                        jlccConexion.prepareStatement(
                                "insert into auditoria_evento (fecha, usuario, accion, entidad,"
                                    + " entidad_id, detalle) values (?,?,?,?,?,?)")) {
            jlccSentencia.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now(jlccClock)));
            jlccSentencia.setString(2, jlccSesion.jlccUsername() == null ? "sistema" : jlccSesion.jlccUsername());
            jlccSentencia.setString(3, AccionAuditoria.RESTAURACION.name());
            jlccSentencia.setString(4, "BaseDatos");
            jlccSentencia.setObject(5, null);
            jlccSentencia.setString(6, "Restauración desde " + jlccArchivo);
            jlccSentencia.executeUpdate();
        } catch (SQLException e) {
            JLCC_log.warn("No se pudo registrar la restauración en la bitácora restaurada.", e);
        }
    }

    private Path jlccRutaDatos() {
        String jlccConfigurado = jlccPropiedades.getDataDir();
        return Path.of(jlccConfigurado == null || jlccConfigurado.isBlank() ? "." : jlccConfigurado)
                .toAbsolutePath();
    }

    public Path jlccArchivoBase() {
        return jlccRutaDatos().resolve("alquileres.db");
    }
}
