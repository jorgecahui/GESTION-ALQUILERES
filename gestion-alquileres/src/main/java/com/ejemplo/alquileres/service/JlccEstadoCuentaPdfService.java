package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoCuota;
import com.ejemplo.alquileres.model.EstadoPago;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.PagoRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccEstadoCuentaPdfService {
    private final ContratoRepository jlccContratos;
    private final PagoRepository jlccPagos;
    private final JlccCalculadoraMora jlccMora;
    private final DatosArrendador jlccArrendador;
    private final Clock jlccClock;

    @Transactional(readOnly = true)
    public byte[] jlccGenerar(Long jlccContratoId) {
        Contrato jlccContrato =
                jlccContratos
                        .detalle(jlccContratoId)
                        .orElseThrow(() -> new JlccNegocioException("El contrato no existe."));
        return jlccEscribir(jlccContrato, jlccPagos.porContrato(jlccContratoId));
    }

    public byte[] jlccEscribir(Contrato jlccContrato, List<Pago> jlccListaPagos) {
        try (ByteArrayOutputStream jlccSalida = new ByteArrayOutputStream()) {
            Document jlccDocumento = JlccDocumentoPdf.jlccDocumento();
            PdfWriter jlccWriter = PdfWriter.getInstance(jlccDocumento, jlccSalida);
            JlccDocumentoPdf.jlccAgregarPie(
                    jlccDocumento,
                    jlccWriter,
                    "Estado de cuenta interno · no es un comprobante electrónico SUNAT");
            jlccDocumento.open();
            jlccDocumento.add(jlccCabecera(jlccContrato));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(12));
            jlccDocumento.add(jlccDatos(jlccContrato));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(14));
            jlccDocumento.add(jlccTitulo("CUOTAS DEL CONTRATO"));
            jlccDocumento.add(jlccTablaCuotas(jlccContrato));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(14));
            jlccDocumento.add(jlccTitulo("PAGOS REGISTRADOS"));
            jlccDocumento.add(jlccTablaPagos(jlccListaPagos));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(14));
            jlccDocumento.add(jlccResumen(jlccContrato));
            jlccDocumento.close();
            return jlccSalida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el estado de cuenta PDF.", e);
        }
    }

    private PdfPTable jlccCabecera(Contrato jlccContrato) {
        PdfPTable jlccTabla = new PdfPTable(new float[] {62, 38});
        jlccTabla.setWidthPercentage(100);
        PdfPCell jlccEmisor = JlccDocumentoPdf.jlccCeldaSinBorde();
        JlccDocumentoPdf.jlccAgregarLogo(jlccEmisor, jlccArrendador.logo());
        jlccEmisor.addElement(
                new Paragraph(
                        jlccArrendador.razonSocial(),
                        JlccDocumentoPdf.jlccFuente(18, Font.BOLD, JlccDocumentoPdf.JLCC_AZUL)));
        jlccEmisor.addElement(
                new Paragraph(
                        jlccArrendador.documentoConTipo(),
                        JlccDocumentoPdf.jlccFuente(9, Font.BOLD, Color.DARK_GRAY)));
        jlccEmisor.addElement(
                new Paragraph(
                        jlccArrendador.direccion(),
                        JlccDocumentoPdf.jlccFuente(8.5f, Font.NORMAL, JlccDocumentoPdf.JLCC_GRIS)));
        jlccEmisor.addElement(
                new Paragraph(
                        jlccArrendador.telefono() + "  ·  " + jlccArrendador.email(),
                        JlccDocumentoPdf.jlccFuente(8.5f, Font.NORMAL, JlccDocumentoPdf.JLCC_GRIS)));
        jlccTabla.addCell(jlccEmisor);
        PdfPTable jlccCaja = new PdfPTable(1);
        jlccCaja.setWidthPercentage(100);
        jlccCaja.addCell(
                JlccDocumentoPdf.jlccCelda(
                        "ESTADO DE CUENTA",
                        JlccDocumentoPdf.jlccFuente(10, Font.BOLD, Color.WHITE),
                        JlccDocumentoPdf.JLCC_AZUL));
        jlccCaja.addCell(
                JlccDocumentoPdf.jlccCelda(
                        jlccContrato.getCodigo(),
                        JlccDocumentoPdf.jlccFuente(15, Font.BOLD, JlccDocumentoPdf.JLCC_AZUL),
                        Color.WHITE));
        PdfPCell jlccMarco = new PdfPCell(jlccCaja);
        jlccMarco.setPadding(0);
        jlccMarco.setBorderColor(JlccDocumentoPdf.JLCC_AZUL);
        jlccMarco.setBorderWidth(1.2f);
        jlccTabla.addCell(jlccMarco);
        return jlccTabla;
    }

    private PdfPTable jlccDatos(Contrato jlccContrato) {
        PdfPTable jlccTabla = new PdfPTable(new float[] {18, 32, 18, 32});
        jlccTabla.setWidthPercentage(100);
        jlccDato(jlccTabla, "ARRENDATARIO", jlccContrato.getArrendatario().getNombreCompleto());
        jlccDato(
                jlccTabla,
                "DOCUMENTO",
                jlccContrato.getArrendatario().getTipoDocumento()
                        + " "
                        + jlccContrato.getArrendatario().getNumeroDocumento());
        jlccDato(
                jlccTabla,
                "INMUEBLE",
                jlccContrato.getInmueble().getCodigo() + " · " + jlccContrato.getInmueble().getNombre());
        jlccDato(jlccTabla, "DIRECCIÓN", jlccContrato.getInmueble().getDireccion());
        jlccDato(
                jlccTabla,
                "VIGENCIA",
                JlccDocumentoPdf.jlccFecha(jlccContrato.getFechaInicio())
                        + " al "
                        + JlccDocumentoPdf.jlccFecha(jlccContrato.getFechaFin()));
        jlccDato(jlccTabla, "RENTA MENSUAL", JlccDocumentoPdf.jlccSoles(jlccContrato.getRentaMensual()));
        jlccDato(jlccTabla, "DÍA DE PAGO", String.valueOf(jlccContrato.getDiaPago()));
        jlccDato(jlccTabla, "EMITIDO", JlccDocumentoPdf.jlccFecha(LocalDate.now(jlccClock)));
        PdfPCell jlccCaja = new PdfPCell(jlccTabla);
        jlccCaja.setBorderColor(JlccDocumentoPdf.JLCC_BORDE);
        jlccCaja.setBackgroundColor(JlccDocumentoPdf.JLCC_SUAVE);
        jlccCaja.setPadding(8);
        PdfPTable jlccMarco = new PdfPTable(1);
        jlccMarco.setWidthPercentage(100);
        jlccMarco.addCell(jlccCaja);
        return jlccMarco;
    }

    private PdfPTable jlccTitulo(String jlccTexto) {
        PdfPTable jlccTabla = new PdfPTable(1);
        jlccTabla.setWidthPercentage(100);
        PdfPCell jlccCelda =
                JlccDocumentoPdf.jlccCelda(
                        jlccTexto, JlccDocumentoPdf.jlccFuente(9, Font.BOLD, Color.WHITE), JlccDocumentoPdf.JLCC_AZUL);
        jlccCelda.setPaddingTop(5);
        jlccCelda.setPaddingBottom(5);
        jlccTabla.addCell(jlccCelda);
        return jlccTabla;
    }

    private PdfPTable jlccTablaCuotas(Contrato jlccContrato) {
        PdfPTable jlccTabla = new PdfPTable(new float[] {6, 27, 13, 13, 12, 12, 12, 15});
        jlccTabla.setWidthPercentage(100);
        String[] jlccTitulos = {
            "#", "CONCEPTO", "VENCIMIENTO", "MONTO", "PAGADO", "MORA", "SALDO", "ESTADO"
        };
        for (String jlccTitulo : jlccTitulos) {
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            jlccTitulo,
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.BOLD, Color.WHITE),
                            JlccDocumentoPdf.JLCC_AZUL));
        }
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        List<Cuota> jlccCuotas =
                jlccContrato.getCuotas().stream()
                        .sorted(
                                Comparator.comparing(Cuota::getFechaVencimiento)
                                        .thenComparing(Cuota::getNumero))
                        .toList();
        int jlccIndice = 0;
        for (Cuota jlccCuota : jlccCuotas) {
            Color jlccFondo = jlccIndice++ % 2 == 0 ? Color.WHITE : JlccDocumentoPdf.JLCC_SUAVE;
            BigDecimal jlccSaldo = jlccMora.jlccSaldoCapital(jlccCuota);
            BigDecimal jlccMoraPendiente =
                    jlccCuota.getEstado() == EstadoCuota.ANULADA
                            ? BigDecimal.ZERO
                            : jlccMora.jlccPendiente(jlccCuota, jlccContrato, jlccHoy);
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            String.valueOf(jlccCuota.getNumero()),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            jlccCuota.getConcepto(),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            JlccDocumentoPdf.jlccFecha(jlccCuota.getFechaVencimiento()),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            JlccDocumentoPdf.jlccSoles(jlccCuota.getMontoCuota()),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            JlccDocumentoPdf.jlccSoles(jlccCuota.getMontoPagado()),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            JlccDocumentoPdf.jlccSoles(jlccMoraPendiente),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            JlccDocumentoPdf.jlccSoles(jlccSaldo),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            jlccCuota.getEstado().getEtiqueta(),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
        }
        return jlccTabla;
    }

    private PdfPTable jlccTablaPagos(List<Pago> jlccListaPagos) {
        PdfPTable jlccTabla = new PdfPTable(new float[] {20, 16, 20, 18, 26});
        jlccTabla.setWidthPercentage(100);
        String[] jlccTitulos = {"RECIBO", "FECHA", "MEDIO", "MONTO", "ESTADO"};
        for (String jlccTitulo : jlccTitulos) {
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            jlccTitulo,
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.BOLD, Color.WHITE),
                            JlccDocumentoPdf.JLCC_AZUL));
        }
        if (jlccListaPagos == null || jlccListaPagos.isEmpty()) {
            PdfPCell jlccVacio =
                    JlccDocumentoPdf.jlccCelda(
                            "Sin pagos registrados para este contrato.",
                            JlccDocumentoPdf.jlccFuente(8, Font.NORMAL, JlccDocumentoPdf.JLCC_GRIS),
                            Color.WHITE);
            jlccVacio.setColspan(5);
            jlccVacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            jlccTabla.addCell(jlccVacio);
            return jlccTabla;
        }
        int jlccIndice = 0;
        for (Pago jlccPago : jlccListaPagos) {
            Color jlccFondo = jlccIndice++ % 2 == 0 ? Color.WHITE : JlccDocumentoPdf.JLCC_SUAVE;
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            jlccPago.getRecibo(),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            JlccDocumentoPdf.jlccFecha(jlccPago.getFechaPago()),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            jlccPago.getMedioPago().getEtiqueta(),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            JlccDocumentoPdf.jlccSoles(jlccPago.getMontoTotal()),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            jlccPago.getEstado() == EstadoPago.ANULADO ? "ANULADO" : "EMITIDO",
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
        }
        return jlccTabla;
    }

    private PdfPTable jlccResumen(Contrato jlccContrato) {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        BigDecimal jlccFacturado = BigDecimal.ZERO,
                jlccPagado = BigDecimal.ZERO,
                jlccMoraCobrada = BigDecimal.ZERO,
                jlccSaldo = BigDecimal.ZERO,
                jlccMoraEstimada = BigDecimal.ZERO;
        for (Cuota jlccCuota : jlccContrato.getCuotas()) {
            if (jlccCuota.getEstado() == EstadoCuota.ANULADA) continue;
            jlccFacturado = jlccFacturado.add(jlccCuota.getMontoCuota());
            jlccPagado = jlccPagado.add(jlccCuota.getMontoPagado());
            jlccMoraCobrada = jlccMoraCobrada.add(jlccCuota.getMoraCobrada());
            jlccSaldo = jlccSaldo.add(jlccMora.jlccSaldoCapital(jlccCuota));
            jlccMoraEstimada = jlccMoraEstimada.add(jlccMora.jlccPendiente(jlccCuota, jlccContrato, jlccHoy));
        }
        PdfPTable jlccTabla = new PdfPTable(new float[] {55, 45});
        jlccTabla.setWidthPercentage(100);
        PdfPCell jlccNota = JlccDocumentoPdf.jlccCeldaSinBorde();
        jlccNota.addElement(
                new Paragraph(
                        "Resumen del contrato",
                        JlccDocumentoPdf.jlccFuente(9, Font.BOLD, JlccDocumentoPdf.JLCC_AZUL)));
        jlccNota.addElement(
                new Paragraph(
                        "La mora estimada se calcula a la fecha de emisión y puede variar hasta el"
                            + " pago.",
                        JlccDocumentoPdf.jlccFuente(8, Font.NORMAL, JlccDocumentoPdf.JLCC_GRIS)));
        jlccNota.addElement(
                new Paragraph(
                        "Documento interno para control administrativo.",
                        JlccDocumentoPdf.jlccFuente(8, Font.NORMAL, JlccDocumentoPdf.JLCC_GRIS)));
        jlccTabla.addCell(jlccNota);
        PdfPTable jlccValores = new PdfPTable(new float[] {58, 42});
        jlccValores.setWidthPercentage(100);
        jlccFilaResumen(jlccValores, "Total facturado", jlccFacturado, false);
        jlccFilaResumen(jlccValores, "Total pagado", jlccPagado, false);
        jlccFilaResumen(jlccValores, "Mora cobrada", jlccMoraCobrada, false);
        jlccFilaResumen(jlccValores, "Saldo pendiente", jlccSaldo, true);
        jlccFilaResumen(jlccValores, "Mora estimada a hoy", jlccMoraEstimada, false);
        jlccTabla.addCell(new PdfPCell(jlccValores));
        return jlccTabla;
    }

    private void jlccFilaResumen(
            PdfPTable jlccTabla, String jlccEtiqueta, BigDecimal jlccValor, boolean jlccDestacado) {
        Color jlccFondo = jlccDestacado ? JlccDocumentoPdf.JLCC_AZUL : Color.WHITE;
        Font jlccFuente =
                JlccDocumentoPdf.jlccFuente(
                        9,
                        jlccDestacado ? Font.BOLD : Font.NORMAL,
                        jlccDestacado ? Color.WHITE : Color.DARK_GRAY);
        jlccTabla.addCell(JlccDocumentoPdf.jlccCelda(jlccEtiqueta, jlccFuente, jlccFondo));
        jlccTabla.addCell(JlccDocumentoPdf.jlccCelda(JlccDocumentoPdf.jlccSoles(jlccValor), jlccFuente, jlccFondo));
    }

    private void jlccDato(PdfPTable jlccTabla, String jlccEtiqueta, String jlccValor) {
        PdfPCell jlccCeldaEtiqueta = JlccDocumentoPdf.jlccCeldaSinBorde();
        jlccCeldaEtiqueta.setPhrase(
                new Phrase(jlccEtiqueta, JlccDocumentoPdf.jlccFuente(7.5f, Font.BOLD, JlccDocumentoPdf.JLCC_GRIS)));
        jlccTabla.addCell(jlccCeldaEtiqueta);
        PdfPCell jlccCeldaValor = JlccDocumentoPdf.jlccCeldaSinBorde();
        jlccCeldaValor.setPhrase(
                new Phrase(
                        JlccDocumentoPdf.jlccTexto(jlccValor),
                        JlccDocumentoPdf.jlccFuente(8.5f, Font.NORMAL, Color.DARK_GRAY)));
        jlccTabla.addCell(jlccCeldaValor);
    }
}
