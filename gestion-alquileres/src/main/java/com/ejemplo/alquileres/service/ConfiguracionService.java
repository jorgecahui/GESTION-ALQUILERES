package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.model.TipoMora;
import com.ejemplo.alquileres.model.TipoRecordatorio;
import com.ejemplo.alquileres.repository.ConfiguracionRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fuente de verdad de los parámetros editables. Mantiene una copia en memoria
 * y la invalida cada vez que se guarda. Las propiedades de Spring solo se usan
 * para construir los valores iniciales de la fila única.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionService {
    private static final Set<String> EXTENSIONES_LOGO = Set.of("png", "jpg", "jpeg");

    private final ConfiguracionRepository repository;
    private final AppProperties propiedades;
    private final Permisos permisos;
    private final AuditoriaService auditoria;
    private volatile Configuracion cache;

    /** Crea la fila única si no existe; se ejecuta al arrancar la aplicación. */
    @Transactional
    public void inicializar() {
        Configuracion actual = cache;
        if (actual != null) return;
        cache = copiar(obtenerEntidad());
    }

    /** Devuelve una copia para que la UI no modifique la caché. */
    @Transactional
    public Configuracion actual() {
        return copiar(obtenerEntidad());
    }

    @Transactional
    public Configuracion guardar(Configuracion cambios) {
        permisos.exigirAdmin();
        if (cambios == null) throw new NegocioException("No hay cambios de configuración para guardar.");
        validar(cambios);
        Configuracion entidad = copiar(obtenerEntidad());
        entidad.setId(Configuracion.FILA_UNICA);
        entidad.setArrendadorRazonSocial(limpiar(cambios.getArrendadorRazonSocial()));
        entidad.setArrendadorTipoDocumento(mayusculas(cambios.getArrendadorTipoDocumento(), "RUC"));
        entidad.setArrendadorDocumento(limpiar(cambios.getArrendadorDocumento()));
        entidad.setArrendadorDireccion(limpiar(cambios.getArrendadorDireccion()));
        entidad.setArrendadorTelefono(limpiar(cambios.getArrendadorTelefono()));
        entidad.setArrendadorEmail(limpiar(cambios.getArrendadorEmail()));
        entidad.setLogoArchivo(limpiar(cambios.getLogoArchivo()));
        entidad.setReciboSerie(mayusculas(cambios.getReciboSerie(), "R001"));
        entidad.setReciboLeyenda(limpiar(cambios.getReciboLeyenda()));
        entidad.setMoraTipo(cambios.getMoraTipo() == null ? TipoMora.PORCENTAJE_DIARIO : cambios.getMoraTipo());
        entidad.setMoraValor(dineroNoNulo(cambios.getMoraValor()).setScale(4, java.math.RoundingMode.HALF_UP));
        entidad.setMoraDiasGracia(cambios.getMoraDiasGracia());
        entidad.setAlertasDiasVencimiento(cambios.getAlertasDiasVencimiento());
        entidad.setAlertaRecordatorioDias(cambios.getAlertaRecordatorioDias());
        entidad.setSemaforoUmbralLeveDias(cambios.getSemaforoUmbralLeveDias());
        entidad.setSemaforoCuotasRojo(cambios.getSemaforoCuotasRojo());
        entidad.setPlantillaPreventivo(limpiar(cambios.getPlantillaPreventivo()));
        entidad.setPlantillaVencido(limpiar(cambios.getPlantillaVencido()));
        entidad.setPlantillaFinContrato(limpiar(cambios.getPlantillaFinContrato()));
        entidad.setInactividadMinutos(cambios.getInactividadMinutos());
        Configuracion guardada = repository.save(entidad);
        cache = copiar(guardada);
        auditoria.registrar(com.ejemplo.alquileres.model.AccionAuditoria.CAMBIO_CONFIGURACION,
                "Configuracion", Configuracion.FILA_UNICA, "Se actualizaron los parámetros generales");
        return copiar(guardada);
    }

    /** Copia el archivo de imagen a la carpeta de datos y devuelve el nombre a guardar. */
    public String copiarLogo(Path origen) {
        permisos.exigirAdmin();
        if (origen == null || !Files.isRegularFile(origen)) {
            throw new NegocioException("Selecciona un archivo de imagen válido.");
        }
        String nombre = origen.getFileName().toString();
        int punto = nombre.lastIndexOf('.');
        String extension = punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES_LOGO.contains(extension)) {
            throw new NegocioException("El logo debe ser una imagen PNG o JPG.");
        }
        try {
            if (Files.size(origen) > 5L * 1024 * 1024) {
                throw new NegocioException("El logo no debe superar los 5 MB.");
            }
            Path destino = rutaDatos().resolve("logo." + extension);
            Files.createDirectories(destino.getParent());
            Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            return destino.getFileName().toString();
        } catch (IOException e) {
            throw new NegocioException("No se pudo copiar el logo a la carpeta de datos.");
        }
    }

    /** Ruta absoluta del logo configurado, o null si no existe. */
    @Transactional
    public Path rutaLogo() {
        String archivo = obtenerEntidad().getLogoArchivo();
        if (archivo == null || archivo.isBlank()) return null;
        Path ruta = rutaDatos().resolve(archivo);
        return Files.isRegularFile(ruta) ? ruta : null;
    }

    public Path rutaDatos() {
        String configurado = propiedades.getDataDir();
        return Path.of(configurado == null || configurado.isBlank() ? "." : configurado).toAbsolutePath();
    }

    public String leyendaRecibo() {
        String leyenda = obtenerEntidad().getReciboLeyenda();
        return leyenda == null || leyenda.isBlank() ? "Documento interno sin validez tributaria" : leyenda;
    }

    public String serieRecibos() { return mayusculas(obtenerEntidad().getReciboSerie(), "R001"); }
    public TipoMora moraTipo() { return obtenerEntidad().getMoraTipo(); }
    public BigDecimal moraValor() { return dineroNoNulo(obtenerEntidad().getMoraValor()); }
    public int moraDiasGracia() { return obtenerEntidad().getMoraDiasGracia(); }
    public int diasVencimiento() { return obtenerEntidad().getAlertasDiasVencimiento(); }
    public int diasRecordatorio() { return obtenerEntidad().getAlertaRecordatorioDias(); }
    public int umbralLeveDias() { return obtenerEntidad().getSemaforoUmbralLeveDias(); }
    public int cuotasRojo() { return obtenerEntidad().getSemaforoCuotasRojo(); }
    public int inactividadMinutos() { return obtenerEntidad().getInactividadMinutos(); }

    public String plantilla(TipoRecordatorio tipo) {
        Configuracion c = obtenerEntidad();
        String personalizada = switch (tipo) {
            case PREVENTIVO -> c.getPlantillaPreventivo();
            case VENCIDO -> c.getPlantillaVencido();
            case FIN_CONTRATO -> c.getPlantillaFinContrato();
        };
        return PlantillasMensaje.resolver(tipo, personalizada);
    }

    /** Invalida la caché; útil después de una restauración de base de datos. */
    public void invalidarCache() { cache = null; }

    private Configuracion obtenerEntidad() {
        Configuracion actual = cache;
        if (actual != null) return actual;
        actual = repository.findById(Configuracion.FILA_UNICA).orElseGet(this::crearPorDefecto);
        cache = actual;
        return actual;
    }

    private Configuracion crearPorDefecto() {
        return repository.save(Configuracion.builder()
                .id(Configuracion.FILA_UNICA)
                .arrendadorRazonSocial(limpiar(propiedades.getArrendador().getRazonSocial()))
                .arrendadorTipoDocumento(mayusculas(propiedades.getArrendador().getTipoDocumento(), "RUC"))
                .arrendadorDocumento(limpiar(propiedades.getArrendador().getDocumento()))
                .arrendadorDireccion(limpiar(propiedades.getArrendador().getDireccion()))
                .arrendadorTelefono(limpiar(propiedades.getArrendador().getTelefono()))
                .arrendadorEmail(limpiar(propiedades.getArrendador().getEmail()))
                .reciboSerie(mayusculas(propiedades.getRecibos().getSerie(), "R001"))
                .reciboLeyenda(limpiar(propiedades.getRecibos().getLeyenda()))
                .moraTipo(tipoMora(propiedades.getMora().getTipo()))
                .moraValor(dineroNoNulo(propiedades.getMora().getValor()).setScale(4, java.math.RoundingMode.HALF_UP))
                .moraDiasGracia(propiedades.getMora().getDiasGracia())
                .alertasDiasVencimiento(propiedades.getAlertas().getDiasVencimiento())
                .alertaRecordatorioDias(propiedades.getAlertas().getRecordatorioDias())
                .semaforoUmbralLeveDias(propiedades.getMorosidad().getUmbralLeveDias())
                .semaforoCuotasRojo(Math.max(1, propiedades.getMorosidad().getCuotasRojo()))
                .plantillaPreventivo(PlantillasMensaje.resolver(TipoRecordatorio.PREVENTIVO, propiedades.getPlantillas().getPreventivo()))
                .plantillaVencido(PlantillasMensaje.resolver(TipoRecordatorio.VENCIDO, propiedades.getPlantillas().getVencido()))
                .plantillaFinContrato(PlantillasMensaje.resolver(TipoRecordatorio.FIN_CONTRATO, propiedades.getPlantillas().getFinContrato()))
                .inactividadMinutos(propiedades.getSeguridad().getInactividadMinutos())
                .build());
    }

    private void validar(Configuracion c) {
        if (vacio(c.getArrendadorRazonSocial())) throw new NegocioException("La razón social del arrendador es obligatoria.");
        if (vacio(c.getArrendadorDocumento())) throw new NegocioException("El documento del arrendador es obligatorio.");
        String serie = mayusculas(c.getReciboSerie(), "");
        if (!serie.matches("[A-Z0-9]{1,10}")) throw new NegocioException("La serie del recibo debe tener hasta 10 letras o números.");
        if (c.getMoraValor() != null && c.getMoraValor().signum() < 0) throw new NegocioException("El valor de la mora no puede ser negativo.");
        if (c.getMoraDiasGracia() < 0) throw new NegocioException("Los días de gracia no pueden ser negativos.");
        if (c.getAlertasDiasVencimiento() < 0 || c.getAlertaRecordatorioDias() < 0) throw new NegocioException("Los días de alerta no pueden ser negativos.");
        if (c.getSemaforoUmbralLeveDias() < 0) throw new NegocioException("El umbral de atraso leve no puede ser negativo.");
        if (c.getSemaforoCuotasRojo() < 1) throw new NegocioException("Debe existir al menos una cuota para marcar rojo.");
        if (c.getInactividadMinutos() < 0) throw new NegocioException("Los minutos de inactividad no pueden ser negativos.");
    }

    private static TipoMora tipoMora(String valor) {
        try { return TipoMora.valueOf(valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return TipoMora.PORCENTAJE_DIARIO; }
    }

    private static Configuracion copiar(Configuracion c) {
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

    private static String limpiar(String valor) { return valor == null ? null : valor.trim(); }
    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private static String mayusculas(String valor, String defecto) {
        String texto = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        return texto.isBlank() ? defecto : texto;
    }
    private static BigDecimal dineroNoNulo(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
}
