package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Unidad que se dibuja como bloque en la maqueta 3D.
 * El semáforo de morosidad solo tiene sentido cuando el inmueble está alquilado.
 * El área, la renta, el fin de contrato y los días para vencer son datos del DTO:
 * nunca se exponen entidades JPA fuera de la transacción.
 */
public record BloqueMaqueta(Long id, String codigo, String nombre, String tipo,
                            int nivel, int orden, EstadoInmueble estado,
                            Long contratoId, String arrendatario,
                            SemaforoMorosidad semaforo, boolean inconsistencia,
                            BigDecimal areaM2,
                            BigDecimal rentaReferencial, LocalDate fechaFinContrato,
                            Long diasParaVencer,
                            Integer dormitorios, Integer banos, Boolean amoblado) {

    public BloqueMaqueta {
        if (estado == null) throw new IllegalArgumentException("El estado del bloque es obligatorio");
        if (estado != EstadoInmueble.ALQUILADO) semaforo = null;
        if (fechaFinContrato == null) diasParaVencer = null;
    }

    /** Constructor compatible con los datos de ejemplo del paso 1. */
    public BloqueMaqueta(Long id, String etiqueta, int nivel, int orden,
                         EstadoInmueble estado, SemaforoMorosidad semaforo) {
        this(id, null, etiqueta, null, nivel, orden, estado, null, null, semaforo, false,
                null, null, null, null, null, null, null);
    }

    /** Constructor compatible con la firma anterior a los datos de renta y vencimiento. */
    public BloqueMaqueta(Long id, String codigo, String nombre, String tipo,
                         int nivel, int orden, EstadoInmueble estado,
                         Long contratoId, String arrendatario,
                         SemaforoMorosidad semaforo, boolean inconsistencia) {
        this(id, codigo, nombre, tipo, nivel, orden, estado, contratoId, arrendatario,
                semaforo, inconsistencia, null, null, null, null, null, null, null);
    }

    /** Constructor compatible con la firma anterior al área del inmueble. */
    public BloqueMaqueta(Long id, String codigo, String nombre, String tipo,
                         int nivel, int orden, EstadoInmueble estado,
                         Long contratoId, String arrendatario,
                         SemaforoMorosidad semaforo, boolean inconsistencia,
                         BigDecimal rentaReferencial, LocalDate fechaFinContrato,
                         Long diasParaVencer) {
        this(id, codigo, nombre, tipo, nivel, orden, estado, contratoId, arrendatario,
                semaforo, inconsistencia, null, rentaReferencial, fechaFinContrato,
                diasParaVencer, null, null, null);
    }

    /** Constructor compatible con la firma anterior a dormitorios, baños y amoblado. */
    public BloqueMaqueta(Long id, String codigo, String nombre, String tipo,
                         int nivel, int orden, EstadoInmueble estado,
                         Long contratoId, String arrendatario,
                         SemaforoMorosidad semaforo, boolean inconsistencia,
                         BigDecimal areaM2, BigDecimal rentaReferencial,
                         LocalDate fechaFinContrato, Long diasParaVencer) {
        this(id, codigo, nombre, tipo, nivel, orden, estado, contratoId, arrendatario,
                semaforo, inconsistencia, areaM2, rentaReferencial, fechaFinContrato,
                diasParaVencer, null, null, null);
    }

    public String etiqueta() {
        return nombre;
    }

    /** Categoría del modo Mora. Los inmuebles no alquilados o sin semáforo quedan en gris. */
    public CategoriaMora categoriaMora() {
        if (estado != EstadoInmueble.ALQUILADO || semaforo == null) return CategoriaMora.SIN_MORA;
        return switch (semaforo) {
            case AL_DIA -> CategoriaMora.AL_DIA;
            case ATRASO_LEVE -> CategoriaMora.ATRASO_LEVE;
            case MOROSO -> CategoriaMora.MOROSO;
        };
    }

    /** Categoría del modo Vencimiento. Los límites son 0, 30, 31, 90 y 91 días. */
    public CategoriaVencimiento categoriaVencimiento() {
        if (fechaFinContrato == null || diasParaVencer == null) {
            return CategoriaVencimiento.SIN_CONTRATO;
        }
        if (diasParaVencer < 0) return CategoriaVencimiento.VENCIDO;
        if (diasParaVencer <= 30) return CategoriaVencimiento.HASTA_30_DIAS;
        if (diasParaVencer <= 90) return CategoriaVencimiento.DE_31_A_90_DIAS;
        return CategoriaVencimiento.MAS_DE_90_DIAS;
    }

    public enum CategoriaMora {
        AL_DIA("Al día"),
        ATRASO_LEVE("Atraso leve"),
        MOROSO("Moroso"),
        SIN_MORA("Sin mora");

        private final String etiqueta;

        CategoriaMora(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
    }

    public enum CategoriaVencimiento {
        VENCIDO("Vencido"),
        HASTA_30_DIAS("30 días o menos"),
        DE_31_A_90_DIAS("31 a 90 días"),
        MAS_DE_90_DIAS("Más de 90 días"),
        SIN_CONTRATO("Sin contrato");

        private final String etiqueta;

        CategoriaVencimiento(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
    }
}
