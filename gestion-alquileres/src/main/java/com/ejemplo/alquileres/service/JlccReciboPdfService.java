package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.model.EstadoPago;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.model.PagoDetalle;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class JlccReciboPdfService {
    private static final Color JLCC_AZUL = new Color(42, 120, 214),
            JLCC_GRIS = new Color(101, 109, 118),
            JLCC_BORDE = new Color(216, 222, 228),
            JLCC_SUAVE = new Color(246, 248, 250);
    private static final DecimalFormat JLCC_MONEDA =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DateTimeFormatter JLCC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JlccPagoService jlccPagos;
    private final DatosArrendador jlccArrendador;

    @Transactional(readOnly = true)
    public byte[] jlccGenerar(Long jlccPagoId) {
        return jlccEscribir(jlccPagos.jlccObtener(jlccPagoId));
    }

    public byte[] jlccEscribir(Pago jlccPago) {
        try (ByteArrayOutputStream jlccSalida = new ByteArrayOutputStream()) {
            Document jlccDoc = new Document(PageSize.A4, 40, 40, 38, 50);
            PdfWriter jlccWriter = PdfWriter.getInstance(jlccDoc, jlccSalida);
            jlccWriter.setPageEvent(new JlccPie(jlccPago, jlccArrendador.leyendaRecibo()));
            jlccDoc.open();
            jlccDoc.add(jlccCabecera(jlccPago));
            jlccDoc.add(jlccEspacio(12));
            jlccDoc.add(jlccDatos(jlccPago));
            jlccDoc.add(jlccEspacio(12));
            jlccDoc.add(jlccDetalles(jlccPago));
            jlccDoc.add(jlccEspacio(10));
            jlccDoc.add(jlccTotales(jlccPago));
            jlccDoc.add(jlccEspacio(10));
            PdfPCell jlccLetras =
                    jlccCelda(
                            "SON: " + JlccNumeroALetras.jlccImporte(jlccPago.getMontoTotal()),
                            jlccFuente(9, Font.BOLD, JLCC_AZUL),
                            new Color(234, 242, 252));
            jlccLetras.setPadding(8);
            PdfPTable jlccCaja = new PdfPTable(1);
            jlccCaja.setWidthPercentage(100);
            jlccCaja.addCell(jlccLetras);
            jlccDoc.add(jlccCaja);
            jlccDoc.add(jlccEspacio(22));
            PdfPTable jlccFirmas = new PdfPTable(2);
            jlccFirmas.setWidthPercentage(100);
            jlccFirmas.addCell(jlccFirma("RECIBÍ CONFORME"));
            jlccFirmas.addCell(jlccFirma("ENTREGUÉ CONFORME"));
            jlccDoc.add(jlccFirmas);
            jlccDoc.close();
            return jlccSalida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el recibo PDF", e);
        }
    }

    private PdfPTable jlccCabecera(Pago p) {
        PdfPTable jlccTabla = new PdfPTable(new float[] {60, 40});
        jlccTabla.setWidthPercentage(100);
        PdfPCell jlccEmisor = jlccSinBorde();
        JlccDocumentoPdf.jlccAgregarLogo(jlccEmisor, jlccArrendador.logo());
        jlccEmisor.addElement(new Paragraph(jlccArrendador.razonSocial(), jlccFuente(18, Font.BOLD, JLCC_AZUL)));
        jlccEmisor.addElement(
                new Paragraph(
                        jlccArrendador.documentoConTipo(), jlccFuente(9, Font.BOLD, Color.DARK_GRAY)));
        jlccEmisor.addElement(new Paragraph(jlccArrendador.direccion(), jlccFuente(8.5f, Font.NORMAL, JLCC_GRIS)));
        jlccEmisor.addElement(
                new Paragraph(
                        jlccArrendador.telefono() + "  ·  " + jlccArrendador.email(),
                        jlccFuente(8.5f, Font.NORMAL, JLCC_GRIS)));
        jlccTabla.addCell(jlccEmisor);
        PdfPTable jlccRecibo = new PdfPTable(1);
        jlccRecibo.setWidthPercentage(100);
        jlccRecibo.addCell(jlccCelda("RECIBO DE ALQUILER", jlccFuente(10, Font.BOLD, Color.WHITE), JLCC_AZUL));
        jlccRecibo.addCell(jlccCelda(p.getRecibo(), jlccFuente(15, Font.BOLD, JLCC_AZUL), Color.WHITE));
        PdfPCell jlccMarco = new PdfPCell(jlccRecibo);
        jlccMarco.setPadding(0);
        jlccMarco.setBorderColor(JLCC_AZUL);
        jlccMarco.setBorderWidth(1.2f);
        jlccTabla.addCell(jlccMarco);
        return jlccTabla;
    }

    private PdfPTable jlccDatos(Pago p) {
        PdfPTable jlccTabla = new PdfPTable(new float[] {20, 30, 20, 30});
        jlccTabla.setWidthPercentage(100);
        jlccDato(jlccTabla, "ARRENDATARIO", p.getArrendatarioNombre());
        jlccDato(jlccTabla, "FECHA", p.getFechaPago().format(JLCC_FECHA));
        jlccDato(jlccTabla, "DOCUMENTO", p.getArrendatarioDocumento());
        jlccDato(jlccTabla, "MEDIO", p.getMedioPago().getEtiqueta());
        jlccDato(jlccTabla, "INMUEBLE", p.getInmuebleCodigo() + " · " + p.getInmuebleNombre());
        jlccDato(jlccTabla, "REFERENCIA", jlccVacio(p.getReferencia()));
        PdfPCell c = new PdfPCell(jlccTabla);
        c.setBorderColor(JLCC_BORDE);
        c.setBackgroundColor(JLCC_SUAVE);
        c.setPadding(8);
        PdfPTable jlccMarco = new PdfPTable(1);
        jlccMarco.setWidthPercentage(100);
        jlccMarco.addCell(c);
        return jlccMarco;
    }

    private PdfPTable jlccDetalles(Pago p) {
        PdfPTable t = new PdfPTable(new float[] {7, 39, 16, 19, 19});
        t.setWidthPercentage(100);
        String[] jlccTitulos = {"#", "CONCEPTO / PERIODO", "VENCIMIENTO", "CAPITAL", "MORA"};
        for (String jlccTitulo : jlccTitulos)
            t.addCell(jlccCelda(jlccTitulo, jlccFuente(8, Font.BOLD, Color.WHITE), JLCC_AZUL));
        int jlccFila = 0;
        for (PagoDetalle d : p.getDetalles()) {
            Color jlccFondo = jlccFila++ % 2 == 0 ? Color.WHITE : JLCC_SUAVE;
            t.addCell(
                    jlccCelda(
                            String.valueOf(d.getItem()),
                            jlccFuente(8, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            t.addCell(
                    jlccCelda(
                            d.getConcepto()
                                    + " · "
                                    + d.getPeriodo().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                            jlccFuente(8, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            t.addCell(
                    jlccCelda(
                            d.getFechaVencimiento().format(JLCC_FECHA),
                            jlccFuente(8, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            t.addCell(
                    jlccCelda(
                            jlccSoles(d.getMontoCapital()),
                            jlccFuente(8, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
            t.addCell(
                    jlccCelda(jlccSoles(d.getMontoMora()), jlccFuente(8, Font.NORMAL, Color.DARK_GRAY), jlccFondo));
        }
        return t;
    }

    private PdfPTable jlccTotales(Pago p) {
        PdfPTable jlccExterno = new PdfPTable(new float[] {55, 45});
        jlccExterno.setWidthPercentage(100);
        PdfPCell jlccNota = jlccSinBorde();
        jlccNota.addElement(
                new Paragraph(
                        "Saldo pendiente del contrato: " + jlccSoles(jlccSaldoContrato(p)),
                        jlccFuente(9, Font.NORMAL, JLCC_GRIS)));
        if (p.getObservacion() != null && !p.getObservacion().isBlank())
            jlccNota.addElement(
                    new Paragraph(
                            "Observación: " + p.getObservacion(), jlccFuente(8.5f, Font.NORMAL, JLCC_GRIS)));
        jlccExterno.addCell(jlccNota);
        PdfPTable t = new PdfPTable(new float[] {60, 40});
        t.setWidthPercentage(100);
        jlccFila(t, "Capital", jlccSoles(p.getMontoCapital()), false);
        jlccFila(t, "Mora cobrada", jlccSoles(p.getMontoMora()), false);
        jlccFila(t, "TOTAL RECIBIDO", jlccSoles(p.getMontoTotal()), true);
        jlccExterno.addCell(new PdfPCell(t));
        return jlccExterno;
    }

    private BigDecimal jlccSaldoContrato(Pago p) {
        return p.getContrato().getCuotas().stream()
                .map(c -> c.getMontoCuota().subtract(c.getMontoPagado()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void jlccDato(PdfPTable t, String jlccEtiqueta, String jlccValor) {
        PdfPCell e = jlccSinBorde();
        e.setPhrase(new Phrase(jlccEtiqueta, jlccFuente(7.5f, Font.BOLD, JLCC_GRIS)));
        t.addCell(e);
        PdfPCell v = jlccSinBorde();
        v.setPhrase(new Phrase(jlccVacio(jlccValor), jlccFuente(8.5f, Font.NORMAL, Color.DARK_GRAY)));
        t.addCell(v);
    }

    private void jlccFila(PdfPTable t, String e, String v, boolean jlccDestacado) {
        Color jlccFondo = jlccDestacado ? JLCC_AZUL : Color.WHITE;
        Font f =
                jlccFuente(
                        9,
                        jlccDestacado ? Font.BOLD : Font.NORMAL,
                        jlccDestacado ? Color.WHITE : Color.DARK_GRAY);
        t.addCell(jlccCelda(e, f, jlccFondo));
        t.addCell(jlccCelda(v, f, jlccFondo));
    }

    private PdfPCell jlccFirma(String jlccTexto) {
        PdfPCell c = jlccSinBorde();
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(25);
        c.setPhrase(
                new Phrase(
                        "_______________________________\n" + jlccTexto, jlccFuente(8, Font.NORMAL, JLCC_GRIS)));
        return c;
    }

    private static PdfPCell jlccCelda(String jlccValor, Font f, Color jlccFondo) {
        PdfPCell c = new PdfPCell(new Phrase(jlccVacio(jlccValor), f));
        c.setBackgroundColor(jlccFondo);
        c.setBorderColor(JLCC_BORDE);
        c.setPadding(6);
        return c;
    }

    private static PdfPCell jlccSinBorde() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private static Paragraph jlccEspacio(float jlccAlto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(jlccAlto);
        return p;
    }

    private static Font jlccFuente(float n, int jlccEstilo, Color jlccColor) {
        return new Font(Font.HELVETICA, n, jlccEstilo, jlccColor);
    }

    private static String jlccSoles(BigDecimal v) {
        return "S/ " + JLCC_MONEDA.format(v);
    }

    private static String jlccVacio(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static class JlccPie extends PdfPageEventHelper {
        private final Pago jlccPago;
        private final String jlccLeyenda;

        JlccPie(Pago p, String l) {
            jlccPago = p;
            jlccLeyenda = l;
        }

        @Override
        public void onEndPage(PdfWriter w, Document d) {
            PdfContentByte jlccCb = w.getDirectContent();
            ColumnText.showTextAligned(
                    jlccCb,
                    Element.ALIGN_CENTER,
                    new Phrase(
                            jlccLeyenda == null || jlccLeyenda.isBlank()
                                    ? "Documento interno sin validez tributaria"
                                    : jlccLeyenda,
                            jlccFuente(8, Font.NORMAL, JLCC_GRIS)),
                    (d.left() + d.right()) / 2,
                    d.bottom() - 20,
                    0);
            if (jlccPago.getEstado() == EstadoPago.ANULADO) {
                PdfContentByte b = w.getDirectContentUnder();
                b.saveState();
                PdfGState g = new PdfGState();
                g.setFillOpacity(.12f);
                b.setGState(g);
                try {
                    b.beginText();
                    b.setFontAndSize(
                            BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false),
                            105);
                    b.setColorFill(new Color(207, 34, 46));
                    b.showTextAligned(
                            Element.ALIGN_CENTER,
                            "ANULADO",
                            (d.left() + d.right()) / 2,
                            (d.top() + d.bottom()) / 2,
                            40);
                    b.endText();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                b.restoreState();
            }
        }
    }
}
