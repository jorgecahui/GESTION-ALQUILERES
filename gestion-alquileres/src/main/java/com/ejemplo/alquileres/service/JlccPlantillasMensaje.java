package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.TipoRecordatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public final class JlccPlantillasMensaje {
    private JlccPlantillasMensaje() {}

    private static final Locale JLCC_PERU = new Locale("es", "PE");
    private static final DateTimeFormatter JLCC_FECHA =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", JLCC_PERU);

    public static final String JLCC_PREVENTIVO =
            "Hola {nombre}, le recordamos cordialmente que el pago de {concepto} del inmueble"
                + " {inmueble}, por {monto}, vence el {vencimiento}. Si ya realizó el pago, por"
                + " favor ignore este mensaje. Atentamente, {arrendador}.";

    public static final String JLCC_VENCIDO =
            "Hola {nombre}, le informamos que el pago de {concepto} del inmueble {inmueble} venció"
                + " el {vencimiento} y registra {dias} días de atraso. Monto pendiente: {monto}"
                + " (incluye mora estimada de {mora}). Agradecemos regularizarlo a la brevedad."
                + " Atentamente, {arrendador}.";

    public static final String JLCC_FIN_CONTRATO =
            "Hola {nombre}, su contrato {contrato} para el inmueble {inmueble} vence el"
                + " {fecha_fin}. Faltan {dias} días. Por favor, coordinemos oportunamente la"
                + " renovación o la entrega del inmueble. Atentamente, {arrendador}.";

    public static String jlccAplicar(String jlccPlantilla, Map<String, String> jlccDatos) {
        if (jlccPlantilla == null) return "";
        String jlccSalida = jlccPlantilla;
        if (jlccDatos != null) {
            for (Map.Entry<String, String> jlccDato : jlccDatos.entrySet()) {
                jlccSalida =
                        jlccSalida.replace(
                                "{" + jlccDato.getKey() + "}",
                                jlccDato.getValue() == null ? "" : jlccDato.getValue());
            }
        }
        return jlccSalida.replaceAll("\\{[^}]+}", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("[ \\t]+\\n", "\n")
                .trim();
    }

    public static String jlccSoles(BigDecimal jlccImporte) {
        BigDecimal jlccValor = jlccImporte == null ? BigDecimal.ZERO : jlccImporte;
        return "S/ " + String.format(Locale.US, "%,.2f", jlccValor);
    }

    public static String jlccFecha(LocalDate jlccFecha) {
        return jlccFecha == null ? "" : jlccFecha.format(JLCC_FECHA);
    }

    public static String jlccResolver(TipoRecordatorio jlccTipo, String jlccPersonalizada) {
        if (jlccPersonalizada != null && !jlccPersonalizada.isBlank()) return jlccPersonalizada;
        return switch (jlccTipo) {
            case PREVENTIVO -> JLCC_PREVENTIVO;
            case VENCIDO -> JLCC_VENCIDO;
            case FIN_CONTRATO -> JLCC_FIN_CONTRATO;
        };
    }
}
