package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.TipoRecordatorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Textos de recordatorio y formato común de importes y fechas. */
public final class PlantillasMensaje {
    private PlantillasMensaje() { }

    private static final Locale PERU = new Locale("es", "PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", PERU);

    public static final String PREVENTIVO =
            "Hola {nombre}, le recordamos cordialmente que el pago de {concepto} del inmueble {inmueble}, "
            + "por {monto}, vence el {vencimiento}. Si ya realizó el pago, por favor ignore este mensaje. "
            + "Atentamente, {arrendador}.";

    public static final String VENCIDO =
            "Hola {nombre}, le informamos que el pago de {concepto} del inmueble {inmueble} venció el "
            + "{vencimiento} y registra {dias} días de atraso. Monto pendiente: {monto} "
            + "(incluye mora estimada de {mora}). Agradecemos regularizarlo a la brevedad. "
            + "Atentamente, {arrendador}.";

    public static final String FIN_CONTRATO =
            "Hola {nombre}, su contrato {contrato} para el inmueble {inmueble} vence el {fecha_fin}. "
            + "Faltan {dias} días. Por favor, coordinemos oportunamente la renovación o la entrega del inmueble. "
            + "Atentamente, {arrendador}.";

    /** Reemplaza los marcadores y elimina cualquier marcador que no tenga dato. */
    public static String aplicar(String plantilla, Map<String, String> datos) {
        if (plantilla == null) return "";
        String salida = plantilla;
        if (datos != null) {
            for (Map.Entry<String, String> dato : datos.entrySet()) {
                salida = salida.replace("{" + dato.getKey() + "}", dato.getValue() == null ? "" : dato.getValue());
            }
        }
        return salida.replaceAll("\\{[^}]+}", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("[ \\t]+\\n", "\n")
                .trim();
    }

    public static String soles(BigDecimal importe) {
        BigDecimal valor = importe == null ? BigDecimal.ZERO : importe;
        return "S/ " + String.format(Locale.US, "%,.2f", valor);
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    /** Devuelve la plantilla configurada o la constante por defecto según el tipo. */
    public static String resolver(TipoRecordatorio tipo, String personalizada) {
        if (personalizada != null && !personalizada.isBlank()) return personalizada;
        return switch (tipo) {
            case PREVENTIVO -> PREVENTIVO;
            case VENCIDO -> VENCIDO;
            case FIN_CONTRATO -> FIN_CONTRATO;
        };
    }
}
