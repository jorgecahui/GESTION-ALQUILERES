package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoCuota;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.ui.BloqueMaqueta;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JlccMaquetaService {
    public static final int JLCC_TONOS_RENTA = 8;

    private final InmuebleRepository jlccInmuebles;
    private final ContratoRepository jlccContratos;
    private final JlccMorosidadService jlccMorosidad;
    private final JlccCalculadoraMora jlccCalculadora;
    private final Clock jlccClock;

    public record ResultadoMaqueta(List<BloqueMaqueta> bloques, int sinUbicacion) {
        public ResultadoMaqueta {
            bloques = List.copyOf(bloques);
        }
    }

    public record ContratoDetalle(
            Long id,
            String codigo,
            String arrendatario,
            String documento,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal rentaMensual,
            SemaforoMorosidad semaforo,
            long cuotasVencidas,
            BigDecimal montoPendiente,
            Long diasParaVencer) {
        public ContratoDetalle(
                Long id,
                String codigo,
                String arrendatario,
                String documento,
                LocalDate fechaInicio,
                LocalDate fechaFin,
                BigDecimal rentaMensual,
                SemaforoMorosidad semaforo,
                long cuotasVencidas,
                BigDecimal montoPendiente) {
            this(
                    id,
                    codigo,
                    arrendatario,
                    documento,
                    fechaInicio,
                    fechaFin,
                    rentaMensual,
                    semaforo,
                    cuotasVencidas,
                    montoPendiente,
                    null);
        }
    }

    public record DetalleMaqueta(
            Long id,
            String codigo,
            String nombre,
            String tipo,
            Integer nivel,
            Integer orden,
            String direccion,
            String distrito,
            BigDecimal areaM2,
            Integer dormitorios,
            Integer banos,
            BigDecimal rentaReferencial,
            EstadoInmueble estado,
            ContratoDetalle contrato,
            boolean inconsistencia) {}

    public List<BloqueMaqueta> jlccListarBloques() {
        return jlccCargar().bloques();
    }

    public ResultadoMaqueta jlccCargar() {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        List<Inmueble> jlccTodos = jlccInmuebles.listarParaMaqueta();
        Map<Long, Contrato> jlccVigentes = new HashMap<>();
        for (Contrato jlccContrato : jlccContratos.vigentesConDetalle()) {
            if (jlccContrato.getInmueble() != null) {
                jlccVigentes.putIfAbsent(jlccContrato.getInmueble().getId(), jlccContrato);
            }
        }

        List<BloqueMaqueta> jlccBloques = new ArrayList<>();
        int jlccSinUbicacion = 0;
        for (Inmueble jlccInmueble : jlccTodos) {
            if (jlccInmueble.getNivel() == null || jlccInmueble.getOrden() == null) {
                jlccSinUbicacion++;
                continue;
            }
            Contrato jlccContrato = jlccVigentes.get(jlccInmueble.getId());
            boolean jlccAlquilado = jlccInmueble.getEstado() == EstadoInmueble.ALQUILADO;
            boolean jlccInconsistencia = jlccAlquilado && jlccContrato == null;
            SemaforoMorosidad jlccSemaforo =
                    jlccAlquilado && jlccContrato != null ? jlccMorosidad.jlccSemaforo(jlccContrato) : null;
            String jlccTipo = jlccInmueble.getTipo() == null ? "Sin tipo" : jlccInmueble.getTipo().getNombre();
            String jlccArrendatario =
                    jlccContrato == null || jlccContrato.getArrendatario() == null
                            ? null
                            : jlccContrato.getArrendatario().getNombreCompleto();
            LocalDate jlccFechaFin = jlccContrato == null ? null : jlccContrato.getFechaFin();
            Long jlccDiasParaVencer = jlccFechaFin == null ? null : ChronoUnit.DAYS.between(jlccHoy, jlccFechaFin);
            jlccBloques.add(
                    new BloqueMaqueta(
                            jlccInmueble.getId(),
                            jlccInmueble.getCodigo(),
                            jlccInmueble.getNombre(),
                            jlccTipo,
                            jlccInmueble.getNivel(),
                            jlccInmueble.getOrden(),
                            jlccInmueble.getEstado(),
                            jlccContrato == null ? null : jlccContrato.getId(),
                            jlccArrendatario,
                            jlccSemaforo,
                            jlccInconsistencia,
                            jlccInmueble.getAreaM2(),
                            jlccInmueble.getRentaReferencial(),
                            jlccFechaFin,
                            jlccDiasParaVencer,
                            jlccInmueble.getDormitorios(),
                            jlccInmueble.getBanos(),
                            jlccInmueble.isAmoblado()));
        }
        return new ResultadoMaqueta(jlccBloques, jlccSinUbicacion);
    }

    public DetalleMaqueta jlccDetalle(Long jlccInmuebleId) {
        if (jlccInmuebleId == null) throw new JlccNegocioException("El inmueble no existe.");
        Inmueble jlccInmueble =
                jlccInmuebles
                        .detallePorId(jlccInmuebleId)
                        .orElseThrow(() -> new JlccNegocioException("El inmueble no existe."));
        Contrato jlccContrato =
                jlccContratos.vigentesPorInmuebleConDetalle(jlccInmuebleId).stream()
                        .findFirst()
                        .orElse(null);
        ContratoDetalle jlccContratoDetalle = jlccContrato == null ? null : jlccContratoDetalle(jlccContrato);
        boolean jlccInconsistencia =
                jlccInmueble.getEstado() == EstadoInmueble.ALQUILADO && jlccContrato == null;
        String jlccTipo = jlccInmueble.getTipo() == null ? "Sin tipo" : jlccInmueble.getTipo().getNombre();
        return new DetalleMaqueta(
                jlccInmueble.getId(),
                jlccInmueble.getCodigo(),
                jlccInmueble.getNombre(),
                jlccTipo,
                jlccInmueble.getNivel(),
                jlccInmueble.getOrden(),
                jlccInmueble.getDireccion(),
                jlccInmueble.getDistrito(),
                jlccInmueble.getAreaM2(),
                jlccInmueble.getDormitorios(),
                jlccInmueble.getBanos(),
                jlccInmueble.getRentaReferencial(),
                jlccInmueble.getEstado(),
                jlccContratoDetalle,
                jlccInconsistencia);
    }

    private ContratoDetalle jlccContratoDetalle(Contrato jlccContrato) {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        List<Cuota> jlccVencidas = jlccMorosidad.jlccVencidas(jlccContrato);
        BigDecimal jlccPendiente = BigDecimal.ZERO;
        if (jlccContrato.getCuotas() != null) {
            for (Cuota jlccCuota : jlccContrato.getCuotas()) {
                if (jlccCuota.getEstado() != EstadoCuota.PENDIENTE
                        && jlccCuota.getEstado() != EstadoCuota.PARCIAL) {
                    continue;
                }
                jlccPendiente =
                        jlccPendiente
                                .add(jlccCalculadora.jlccSaldoCapital(jlccCuota))
                                .add(jlccCalculadora.jlccPendiente(jlccCuota, jlccContrato, jlccHoy));
            }
        }
        jlccPendiente = jlccPendiente.setScale(2, RoundingMode.HALF_UP);
        String jlccArrendatario =
                jlccContrato.getArrendatario() == null
                        ? null
                        : jlccContrato.getArrendatario().getNombreCompleto();
        String jlccDocumento =
                jlccContrato.getArrendatario() == null
                        ? null
                        : jlccContrato.getArrendatario().getNumeroDocumento();
        return new ContratoDetalle(
                jlccContrato.getId(),
                jlccContrato.getCodigo(),
                jlccArrendatario,
                jlccDocumento,
                jlccContrato.getFechaInicio(),
                jlccContrato.getFechaFin(),
                jlccContrato.getRentaMensual(),
                jlccMorosidad.jlccSemaforo(jlccContrato),
                jlccVencidas.size(),
                jlccPendiente,
                jlccContrato.getFechaFin() == null
                        ? null
                        : ChronoUnit.DAYS.between(jlccHoy, jlccContrato.getFechaFin()));
    }

    public static int jlccTonoRenta(BigDecimal jlccRenta, BigDecimal jlccMinima, BigDecimal jlccMaxima, int jlccTonos) {
        if (jlccTonos < 1) throw new IllegalArgumentException("La escala debe tener al menos un tono");
        if (jlccTonos == 1) return 0;
        if (jlccRenta == null || jlccMinima == null || jlccMaxima == null) return -1;
        if (jlccMaxima.compareTo(jlccMinima) <= 0) return (jlccTonos - 1) / 2;

        BigDecimal jlccProporcion =
                jlccRenta.subtract(jlccMinima).divide(jlccMaxima.subtract(jlccMinima), 6, RoundingMode.HALF_UP);
        if (jlccProporcion.signum() < 0) jlccProporcion = BigDecimal.ZERO;
        if (jlccProporcion.compareTo(BigDecimal.ONE) > 0) jlccProporcion = BigDecimal.ONE;
        int jlccIndice =
                jlccProporcion
                        .multiply(BigDecimal.valueOf(jlccTonos - 1))
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
        return Math.max(0, Math.min(jlccTonos - 1, jlccIndice));
    }
}
