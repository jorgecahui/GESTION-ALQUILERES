package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.CanalRecordatorio;
import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.Recordatorio;
import com.ejemplo.alquileres.model.TipoRecordatorio;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.CuotaRepository;
import com.ejemplo.alquileres.repository.RecordatorioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JlccRecordatorioService {
    private final RecordatorioRepository jlccRecordatorios;
    private final CuotaRepository jlccCuotas;
    private final ContratoRepository jlccContratos;
    private final DatosArrendador jlccArrendador;
    private final JlccConfiguracionService jlccConfiguracion;
    private final JlccCalculadoraMora jlccMora;
    private final JlccPermisos jlccPermisos;
    private final Clock jlccClock;

    public record Propuesta(
            Long cuotaId, Long contratoId, String telefono, TipoRecordatorio tipo, String mensaje) {
        public boolean telefonoValido() {
            return telefono != null && !telefono.isBlank();
        }
    }

    @Transactional(readOnly = true)
    public List<Propuesta> jlccSugerencias() {
        return jlccSugerencias(LocalDate.now(jlccClock));
    }

    @Transactional(readOnly = true)
    public List<Propuesta> jlccSugerencias(LocalDate jlccHoy) {
        LocalDateTime jlccDesde = LocalDateTime.now(jlccClock).minusDays(3);
        return jlccCuotas.pendientesConContrato().stream()
                .filter(
                        cuota ->
                                !cuota.getFechaVencimiento()
                                        .isAfter(jlccHoy.plusDays(jlccConfiguracion.jlccDiasRecordatorio())))
                .filter(cuota -> jlccRecordatorios.recientes(cuota.getId(), jlccDesde).isEmpty())
                .map(cuota -> jlccPropuesta(cuota, jlccHoy))
                .toList();
    }

    @Transactional(readOnly = true)
    public Propuesta jlccPropuestaPorCuota(Long jlccCuotaId) {
        Cuota jlccCuota =
                jlccCuotas.findById(jlccCuotaId)
                        .orElseThrow(() -> new JlccNegocioException("La cuota no existe."));
        return jlccPropuesta(jlccCuota, LocalDate.now(jlccClock));
    }

    public Propuesta jlccPropuesta(Cuota jlccCuota, LocalDate jlccHoy) {
        Contrato jlccContrato = jlccCuota.getContrato();
        String jlccTelefono = JlccTelefonoUtil.jlccNormalizarPeru(jlccContrato.getArrendatario().getTelefono());
        long jlccDiasAtraso = Math.max(0, ChronoUnit.DAYS.between(jlccCuota.getFechaVencimiento(), jlccHoy));
        TipoRecordatorio jlccTipo =
                jlccDiasAtraso > 0 ? TipoRecordatorio.VENCIDO : TipoRecordatorio.PREVENTIVO;
        BigDecimal jlccSaldo = jlccMora.jlccSaldoCapital(jlccCuota);
        BigDecimal jlccMoraPendiente = jlccMora.jlccPendiente(jlccCuota, jlccContrato, jlccHoy);
        Map<String, String> jlccDatos = jlccDatosComunes(jlccContrato);
        jlccDatos.put("concepto", jlccCuota.getConcepto());
        jlccDatos.put("monto", JlccPlantillasMensaje.jlccSoles(jlccSaldo.add(jlccMoraPendiente)));
        jlccDatos.put("mora", JlccPlantillasMensaje.jlccSoles(jlccMoraPendiente));
        jlccDatos.put("vencimiento", JlccPlantillasMensaje.jlccFecha(jlccCuota.getFechaVencimiento()));
        jlccDatos.put("dias", String.valueOf(jlccDiasAtraso));
        String jlccMensaje = JlccPlantillasMensaje.jlccAplicar(jlccConfiguracion.jlccPlantilla(jlccTipo), jlccDatos);
        return new Propuesta(jlccCuota.getId(), jlccContrato.getId(), jlccTelefono, jlccTipo, jlccMensaje);
    }

    @Transactional(readOnly = true)
    public Propuesta jlccPropuestaFinContrato(Long jlccContratoId) {
        Contrato jlccContrato =
                jlccContratos
                        .detalle(jlccContratoId)
                        .orElseThrow(() -> new JlccNegocioException("El contrato no existe."));
        return jlccPropuestaFinContrato(jlccContrato, LocalDate.now(jlccClock));
    }

    public Propuesta jlccPropuestaFinContrato(Contrato jlccContrato, LocalDate jlccHoy) {
        String jlccTelefono = JlccTelefonoUtil.jlccNormalizarPeru(jlccContrato.getArrendatario().getTelefono());
        Map<String, String> jlccDatos = jlccDatosComunes(jlccContrato);
        jlccDatos.put("contrato", jlccContrato.getCodigo());
        jlccDatos.put("fecha_fin", JlccPlantillasMensaje.jlccFecha(jlccContrato.getFechaFin()));
        jlccDatos.put(
                "dias",
                String.valueOf(Math.max(0, ChronoUnit.DAYS.between(jlccHoy, jlccContrato.getFechaFin()))));
        String jlccMensaje =
                JlccPlantillasMensaje.jlccAplicar(
                        jlccConfiguracion.jlccPlantilla(TipoRecordatorio.FIN_CONTRATO), jlccDatos);
        return new Propuesta(
                null, jlccContrato.getId(), jlccTelefono, TipoRecordatorio.FIN_CONTRATO, jlccMensaje);
    }

    @Transactional(readOnly = true)
    public List<Recordatorio> jlccListar(Long jlccContratoId) {
        return jlccRecordatorios.findByContratoIdOrderByFechaEnvioDesc(jlccContratoId);
    }

    @Transactional(readOnly = true)
    public Map<Long, LocalDateTime> jlccUltimosPorCuota() {
        Map<Long, LocalDateTime> jlccResultado = new HashMap<>();
        for (Object[] jlccFila : jlccRecordatorios.ultimosPorCuota()) {
            if (jlccFila != null && jlccFila[0] != null && jlccFila[1] != null) {
                jlccResultado.put(((Number) jlccFila[0]).longValue(), (LocalDateTime) jlccFila[1]);
            }
        }
        return jlccResultado;
    }

    @Transactional
    public Recordatorio jlccRegistrar(Propuesta jlccPropuesta, String jlccMensaje, CanalRecordatorio jlccCanal) {
        jlccPermisos.jlccExigirGestion();
        if (jlccPropuesta == null) throw new JlccNegocioException("No hay un recordatorio para registrar.");
        if (jlccMensaje == null || jlccMensaje.isBlank())
            throw new JlccNegocioException("El mensaje no puede estar vacío.");
        if (jlccCanal == null) throw new JlccNegocioException("Selecciona un canal de envío.");
        String jlccTelefono = JlccTelefonoUtil.jlccNormalizarPeru(jlccPropuesta.telefono());
        if (jlccTelefono == null) throw new JlccNegocioException("Teléfono no válido o ausente.");
        Cuota jlccCuota =
                jlccPropuesta.cuotaId() == null
                        ? null
                        : jlccCuotas.findById(jlccPropuesta.cuotaId())
                                .orElseThrow(() -> new JlccNegocioException("La cuota no existe."));
        Contrato jlccContrato =
                jlccCuota != null
                        ? jlccCuota.getContrato()
                        : jlccContratos
                                .findById(jlccPropuesta.contratoId())
                                .orElseThrow(() -> new JlccNegocioException("El contrato no existe."));
        return jlccRecordatorios.save(
                Recordatorio.builder()
                        .contrato(jlccContrato)
                        .cuota(jlccCuota)
                        .tipo(jlccPropuesta.tipo())
                        .canal(jlccCanal)
                        .telefono(jlccTelefono)
                        .mensaje(jlccMensaje.trim())
                        .fechaEnvio(LocalDateTime.now(jlccClock))
                        .build());
    }

    public String jlccUrlWhatsApp(String jlccTelefono, String jlccMensaje) {
        String jlccNormalizado = JlccTelefonoUtil.jlccNormalizarPeru(jlccTelefono);
        if (jlccNormalizado == null) throw new JlccNegocioException("Teléfono no válido o ausente.");
        String jlccTexto =
                URLEncoder.encode(jlccMensaje == null ? "" : jlccMensaje, StandardCharsets.UTF_8)
                        .replace("+", "%20");
        return "https://wa.me/" + jlccNormalizado + "?text=" + jlccTexto;
    }

    public String jlccUltimoTexto(LocalDateTime jlccFecha) {
        if (jlccFecha == null) return "—";
        long jlccDias = ChronoUnit.DAYS.between(jlccFecha.toLocalDate(), LocalDate.now(jlccClock));
        if (jlccDias <= 0) return "hoy";
        if (jlccDias == 1) return "ayer";
        return "hace " + jlccDias + " días";
    }

    private Map<String, String> jlccDatosComunes(Contrato jlccContrato) {
        Map<String, String> jlccDatos = new LinkedHashMap<>();
        jlccDatos.put("nombre", jlccContrato.getArrendatario().getNombreCompleto());
        jlccDatos.put("inmueble", jlccContrato.getInmueble().getNombre());
        jlccDatos.put("arrendador", jlccArrendador.razonSocial());
        jlccDatos.put("contrato", jlccContrato.getCodigo());
        jlccDatos.put("fecha_fin", JlccPlantillasMensaje.jlccFecha(jlccContrato.getFechaFin()));
        return jlccDatos;
    }
}
