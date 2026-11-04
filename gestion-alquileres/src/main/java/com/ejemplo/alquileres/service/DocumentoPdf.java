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
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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

/** Estilo compartido por los documentos PDF internos. */
final class DocumentoPdf {
    private DocumentoPdf() { }

    static final Color AZUL = new Color(42, 120, 214);
    static final Color GRIS = new Color(101, 109, 118);
    static final Color BORDE = new Color(216, 222, 228);
    static final Color SUAVE = new Color(246, 248, 250);
    private static final DecimalFormat MONEDA = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static Document documento() { return new Document(PageSize.A4, 40, 40, 38, 52); }

    static Font fuente(float tamano, int estilo, Color color) {
        return new Font(Font.HELVETICA, tamano, estilo, color);
    }

    static PdfPCell celda(String valor, Font fuente, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto(valor), fuente));
        celda.setBackgroundColor(fondo);
        celda.setBorderColor(BORDE);
        celda.setPadding(6);
        return celda;
    }

    static PdfPCell celdaSinBorde() {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    static Paragraph espacio(float alto) {
        Paragraph parrafo = new Paragraph(" ");
        parrafo.setLeading(alto);
        return parrafo;
    }

    /** Agrega el logo si la ruta existe y es una imagen válida; nunca rompe el PDF. */
    static void agregarLogo(PdfPCell celda, Path ruta) {
        if (celda == null || ruta == null || !Files.isRegularFile(ruta)) return;
        try {
            Image imagen = Image.getInstance(ruta.toAbsolutePath().toString());
            imagen.scaleToFit(90, 46);
            celda.addElement(imagen);
        } catch (Exception ignorada) {
            // Un logo ilegible no debe impedir la emisión del documento.
        }
    }

    static String soles(BigDecimal valor) {
        return "S/ " + MONEDA.format(valor == null ? BigDecimal.ZERO : valor);
    }

    static String fecha(LocalDate fecha) { return fecha == null ? "—" : fecha.format(FECHA); }

    static String texto(String valor) { return valor == null || valor.isBlank() ? "—" : valor; }

    static void agregarPie(Document documento, PdfWriter writer, String nota) {
        writer.setPageEvent(new Pie(nota));
    }

    private static final class Pie extends PdfPageEventHelper {
        private final String nota;
        private Pie(String nota) { this.nota = nota; }
        @Override public void onEndPage(PdfWriter writer, Document documento) {
            PdfContentByte lienzo = writer.getDirectContent();
            ColumnText.showTextAligned(lienzo, Element.ALIGN_CENTER,
                    new Phrase(nota, fuente(8, Font.NORMAL, GRIS)),
                    (documento.left() + documento.right()) / 2, documento.bottom() - 22, 0);
            ColumnText.showTextAligned(lienzo, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), fuente(8, Font.NORMAL, GRIS)),
                    documento.right(), documento.bottom() - 22, 0);
        }
    }
}
