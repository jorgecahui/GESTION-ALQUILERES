package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.model.TipoMora;
import com.ejemplo.alquileres.model.TipoRecordatorio;
import com.ejemplo.alquileres.repository.ConfiguracionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JlccConfiguracionService {
    private static final Set<String> JLCC_EXTENSIONES_LOGO = Set.of("png", "jpg", "jpeg");

    private final ConfiguracionRepository jlccRepository;
    private final AppProperties jlccPropiedades;
    private final JlccPermisos jlccPermisos;
    private final JlccAuditoriaService jlccAuditoria;
    private volatile Configuracion jlccCache;

    @Transactional
    public void jlccInicializar() {
        Configuracion jlccActual = jlccCache;
        if (jlccActual != null) return;
        jlccCache = jlccCopiar(jlccObtenerEntidad());
    }

    @Transactional
    public Configuracion jlccActual() {
        return jlccCopiar(jlccObtenerEntidad());
    }

    @Transactional
    public Configuracion jlccGuardar(Configuracion jlccCambios) {
        jlccPermisos.jlccExigirAdmin();
        if (jlccCambios == null)
            throw new JlccNegocioException("No hay cambios de configuración para guardar.");
        jlccValidar(jlccCambios);
        Configuracion jlccEntidad = jlccCopiar(jlccObtenerEntidad());
        jlccEntidad.setId(Configuracion.FILA_UNICA);
        jlccEntidad.setArrendadorRazonSocial(jlccLimpiar(jlccCambios.getArrendadorRazonSocial()));
        jlccEntidad.setArrendadorTipoDocumento(jlccMayusculas(jlccCambios.getArrendadorTipoDocumento(), "RUC"));
        jlccEntidad.setArrendadorDocumento(jlccLimpiar(jlccCambios.getArrendadorDocumento()));
        jlccEntidad.setArrendadorDireccion(jlccLimpiar(jlccCambios.getArrendadorDireccion()));
        jlccEntidad.setArrendadorTelefono(jlccLimpiar(jlccCambios.getArrendadorTelefono()));
        jlccEntidad.setArrendadorEmail(jlccLimpiar(jlccCambios.getArrendadorEmail()));
        jlccEntidad.setLogoArchivo(jlccLimpiar(jlccCambios.getLogoArchivo()));
        jlccEntidad.setReciboSerie(jlccMayusculas(jlccCambios.getReciboSerie(), "R001"));
        jlccEntidad.setReciboLeyenda(jlccLimpiar(jlccCambios.getReciboLeyenda()));
        jlccEntidad.setMoraTipo(
                jlccCambios.getMoraTipo() == null ? TipoMora.PORCENTAJE_DIARIO : jlccCambios.getMoraTipo());
        jlccEntidad.setMoraValor(
                jlccDineroNoNulo(jlccCambios.getMoraValor()).setScale(4, java.math.RoundingMode.HALF_UP));
        jlccEntidad.setMoraDiasGracia(jlccCambios.getMoraDiasGracia());
        jlccEntidad.setAlertasDiasVencimiento(jlccCambios.getAlertasDiasVencimiento());
        jlccEntidad.setAlertaRecordatorioDias(jlccCambios.getAlertaRecordatorioDias());
        jlccEntidad.setSemaforoUmbralLeveDias(jlccCambios.getSemaforoUmbralLeveDias());
        jlccEntidad.setSemaforoCuotasRojo(jlccCambios.getSemaforoCuotasRojo());
        jlccEntidad.setPlantillaPreventivo(jlccLimpiar(jlccCambios.getPlantillaPreventivo()));
        jlccEntidad.setPlantillaVencido(jlccLimpiar(jlccCambios.getPlantillaVencido()));
        jlccEntidad.setPlantillaFinContrato(jlccLimpiar(jlccCambios.getPlantillaFinContrato()));
        jlccEntidad.setInactividadMinutos(jlccCambios.getInactividadMinutos());
        Configuracion jlccGuardada = jlccRepository.save(jlccEntidad);
        jlccCache = jlccCopiar(jlccGuardada);
        jlccAuditoria.jlccRegistrar(
                com.ejemplo.alquileres.model.AccionAuditoria.CAMBIO_CONFIGURACION,
                "Configuracion",
                Configuracion.FILA_UNICA,
                "Se actualizaron los parámetros generales");
        return jlccCopiar(jlccGuardada);
    }

    public String jlccCopiarLogo(Path jlccOrigen) {
        jlccPermisos.jlccExigirAdmin();
        if (jlccOrigen == null || !Files.isRegularFile(jlccOrigen)) {
            throw new JlccNegocioException("Selecciona un archivo de imagen válido.");
        }
        String jlccNombre = jlccOrigen.getFileName().toString();
        int jlccPunto = jlccNombre.lastIndexOf('.');
        String jlccExtension = jlccPunto < 0 ? "" : jlccNombre.substring(jlccPunto + 1).toLowerCase(Locale.ROOT);
        if (!JLCC_EXTENSIONES_LOGO.contains(jlccExtension)) {
            throw new JlccNegocioException("El logo debe ser una imagen PNG o JPG.");
        }
        try {
            if (Files.size(jlccOrigen) > 5L * 1024 * 1024) {
                throw new JlccNegocioException("El logo no debe superar los 5 MB.");
            }
            Path jlccDestino = jlccRutaDatos().resolve("logo." + jlccExtension);
            Files.createDirectories(jlccDestino.getParent());
            Files.copy(jlccOrigen, jlccDestino, StandardCopyOption.REPLACE_EXISTING);
            return jlccDestino.getFileName().toString();
        } catch (IOException e) {
            throw new JlccNegocioException("No se pudo copiar el logo a la carpeta de datos.");
        }
    }

    @Transactional
    public Path jlccRutaLogo() {
        String jlccArchivo = jlccObtenerEntidad().getLogoArchivo();
        if (jlccArchivo == null || jlccArchivo.isBlank()) return null;
        Path jlccRuta = jlccRutaDatos().resolve(jlccArchivo);
        return Files.isRegularFile(jlccRuta) ? jlccRuta : null;
    }

    public Path jlccRutaDatos() {
        String jlccConfigurado = jlccPropiedades.getDataDir();
        return Path.of(jlccConfigurado == null || jlccConfigurado.isBlank() ? "." : jlccConfigurado)
                .toAbsolutePath();
    }

    public String jlccLeyendaRecibo() {
        String jlccLeyenda = jlccObtenerEntidad().getReciboLeyenda();
        return jlccLeyenda == null || jlccLeyenda.isBlank()
                ? "Documento interno sin validez tributaria"
                : jlccLeyenda;
    }

    public String jlccSerieRecibos() {
        return jlccMayusculas(jlccObtenerEntidad().getReciboSerie(), "R001");
    }

    public TipoMora jlccMoraTipo() {
        return jlccObtenerEntidad().getMoraTipo();
    }

    public BigDecimal jlccMoraValor() {
        return jlccDineroNoNulo(jlccObtenerEntidad().getMoraValor());
    }

    public int jlccMoraDiasGracia() {
        return jlccObtenerEntidad().getMoraDiasGracia();
    }

    public int jlccDiasVencimiento() {
        return jlccObtenerEntidad().getAlertasDiasVencimiento();
    }

    public int jlccDiasRecordatorio() {
        return jlccObtenerEntidad().getAlertaRecordatorioDias();
    }

    public int jlccUmbralLeveDias() {
        return jlccObtenerEntidad().getSemaforoUmbralLeveDias();
    }

    public int jlccCuotasRojo() {
        return jlccObtenerEntidad().getSemaforoCuotasRojo();
    }

    public int jlccInactividadMinutos() {
        return jlccObtenerEntidad().getInactividadMinutos();
    }

    public String jlccPlantilla(TipoRecordatorio jlccTipo) {
        Configuracion c = jlccObtenerEntidad();
        String jlccPersonalizada =
                switch (jlccTipo) {
                    case PREVENTIVO -> c.getPlantillaPreventivo();
                    case VENCIDO -> c.getPlantillaVencido();
                    case FIN_CONTRATO -> c.getPlantillaFinContrato();
                };
        return JlccPlantillasMensaje.jlccResolver(jlccTipo, jlccPersonalizada);
    }

    public void jlccInvalidarCache() {
        jlccCache = null;
    }

    private Configuracion jlccObtenerEntidad() {
        Configuracion jlccActual = jlccCache;
        if (jlccActual != null) return jlccActual;
        jlccActual = jlccRepository.findById(Configuracion.FILA_UNICA).orElseGet(this::jlccCrearPorDefecto);
        jlccCache = jlccActual;
        return jlccActual;
    }

    private Configuracion jlccCrearPorDefecto() {
        return jlccRepository.save(
                Configuracion.builder()
                        .id(Configuracion.FILA_UNICA)
                        .arrendadorRazonSocial(
                                jlccLimpiar(jlccPropiedades.getArrendador().getRazonSocial()))
                        .arrendadorTipoDocumento(
                                jlccMayusculas(jlccPropiedades.getArrendador().getTipoDocumento(), "RUC"))
                        .arrendadorDocumento(jlccLimpiar(jlccPropiedades.getArrendador().getDocumento()))
                        .arrendadorDireccion(jlccLimpiar(jlccPropiedades.getArrendador().getDireccion()))
                        .arrendadorTelefono(jlccLimpiar(jlccPropiedades.getArrendador().getTelefono()))
                        .arrendadorEmail(jlccLimpiar(jlccPropiedades.getArrendador().getEmail()))
                        .reciboSerie(jlccMayusculas(jlccPropiedades.getRecibos().getSerie(), "R001"))
                        .reciboLeyenda(jlccLimpiar(jlccPropiedades.getRecibos().getLeyenda()))
                        .moraTipo(jlccTipoMora(jlccPropiedades.getMora().getTipo()))
                        .moraValor(
                                jlccDineroNoNulo(jlccPropiedades.getMora().getValor())
                                        .setScale(4, java.math.RoundingMode.HALF_UP))
                        .moraDiasGracia(jlccPropiedades.getMora().getDiasGracia())
                        .alertasDiasVencimiento(jlccPropiedades.getAlertas().getDiasVencimiento())
                        .alertaRecordatorioDias(jlccPropiedades.getAlertas().getRecordatorioDias())
                        .semaforoUmbralLeveDias(jlccPropiedades.getMorosidad().getUmbralLeveDias())
                        .semaforoCuotasRojo(Math.max(1, jlccPropiedades.getMorosidad().getCuotasRojo()))
                        .plantillaPreventivo(
                                JlccPlantillasMensaje.jlccResolver(
                                        TipoRecordatorio.PREVENTIVO,
                                        jlccPropiedades.getPlantillas().getPreventivo()))
                        .plantillaVencido(
                                JlccPlantillasMensaje.jlccResolver(
                                        TipoRecordatorio.VENCIDO,
                                        jlccPropiedades.getPlantillas().getVencido()))
                        .plantillaFinContrato(
                                JlccPlantillasMensaje.jlccResolver(
                                        TipoRecordatorio.FIN_CONTRATO,
                                        jlccPropiedades.getPlantillas().getFinContrato()))
                        .inactividadMinutos(jlccPropiedades.getSeguridad().getInactividadMinutos())
                        .build());
    }

    private void jlccValidar(Configuracion c) {
        if (jlccVacio(c.getArrendadorRazonSocial()))
            throw new JlccNegocioException("La razón social del arrendador es obligatoria.");
        if (jlccVacio(c.getArrendadorDocumento()))
            throw new JlccNegocioException("El documento del arrendador es obligatorio.");
        String jlccSerie = jlccMayusculas(c.getReciboSerie(), "");
        if (!jlccSerie.matches("[A-Z0-9]{1,10}"))
            throw new JlccNegocioException("La serie del recibo debe tener hasta 10 letras o números.");
        if (c.getMoraValor() != null && c.getMoraValor().signum() < 0)
            throw new JlccNegocioException("El valor de la mora no puede ser negativo.");
        if (c.getMoraDiasGracia() < 0)
            throw new JlccNegocioException("Los días de gracia no pueden ser negativos.");
        if (c.getAlertasDiasVencimiento() < 0 || c.getAlertaRecordatorioDias() < 0)
            throw new JlccNegocioException("Los días de alerta no pueden ser negativos.");
        if (c.getSemaforoUmbralLeveDias() < 0)
            throw new JlccNegocioException("El umbral de atraso leve no puede ser negativo.");
        if (c.getSemaforoCuotasRojo() < 1)
            throw new JlccNegocioException("Debe existir al menos una cuota para marcar rojo.");
        if (c.getInactividadMinutos() < 0)
            throw new JlccNegocioException("Los minutos de inactividad no pueden ser negativos.");
    }

    private static TipoMora jlccTipoMora(String jlccValor) {
        try {
            return TipoMora.valueOf(jlccValor == null ? "" : jlccValor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return TipoMora.PORCENTAJE_DIARIO;
        }
    }

    private static Configuracion jlccCopiar(Configuracion c) {
        return Configuracion.builder()
                .id(c.getId())
                .arrendadorRazonSocial(c.getArrendadorRazonSocial())
                .arrendadorTipoDocumento(c.getArrendadorTipoDocumento())
                .arrendadorDocumento(c.getArrendadorDocumento())
                .arrendadorDireccion(c.getArrendadorDireccion())
                .arrendadorTelefono(c.getArrendadorTelefono())
                .arrendadorEmail(c.getArrendadorEmail())
                .logoArchivo(c.getLogoArchivo())
                .reciboSerie(c.getReciboSerie())
                .reciboLeyenda(c.getReciboLeyenda())
                .moraTipo(c.getMoraTipo())
                .moraValor(c.getMoraValor())
                .moraDiasGracia(c.getMoraDiasGracia())
                .alertasDiasVencimiento(c.getAlertasDiasVencimiento())
                .alertaRecordatorioDias(c.getAlertaRecordatorioDias())
                .semaforoUmbralLeveDias(c.getSemaforoUmbralLeveDias())
                .semaforoCuotasRojo(c.getSemaforoCuotasRojo())
                .plantillaPreventivo(c.getPlantillaPreventivo())
                .plantillaVencido(c.getPlantillaVencido())
                .plantillaFinContrato(c.getPlantillaFinContrato())
                .inactividadMinutos(c.getInactividadMinutos())
                .version(c.getVersion())
                .build();
    }

    private static String jlccLimpiar(String jlccValor) {
        return jlccValor == null ? null : jlccValor.trim();
    }

    private static boolean jlccVacio(String jlccValor) {
        return jlccValor == null || jlccValor.isBlank();
    }

    private static String jlccMayusculas(String jlccValor, String jlccDefecto) {
        String jlccTexto = jlccValor == null ? "" : jlccValor.trim().toUpperCase(Locale.ROOT);
        return jlccTexto.isBlank() ? jlccDefecto : jlccTexto;
    }

    private static BigDecimal jlccDineroNoNulo(BigDecimal jlccValor) {
        return jlccValor == null ? BigDecimal.ZERO : jlccValor;
    }
}
