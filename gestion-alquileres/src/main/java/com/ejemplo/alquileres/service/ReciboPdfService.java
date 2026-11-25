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
public class ReciboPdfService {
    private static final Color AZUL = new Color(42, 120, 214),
            GRIS = new Color(101, 109, 118),
            BORDE = new Color(216, 222, 228),
            SUAVE = new Color(246, 248, 250);
    private static final DecimalFormat MONEDA =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final PagoService pagos;
    private final DatosArrendador arrendador;

    @Transactional(readOnly = true)
    public byte[] generar(Long pagoId) {
        return escribir(pagos.obtener(pagoId));
    }

    public byte[] escribir(Pago pago) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 38, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, salida);
            writer.setPageEvent(new Pie(pago, arrendador.leyendaRecibo()));
            doc.open();
            doc.add(cabecera(pago));
            doc.add(espacio(12));
            doc.add(datos(pago));
            doc.add(espacio(12));
            doc.add(detalles(pago));
            doc.add(espacio(10));
            doc.add(totales(pago));
            doc.add(espacio(10));
            PdfPCell letras =
                    celda(
                            "SON: " + NumeroALetras.importe(pago.getMontoTotal()),
                            fuente(9, Font.BOLD, AZUL),
                            new Color(234, 242, 252));
            letras.setPadding(8);
            PdfPTable caja = new PdfPTable(1);
            caja.setWidthPercentage(100);
            caja.addCell(letras);
            doc.add(caja);
            doc.add(espacio(22));
            PdfPTable firmas = new PdfPTable(2);
            firmas.setWidthPercentage(100);
            firmas.addCell(firma("RECIBÍ CONFORME"));
            firmas.addCell(firma("ENTREGUÉ CONFORME"));
            doc.add(firmas);
            doc.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el recibo PDF", e);
        }
    }

    private PdfPTable cabecera(Pago p) {
        PdfPTable tabla = new PdfPTable(new float[] {60, 40});
        tabla.setWidthPercentage(100);
        PdfPCell emisor = sinBorde();
        DocumentoPdf.agregarLogo(emisor, arrendador.logo());
        emisor.addElement(new Paragraph(arrendador.razonSocial(), fuente(18, Font.BOLD, AZUL)));
        emisor.addElement(
                new Paragraph(
                        arrendador.documentoConTipo(), fuente(9, Font.BOLD, Color.DARK_GRAY)));
        emisor.addElement(new Paragraph(arrendador.direccion(), fuente(8.5f, Font.NORMAL, GRIS)));
        emisor.addElement(
                new Paragraph(
                        arrendador.telefono() + "  ·  " + arrendador.email(),
                        fuente(8.5f, Font.NORMAL, GRIS)));
        tabla.addCell(emisor);
        PdfPTable recibo = new PdfPTable(1);
        recibo.setWidthPercentage(100);
        recibo.addCell(celda("RECIBO DE ALQUILER", fuente(10, Font.BOLD, Color.WHITE), AZUL));
        recibo.addCell(celda(p.getRecibo(), fuente(15, Font.BOLD, AZUL), Color.WHITE));
        PdfPCell marco = new PdfPCell(recibo);
        marco.setPadding(0);
        marco.setBorderColor(AZUL);
        marco.setBorderWidth(1.2f);
        tabla.addCell(marco);
        return tabla;
    }

    private PdfPTable datos(Pago p) {
        PdfPTable tabla = new PdfPTable(new float[] {20, 30, 20, 30});
        tabla.setWidthPercentage(100);
        dato(tabla, "ARRENDATARIO", p.getArrendatarioNombre());
        dato(tabla, "FECHA", p.getFechaPago().format(FECHA));
        dato(tabla, "DOCUMENTO", p.getArrendatarioDocumento());
        dato(tabla, "MEDIO", p.getMedioPago().getEtiqueta());
        dato(tabla, "INMUEBLE", p.getInmuebleCodigo() + " · " + p.getInmuebleNombre());
        dato(tabla, "REFERENCIA", vacio(p.getReferencia()));
        PdfPCell c = new PdfPCell(tabla);
        c.setBorderColor(BORDE);
        c.setBackgroundColor(SUAVE);
        c.setPadding(8);
        PdfPTable marco = new PdfPTable(1);
        marco.setWidthPercentage(100);
        marco.addCell(c);
        return marco;
    }

    private PdfPTable detalles(Pago p) {
        PdfPTable t = new PdfPTable(new float[] {7, 39, 16, 19, 19});
        t.setWidthPercentage(100);
        String[] titulos = {"#", "CONCEPTO / PERIODO", "VENCIMIENTO", "CAPITAL", "MORA"};
        for (String titulo : titulos)
            t.addCell(celda(titulo, fuente(8, Font.BOLD, Color.WHITE), AZUL));
        int fila = 0;
        for (PagoDetalle d : p.getDetalles()) {
            Color fondo = fila++ % 2 == 0 ? Color.WHITE : SUAVE;
            t.addCell(
                    celda(
                            String.valueOf(d.getItem()),
                            fuente(8, Font.NORMAL, Color.DARK_GRAY),
                            fondo));
            t.addCell(
                    celda(
                            d.getConcepto()
                                    + " · "
                                    + d.getPeriodo().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                            fuente(8, Font.NORMAL, Color.DARK_GRAY),
                            fondo));
            t.addCell(
                    celda(
                            d.getFechaVencimiento().format(FECHA),
                            fuente(8, Font.NORMAL, Color.DARK_GRAY),
                            fondo));
            t.addCell(
                    celda(
                            soles(d.getMontoCapital()),
                            fuente(8, Font.NORMAL, Color.DARK_GRAY),
                            fondo));
            t.addCell(
                    celda(soles(d.getMontoMora()), fuente(8, Font.NORMAL, Color.DARK_GRAY), fondo));
        }
        return t;
    }

    private PdfPTable totales(Pago p) {
        PdfPTable externo = new PdfPTable(new float[] {55, 45});
        externo.setWidthPercentage(100);
        PdfPCell nota = sinBorde();
        nota.addElement(
                new Paragraph(
                        "Saldo pendiente del contrato: " + soles(saldoContrato(p)),
                        fuente(9, Font.NORMAL, GRIS)));
        if (p.getObservacion() != null && !p.getObservacion().isBlank())
            nota.addElement(
                    new Paragraph(
                            "Observación: " + p.getObservacion(), fuente(8.5f, Font.NORMAL, GRIS)));
        externo.addCell(nota);
        PdfPTable t = new PdfPTable(new float[] {60, 40});
        t.setWidthPercentage(100);
        fila(t, "Capital", soles(p.getMontoCapital()), false);
        fila(t, "Mora cobrada", soles(p.getMontoMora()), false);
        fila(t, "TOTAL RECIBIDO", soles(p.getMontoTotal()), true);
        externo.addCell(new PdfPCell(t));
        return externo;
    }

    private BigDecimal saldoContrato(Pago p) {
        return p.getContrato().getCuotas().stream()
                .map(c -> c.getMontoCuota().subtract(c.getMontoPagado()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void dato(PdfPTable t, String etiqueta, String valor) {
        PdfPCell e = sinBorde();
        e.setPhrase(new Phrase(etiqueta, fuente(7.5f, Font.BOLD, GRIS)));
        t.addCell(e);
        PdfPCell v = sinBorde();
        v.setPhrase(new Phrase(vacio(valor), fuente(8.5f, Font.NORMAL, Color.DARK_GRAY)));
        t.addCell(v);
    }

    private void fila(PdfPTable t, String e, String v, boolean destacado) {
        Color fondo = destacado ? AZUL : Color.WHITE;
        Font f =
                fuente(
                        9,
                        destacado ? Font.BOLD : Font.NORMAL,
                        destacado ? Color.WHITE : Color.DARK_GRAY);
        t.addCell(celda(e, f, fondo));
        t.addCell(celda(v, f, fondo));
    }

    private PdfPCell firma(String texto) {
        PdfPCell c = sinBorde();
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(25);
        c.setPhrase(
                new Phrase(
                        "_______________________________\n" + texto, fuente(8, Font.NORMAL, GRIS)));
        return c;
    }

    private static PdfPCell celda(String valor, Font f, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(vacio(valor), f));
        c.setBackgroundColor(fondo);
        c.setBorderColor(BORDE);
        c.setPadding(6);
        return c;
    }

    private static PdfPCell sinBorde() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private static Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    private static Font fuente(float n, int estilo, Color color) {
        return new Font(Font.HELVETICA, n, estilo, color);
    }

    private static String soles(BigDecimal v) {
        return "S/ " + MONEDA.format(v);
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static class Pie extends PdfPageEventHelper {
        private final Pago pago;
        private final String leyenda;

        Pie(Pago p, String l) {
            pago = p;
            leyenda = l;
        }

        @Override
        public void onEndPage(PdfWriter w, Document d) {
            PdfContentByte cb = w.getDirectContent();
            ColumnText.showTextAligned(
                    cb,
                    Element.ALIGN_CENTER,
                    new Phrase(
                            leyenda == null || leyenda.isBlank()
                                    ? "Documento interno sin validez tributaria"
                                    : leyenda,
                            fuente(8, Font.NORMAL, GRIS)),
                    (d.left() + d.right()) / 2,
                    d.bottom() - 20,
                    0);
            if (pago.getEstado() == EstadoPago.ANULADO) {
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
