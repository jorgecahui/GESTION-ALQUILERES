package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.exception.NegocioException;
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
public class RecordatorioService {
    private final RecordatorioRepository recordatorios;
    private final CuotaRepository cuotas;
    private final ContratoRepository contratos;
    private final DatosArrendador arrendador;
    private final ConfiguracionService configuracion;
    private final CalculadoraMora mora;
    private final Permisos permisos;
    private final Clock clock;

    public record Propuesta(
            Long cuotaId, Long contratoId, String telefono, TipoRecordatorio tipo, String mensaje) {
        public boolean telefonoValido() {
            return telefono != null && !telefono.isBlank();
        }
    }

    @Transactional(readOnly = true)
    public List<Propuesta> sugerencias() {
        return sugerencias(LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public List<Propuesta> sugerencias(LocalDate hoy) {
        LocalDateTime desde = LocalDateTime.now(clock).minusDays(3);
        return cuotas.pendientesConContrato().stream()
                .filter(
                        cuota ->
                                !cuota.getFechaVencimiento()
                                        .isAfter(hoy.plusDays(configuracion.diasRecordatorio())))
                .filter(cuota -> recordatorios.recientes(cuota.getId(), desde).isEmpty())
                .map(cuota -> propuesta(cuota, hoy))
                .toList();
    }

    @Transactional(readOnly = true)
    public Propuesta propuestaPorCuota(Long cuotaId) {
        Cuota cuota =
                cuotas.findById(cuotaId)
                        .orElseThrow(() -> new NegocioException("La cuota no existe."));
        return propuesta(cuota, LocalDate.now(clock));
    }

    public Propuesta propuesta(Cuota cuota, LocalDate hoy) {
        Contrato contrato = cuota.getContrato();
        String telefono = TelefonoUtil.normalizarPeru(contrato.getArrendatario().getTelefono());
        long diasAtraso = Math.max(0, ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy));
        TipoRecordatorio tipo =
                diasAtraso > 0 ? TipoRecordatorio.VENCIDO : TipoRecordatorio.PREVENTIVO;
        BigDecimal saldo = mora.saldoCapital(cuota);
        BigDecimal moraPendiente = mora.pendiente(cuota, contrato, hoy);
        Map<String, String> datos = datosComunes(contrato);
        datos.put("concepto", cuota.getConcepto());
        datos.put("monto", PlantillasMensaje.soles(saldo.add(moraPendiente)));
        datos.put("mora", PlantillasMensaje.soles(moraPendiente));
        datos.put("vencimiento", PlantillasMensaje.fecha(cuota.getFechaVencimiento()));
        datos.put("dias", String.valueOf(diasAtraso));
        String mensaje = PlantillasMensaje.aplicar(configuracion.plantilla(tipo), datos);
        return new Propuesta(cuota.getId(), contrato.getId(), telefono, tipo, mensaje);
    }

    @Transactional(readOnly = true)
    public Propuesta propuestaFinContrato(Long contratoId) {
        Contrato contrato =
                contratos
                        .detalle(contratoId)
                        .orElseThrow(() -> new NegocioException("El contrato no existe."));
        return propuestaFinContrato(contrato, LocalDate.now(clock));
    }

    public Propuesta propuestaFinContrato(Contrato contrato, LocalDate hoy) {
        String telefono = TelefonoUtil.normalizarPeru(contrato.getArrendatario().getTelefono());
        Map<String, String> datos = datosComunes(contrato);
        datos.put("contrato", contrato.getCodigo());
        datos.put("fecha_fin", PlantillasMensaje.fecha(contrato.getFechaFin()));
        datos.put(
                "dias",
                String.valueOf(Math.max(0, ChronoUnit.DAYS.between(hoy, contrato.getFechaFin()))));
        String mensaje =
                PlantillasMensaje.aplicar(
                        configuracion.plantilla(TipoRecordatorio.FIN_CONTRATO), datos);
        return new Propuesta(
                null, contrato.getId(), telefono, TipoRecordatorio.FIN_CONTRATO, mensaje);
    }

    @Transactional(readOnly = true)
    public List<Recordatorio> listar(Long contratoId) {
        return recordatorios.findByContratoIdOrderByFechaEnvioDesc(contratoId);
    }

    @Transactional(readOnly = true)
    public Map<Long, LocalDateTime> ultimosPorCuota() {
        Map<Long, LocalDateTime> resultado = new HashMap<>();
        for (Object[] fila : recordatorios.ultimosPorCuota()) {
            if (fila != null && fila[0] != null && fila[1] != null) {
                resultado.put(((Number) fila[0]).longValue(), (LocalDateTime) fila[1]);
            }
        }
        return resultado;
    }

    @Transactional
    public Recordatorio registrar(Propuesta propuesta, String mensaje, CanalRecordatorio canal) {
        permisos.exigirGestion();
        if (propuesta == null) throw new NegocioException("No hay un recordatorio para registrar.");
        if (mensaje == null || mensaje.isBlank())
            throw new NegocioException("El mensaje no puede estar vacío.");
        if (canal == null) throw new NegocioException("Selecciona un canal de envío.");
        String telefono = TelefonoUtil.normalizarPeru(propuesta.telefono());
        if (telefono == null) throw new NegocioException("Teléfono no válido o ausente.");
        Cuota cuota =
                propuesta.cuotaId() == null
                        ? null
                        : cuotas.findById(propuesta.cuotaId())
                                .orElseThrow(() -> new NegocioException("La cuota no existe."));
        Contrato contrato =
                cuota != null
                        ? cuota.getContrato()
                        : contratos
                                .findById(propuesta.contratoId())
                                .orElseThrow(() -> new NegocioException("El contrato no existe."));
        return recordatorios.save(
                Recordatorio.builder()
                        .contrato(contrato)
                        .cuota(cuota)
                        .tipo(propuesta.tipo())
                        .canal(canal)
                        .telefono(telefono)
                        .mensaje(mensaje.trim())
                        .fechaEnvio(LocalDateTime.now(clock))
                        .build());
    }

    public String urlWhatsApp(String telefono, String mensaje) {
        String normalizado = TelefonoUtil.normalizarPeru(telefono);
        if (normalizado == null) throw new NegocioException("Teléfono no válido o ausente.");
        String texto =
                URLEncoder.encode(mensaje == null ? "" : mensaje, StandardCharsets.UTF_8)
                        .replace("+", "%20");
        return "https://wa.me/" + normalizado + "?text=" + texto;
    }

    public String ultimoTexto(LocalDateTime fecha) {
        if (fecha == null) return "—";
        long dias = ChronoUnit.DAYS.between(fecha.toLocalDate(), LocalDate.now(clock));
        if (dias <= 0) return "hoy";
        if (dias == 1) return "ayer";
        return "hace " + dias + " días";
    }

    private Map<String, String> datosComunes(Contrato contrato) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("nombre", contrato.getArrendatario().getNombreCompleto());
        datos.put("inmueble", contrato.getInmueble().getNombre());
        datos.put("arrendador", arrendador.razonSocial());
        datos.put("contrato", contrato.getCodigo());
        datos.put("fecha_fin", PlantillasMensaje.fecha(contrato.getFechaFin()));
        return datos;
    }
}
