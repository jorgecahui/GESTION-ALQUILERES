package com.ejemplo.alquileres.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class JlccDocumentoPdf {
    private JlccDocumentoPdf() {}

    static final Color JLCC_AZUL = new Color(42, 120, 214);
    static final Color JLCC_GRIS = new Color(101, 109, 118);
    static final Color JLCC_BORDE = new Color(216, 222, 228);
    static final Color JLCC_SUAVE = new Color(246, 248, 250);
    private static final DecimalFormat JLCC_MONEDA =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DateTimeFormatter JLCC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static Document jlccDocumento() {
        return new Document(PageSize.A4, 40, 40, 38, 52);
    }

    static Font jlccFuente(float jlccTamano, int jlccEstilo, Color jlccColor) {
        return new Font(Font.HELVETICA, jlccTamano, jlccEstilo, jlccColor);
    }

    static PdfPCell jlccCelda(String jlccValor, Font jlccFuente, Color jlccFondo) {
        PdfPCell jlccCelda = new PdfPCell(new Phrase(jlccTexto(jlccValor), jlccFuente));
        jlccCelda.setBackgroundColor(jlccFondo);
        jlccCelda.setBorderColor(JLCC_BORDE);
        jlccCelda.setPadding(6);
        return jlccCelda;
    }

    static PdfPCell jlccCeldaSinBorde() {
        PdfPCell jlccCelda = new PdfPCell();
        jlccCelda.setBorder(Rectangle.NO_BORDER);
        return jlccCelda;
    }

    static Paragraph jlccEspacio(float jlccAlto) {
        Paragraph jlccParrafo = new Paragraph(" ");
        jlccParrafo.setLeading(jlccAlto);
        return jlccParrafo;
    }

    static void jlccAgregarLogo(PdfPCell jlccCelda, Path jlccRuta) {
        if (jlccCelda == null || jlccRuta == null || !Files.isRegularFile(jlccRuta)) return;
        try {
            Image jlccImagen = Image.getInstance(jlccRuta.toAbsolutePath().toString());
            jlccImagen.scaleToFit(90, 46);
            jlccCelda.addElement(jlccImagen);
        } catch (Exception jlccIgnorada) {
        }
    }

    static String jlccSoles(BigDecimal jlccValor) {
        return "S/ " + JLCC_MONEDA.format(jlccValor == null ? BigDecimal.ZERO : jlccValor);
    }

    static String jlccFecha(LocalDate jlccFecha) {
        return jlccFecha == null ? "—" : jlccFecha.format(JLCC_FECHA);
    }

    static String jlccTexto(String jlccValor) {
        return jlccValor == null || jlccValor.isBlank() ? "—" : jlccValor;
    }

    static void jlccAgregarPie(Document jlccDocumento, PdfWriter jlccWriter, String jlccNota) {
        jlccWriter.setPageEvent(new JlccPie(jlccNota));
    }

    private static final class JlccPie extends PdfPageEventHelper {
        private final String jlccNota;

        private JlccPie(String jlccNota) {
            this.jlccNota = jlccNota;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            PdfContentByte jlccLienzo = writer.getDirectContent();
            ColumnText.showTextAligned(
                    jlccLienzo,
                    Element.ALIGN_CENTER,
                    new Phrase(jlccNota, jlccFuente(8, Font.NORMAL, JLCC_GRIS)),
                    (documento.left() + documento.right()) / 2,
                    documento.bottom() - 22,
                    0);
            ColumnText.showTextAligned(
                    jlccLienzo,
                    Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), jlccFuente(8, Font.NORMAL, JLCC_GRIS)),
                    documento.right(),
                    documento.bottom() - 22,
                    0);
        }
    }
}
