package com.ejemplo.alquileres.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * Parámetros editables de la aplicación. Se guarda una única fila con id 1;
 * el servicio la crea a partir de las propiedades al primer arranque.
 */
@Entity
@Table(name = "configuracion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Configuracion {
    public static final long FILA_UNICA = 1L;

    @Id private Long id;

    @Column(name = "arrendador_razon_social", length = 180) private String arrendadorRazonSocial;
    @Column(name = "arrendador_tipo_documento", length = 20) private String arrendadorTipoDocumento;
    @Column(name = "arrendador_documento", length = 20) private String arrendadorDocumento;
    @Column(name = "arrendador_direccion", length = 250) private String arrendadorDireccion;
    @Column(name = "arrendador_telefono", length = 40) private String arrendadorTelefono;
    @Column(name = "arrendador_email", length = 120) private String arrendadorEmail;
    @Column(name = "logo_archivo", length = 180) private String logoArchivo;

    @Column(name = "recibo_serie", length = 10) private String reciboSerie;
    @Column(name = "recibo_leyenda", length = 250) private String reciboLeyenda;

    @Enumerated(EnumType.STRING) @Column(name = "mora_tipo", length = 30) private TipoMora moraTipo;
    @Column(name = "mora_valor", precision = 10, scale = 4) private BigDecimal moraValor;
    @Column(name = "mora_dias_gracia") private int moraDiasGracia;

    @Column(name = "alertas_dias_vencimiento") private int alertasDiasVencimiento;
    @Column(name = "alerta_recordatorio_dias") private int alertaRecordatorioDias;

    @Column(name = "semaforo_umbral_leve_dias") private int semaforoUmbralLeveDias;
    @Column(name = "semaforo_cuotas_rojo") private int semaforoCuotasRojo;

    @Column(name = "plantilla_preventivo", length = 2000) private String plantillaPreventivo;
    @Column(name = "plantilla_vencido", length = 2000) private String plantillaVencido;
    @Column(name = "plantilla_fin_contrato", length = 2000) private String plantillaFinContrato;

    @Column(name = "inactividad_minutos") private int inactividadMinutos;

    @Version private Long version;
}
