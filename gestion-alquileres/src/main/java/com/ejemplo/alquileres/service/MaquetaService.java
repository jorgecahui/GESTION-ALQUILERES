package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoCuota;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.ui.BloqueMaqueta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma los DTO de la maqueta sin exponer entidades JPA fuera de la transacción. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaquetaService {
    public static final int TONOS_RENTA = 8;

    private final InmuebleRepository inmuebles;
    private final ContratoRepository contratos;
    private final MorosidadService morosidad;
    private final CalculadoraMora calculadora;
    private final Clock clock;

    public record ResultadoMaqueta(List<BloqueMaqueta> bloques, int sinUbicacion) {
        public ResultadoMaqueta {
            bloques = List.copyOf(bloques);
        }
    }

    public record ContratoDetalle(Long id, String codigo, String arrendatario, String documento,
                                  LocalDate fechaInicio, LocalDate fechaFin, BigDecimal rentaMensual,
                                  SemaforoMorosidad semaforo, long cuotasVencidas,
                                  BigDecimal montoPendiente, Long diasParaVencer) {
        /** Constructor compatible con la firma anterior a los días para vencer. */
        public ContratoDetalle(Long id, String codigo, String arrendatario, String documento,
                               LocalDate fechaInicio, LocalDate fechaFin, BigDecimal rentaMensual,
                               SemaforoMorosidad semaforo, long cuotasVencidas,
                               BigDecimal montoPendiente) {
            this(id, codigo, arrendatario, documento, fechaInicio, fechaFin, rentaMensual,
                    semaforo, cuotasVencidas, montoPendiente, null);
        }
    }

    public record DetalleMaqueta(Long id, String codigo, String nombre, String tipo,
                                 Integer nivel, Integer orden, String direccion, String distrito,
                                 BigDecimal areaM2, Integer dormitorios, Integer banos,
                                 BigDecimal rentaReferencial, EstadoInmueble estado,
                                 ContratoDetalle contrato, boolean inconsistencia) {
    }

    public List<BloqueMaqueta> listarBloques() {
        return cargar().bloques();
    }

    public ResultadoMaqueta cargar() {
        LocalDate hoy = LocalDate.now(clock);
        List<Inmueble> todos = inmuebles.listarParaMaqueta();
        Map<Long, Contrato> vigentes = new HashMap<>();
        for (Contrato contrato : contratos.vigentesConDetalle()) {
            if (contrato.getInmueble() != null) {
                vigentes.putIfAbsent(contrato.getInmueble().getId(), contrato);
            }
        }

        List<BloqueMaqueta> bloques = new ArrayList<>();
        int sinUbicacion = 0;
        for (Inmueble inmueble : todos) {
            if (inmueble.getNivel() == null || inmueble.getOrden() == null) {
                sinUbicacion++;
                continue;
            }
            Contrato contrato = vigentes.get(inmueble.getId());
            boolean alquilado = inmueble.getEstado() == EstadoInmueble.ALQUILADO;
            boolean inconsistencia = alquilado && contrato == null;
            SemaforoMorosidad semaforo = alquilado && contrato != null
                    ? morosidad.semaforo(contrato)
                    : null;
            String tipo = inmueble.getTipo() == null ? "Sin tipo" : inmueble.getTipo().getNombre();
            String arrendatario = contrato == null || contrato.getArrendatario() == null
                    ? null
                    : contrato.getArrendatario().getNombreCompleto();
            LocalDate fechaFin = contrato == null ? null : contrato.getFechaFin();
            Long diasParaVencer = fechaFin == null ? null : ChronoUnit.DAYS.between(hoy, fechaFin);
            bloques.add(new BloqueMaqueta(
                    inmueble.getId(),
                    inmueble.getCodigo(),
                    inmueble.getNombre(),
                    tipo,
                    inmueble.getNivel(),
                    inmueble.getOrden(),
                    inmueble.getEstado(),
                    contrato == null ? null : contrato.getId(),
                    arrendatario,
                    semaforo,
                    inconsistencia,
                    inmueble.getAreaM2(),
                    inmueble.getRentaReferencial(),
                    fechaFin,
                    diasParaVencer,
                    inmueble.getDormitorios(),
                    inmueble.getBanos(),
                    inmueble.isAmoblado()));
        }
        return new ResultadoMaqueta(bloques, sinUbicacion);
    }

    public DetalleMaqueta detalle(Long inmuebleId) {
        if (inmuebleId == null) throw new NegocioException("El inmueble no existe.");
        Inmueble inmueble = inmuebles.detallePorId(inmuebleId)
                .orElseThrow(() -> new NegocioException("El inmueble no existe."));
        Contrato contrato = contratos.vigentesPorInmuebleConDetalle(inmuebleId).stream()
                .findFirst().orElse(null);
        ContratoDetalle contratoDetalle = contrato == null ? null : contratoDetalle(contrato);
        boolean inconsistencia = inmueble.getEstado() == EstadoInmueble.ALQUILADO && contrato == null;
        String tipo = inmueble.getTipo() == null ? "Sin tipo" : inmueble.getTipo().getNombre();
        return new DetalleMaqueta(
                inmueble.getId(),
                inmueble.getCodigo(),
                inmueble.getNombre(),
                tipo,
                inmueble.getNivel(),
                inmueble.getOrden(),
                inmueble.getDireccion(),
                inmueble.getDistrito(),
                inmueble.getAreaM2(),
                inmueble.getDormitorios(),
                inmueble.getBanos(),
                inmueble.getRentaReferencial(),
                inmueble.getEstado(),
                contratoDetalle,
                inconsistencia);
    }

    private ContratoDetalle contratoDetalle(Contrato contrato) {
        LocalDate hoy = LocalDate.now(clock);
        List<Cuota> vencidas = morosidad.vencidas(contrato);
        BigDecimal pendiente = BigDecimal.ZERO;
        if (contrato.getCuotas() != null) {
            for (Cuota cuota : contrato.getCuotas()) {
                if (cuota.getEstado() != EstadoCuota.PENDIENTE && cuota.getEstado() != EstadoCuota.PARCIAL) {
                    continue;
                }
                pendiente = pendiente
                        .add(calculadora.saldoCapital(cuota))
                        .add(calculadora.pendiente(cuota, contrato, hoy));
            }
        }
        pendiente = pendiente.setScale(2, RoundingMode.HALF_UP);
        String arrendatario = contrato.getArrendatario() == null
                ? null
                : contrato.getArrendatario().getNombreCompleto();
        String documento = contrato.getArrendatario() == null
                ? null
                : contrato.getArrendatario().getNumeroDocumento();
        return new ContratoDetalle(
                contrato.getId(),
                contrato.getCodigo(),
                arrendatario,
                documento,
                contrato.getFechaInicio(),
                contrato.getFechaFin(),
                contrato.getRentaMensual(),
                morosidad.semaforo(contrato),
                vencidas.size(),
                pendiente,
                contrato.getFechaFin() == null
                        ? null : ChronoUnit.DAYS.between(hoy, contrato.getFechaFin()));
    }

    /**
     * Ubica una renta dentro de una escala fija de tonos. Devuelve -1 cuando no
     * hay renta. Si el rango es cero (todas las rentas iguales) usa el tono medio
     * para no dividir por cero.
     */
    public static int tonoRenta(BigDecimal renta, BigDecimal minima, BigDecimal maxima, int tonos) {
        if (tonos < 1) throw new IllegalArgumentException("La escala debe tener al menos un tono");
        if (tonos == 1) return 0;
        if (renta == null || minima == null || maxima == null) return -1;
        if (maxima.compareTo(minima) <= 0) return (tonos - 1) / 2;

        BigDecimal proporcion = renta.subtract(minima)
                .divide(maxima.subtract(minima), 6, RoundingMode.HALF_UP);
        if (proporcion.signum() < 0) proporcion = BigDecimal.ZERO;
        if (proporcion.compareTo(BigDecimal.ONE) > 0) proporcion = BigDecimal.ONE;
        int indice = proporcion.multiply(BigDecimal.valueOf(tonos - 1))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.max(0, Math.min(tonos - 1, indice));
    }
}
