package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.TipoMora;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hoja resumen del contrato con partes, inmueble, condiciones y cronograma. */
@Service
@RequiredArgsConstructor
public class ContratoPdfService {
    private final ContratoRepository contratos;
    private final DatosArrendador arrendador;
    private final Clock clock;

    @Transactional(readOnly = true)
    public byte[] generar(Long contratoId) {
        Contrato contrato = contratos.detalle(contratoId)
                .orElseThrow(() -> new NegocioException("El contrato no existe."));
        return escribir(contrato);
    }

    public byte[] escribir(Contrato contrato) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = DocumentoPdf.documento();
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            DocumentoPdf.agregarPie(documento, writer, "Hoja resumen. No reemplaza el contrato firmado por las partes.");
            documento.open();
            documento.add(cabecera(contrato));
            documento.add(DocumentoPdf.espacio(12));
            documento.add(seccionPartes(contrato));
            documento.add(DocumentoPdf.espacio(12));
            documento.add(seccionInmueble(contrato));
            documento.add(DocumentoPdf.espacio(12));
            documento.add(seccionCondiciones(contrato));
            documento.add(DocumentoPdf.espacio(12));
            documento.add(titulo("CRONOGRAMA DE CUOTAS"));
            documento.add(tablaCronograma(contrato));
            documento.add(DocumentoPdf.espacio(18));
            documento.add(notaFinal());
            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar la hoja de contrato PDF.", e);
        }
    }

    private PdfPTable cabecera(Contrato contrato) {
        PdfPTable tabla = new PdfPTable(new float[]{62, 38});
        tabla.setWidthPercentage(100);
        PdfPCell emisor = DocumentoPdf.celdaSinBorde();
        DocumentoPdf.agregarLogo(emisor, arrendador.logo());
        emisor.addElement(new Paragraph(arrendador.razonSocial(), DocumentoPdf.fuente(18, Font.BOLD, DocumentoPdf.AZUL)));
        emisor.addElement(new Paragraph(arrendador.documentoConTipo(), DocumentoPdf.fuente(9, Font.BOLD, Color.DARK_GRAY)));
        emisor.addElement(new Paragraph(arrendador.direccion(), DocumentoPdf.fuente(8.5f, Font.NORMAL, DocumentoPdf.GRIS)));
        emisor.addElement(new Paragraph(arrendador.telefono() + "  ·  " + arrendador.email(), DocumentoPdf.fuente(8.5f, Font.NORMAL, DocumentoPdf.GRIS)));
        tabla.addCell(emisor);
        PdfPTable caja = new PdfPTable(1);
        caja.setWidthPercentage(100);
        caja.addCell(DocumentoPdf.celda("HOJA DE CONTRATO", DocumentoPdf.fuente(10, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL));
        caja.addCell(DocumentoPdf.celda(contrato.getCodigo(), DocumentoPdf.fuente(15, Font.BOLD, DocumentoPdf.AZUL), Color.WHITE));
        PdfPCell marco = new PdfPCell(caja);
        marco.setPadding(0);
        marco.setBorderColor(DocumentoPdf.AZUL);
        marco.setBorderWidth(1.2f);
        tabla.addCell(marco);
        return tabla;
    }

    private PdfPTable seccionPartes(Contrato contrato) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.addCell(DocumentoPdf.celda("PARTES DEL CONTRATO", DocumentoPdf.fuente(9, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL));
        PdfPTable contenido = new PdfPTable(new float[]{18, 32, 18, 32});
        contenido.setWidthPercentage(100);
        dato(contenido, "ARRENDADOR", arrendador.razonSocial());
        dato(contenido, "DOCUMENTO", arrendador.documento());
        dato(contenido, "ARRENDATARIO", contrato.getArrendatario().getNombreCompleto());
        dato(contenido, "DOCUMENTO", contrato.getArrendatario().getTipoDocumento() + " " + contrato.getArrendatario().getNumeroDocumento());
        dato(contenido, "TELÉFONO", contrato.getArrendatario().getTelefono());
        dato(contenido, "EMAIL", contrato.getArrendatario().getEmail());
        dato(contenido, "GARANTE", contrato.getGarante() == null ? "Sin garante" : contrato.getGarante().getNombreCompleto());
        dato(contenido, "DOCUMENTO", contrato.getGarante() == null ? "—" : contrato.getGarante().getTipoDocumento() + " " + contrato.getGarante().getNumeroDocumento());
        PdfPCell caja = new PdfPCell(contenido);
        caja.setPadding(8);
        caja.setBorderColor(DocumentoPdf.BORDE);
        tabla.addCell(caja);
        return tabla;
    }

    private PdfPTable seccionInmueble(Contrato contrato) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.addCell(DocumentoPdf.celda("INMUEBLE ARRENDADO", DocumentoPdf.fuente(9, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL));
        PdfPTable contenido = new PdfPTable(new float[]{18, 32, 18, 32});
        contenido.setWidthPercentage(100);
        dato(contenido, "CÓDIGO", contrato.getInmueble().getCodigo());
        dato(contenido, "NOMBRE", contrato.getInmueble().getNombre());
        dato(contenido, "DIRECCIÓN", contrato.getInmueble().getDireccion());
        dato(contenido, "DISTRITO", contrato.getInmueble().getDistrito());
        dato(contenido, "ÁREA", contrato.getInmueble().getAreaM2() == null ? "—" : contrato.getInmueble().getAreaM2() + " m²");
        dato(contenido, "CARACTERÍSTICAS", caracteristicas(contrato));
        dato(contenido, "SERVICIOS", contrato.getInmueble().getServiciosIncluidos());
        dato(contenido, "TIPO", contrato.getInmueble().getTipo() == null ? "—" : contrato.getInmueble().getTipo().getNombre());
        PdfPCell caja = new PdfPCell(contenido);
        caja.setPadding(8);
        caja.setBorderColor(DocumentoPdf.BORDE);
        tabla.addCell(caja);
        return tabla;
    }

    private PdfPTable seccionCondiciones(Contrato contrato) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.addCell(DocumentoPdf.celda("CONDICIONES ECONÓMICAS", DocumentoPdf.fuente(9, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL));
        PdfPTable contenido = new PdfPTable(new float[]{22, 28, 22, 28});
        contenido.setWidthPercentage(100);
        dato(contenido, "VIGENCIA", DocumentoPdf.fecha(contrato.getFechaInicio()) + " al " + DocumentoPdf.fecha(contrato.getFechaFin()));
        dato(contenido, "DÍA DE PAGO", "Día " + contrato.getDiaPago() + " de cada mes");
        dato(contenido, "RENTA MENSUAL", DocumentoPdf.soles(contrato.getRentaMensual()));
        dato(contenido, "INCREMENTO ANUAL", contrato.getIncrementoAnualPct() + " %");
        dato(contenido, "GARANTÍA", DocumentoPdf.soles(contrato.getGarantiaMonto()) + " · " + estadoGarantia(contrato));
        dato(contenido, "POLÍTICA DE MORA", politicaMora(contrato));
        dato(contenido, "DÍAS DE GRACIA", String.valueOf(contrato.getDiasGracia()));
        dato(contenido, "EMITIDO", DocumentoPdf.fecha(LocalDate.now(clock)));
        PdfPCell caja = new PdfPCell(contenido);
        caja.setPadding(8);
        caja.setBorderColor(DocumentoPdf.BORDE);
        tabla.addCell(caja);
        if (contrato.getObservaciones() != null && !contrato.getObservaciones().isBlank()) {
            PdfPCell observaciones = DocumentoPdf.celda("Observaciones: " + contrato.getObservaciones(),
                    DocumentoPdf.fuente(8, Font.NORMAL, DocumentoPdf.GRIS), DocumentoPdf.SUAVE);
            observaciones.setColspan(4);
            tabla.addCell(observaciones);
        }
        return tabla;
    }

    private PdfPTable titulo(String texto) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell celda = DocumentoPdf.celda(texto, DocumentoPdf.fuente(9, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL);
        celda.setPaddingTop(5);
        celda.setPaddingBottom(5);
        tabla.addCell(celda);
        return tabla;
    }

    private PdfPTable tablaCronograma(Contrato contrato) {
        PdfPTable tabla = new PdfPTable(new float[]{6, 32, 16, 16, 16, 14});
        tabla.setWidthPercentage(100);
        for (String titulo : new String[]{"#", "CONCEPTO", "PERIODO", "VENCIMIENTO", "MONTO", "ESTADO"}) {
            tabla.addCell(DocumentoPdf.celda(titulo, DocumentoPdf.fuente(7.5f, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL));
        }
        List<Cuota> cuotas = contrato.getCuotas().stream()
                .sorted(Comparator.comparing(Cuota::getFechaVencimiento).thenComparing(Cuota::getNumero)).toList();
        int indice = 0;
        for (Cuota cuota : cuotas) {
            Color fondo = indice++ % 2 == 0 ? Color.WHITE : DocumentoPdf.SUAVE;
            tabla.addCell(DocumentoPdf.celda(String.valueOf(cuota.getNumero()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(cuota.getConcepto(), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(cuota.getPeriodo() == null ? "—" : cuota.getPeriodo().format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.fecha(cuota.getFechaVencimiento()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.soles(cuota.getMontoCuota()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(cuota.getEstado().getEtiqueta(), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
        }
        return tabla;
    }

    private PdfPTable notaFinal() {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell nota = DocumentoPdf.celda(
                "Hoja resumen. No reemplaza el contrato firmado por las partes.",
                DocumentoPdf.fuente(9, Font.BOLD, DocumentoPdf.GRIS), DocumentoPdf.SUAVE);
        nota.setHorizontalAlignment(Element.ALIGN_CENTER);
        nota.setPadding(10);
        tabla.addCell(nota);
        return tabla;
    }

    private String caracteristicas(Contrato contrato) {
        StringBuilder texto = new StringBuilder();
        if (contrato.getInmueble().getDormitorios() != null) texto.append(contrato.getInmueble().getDormitorios()).append(" dorm. ");
        if (contrato.getInmueble().getBanos() != null) texto.append(contrato.getInmueble().getBanos()).append(" baños ");
        if (contrato.getInmueble().getEstacionamientos() != null) texto.append(contrato.getInmueble().getEstacionamientos()).append(" estac. ");
        if (contrato.getInmueble().isAmoblado()) texto.append("amoblado");
        return texto.toString().trim();
    }

    private String estadoGarantia(Contrato contrato) {
        if (contrato.getGarantiaEstado() == null) return "—";
        return switch (contrato.getGarantiaEstado()) {
            case SIN_GARANTIA -> "sin garantía";
            case PENDIENTE -> "pendiente de entrega";
            case RECIBIDA -> "recibida";
            case DEVUELTA -> "devuelta";
            case RETENIDA_PARCIAL -> "retenida parcialmente";
        };
    }

    private String politicaMora(Contrato contrato) {
        if (contrato.getTipoMora() == null || contrato.getValorMora() == null) return "—";
        return contrato.getTipoMora() == TipoMora.PORCENTAJE_DIARIO
                ? contrato.getValorMora() + " % diario"
                : DocumentoPdf.soles(contrato.getValorMora()) + " diario";
    }

    private void dato(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = DocumentoPdf.celdaSinBorde();
        celdaEtiqueta.setPhrase(new Phrase(etiqueta, DocumentoPdf.fuente(7.5f, Font.BOLD, DocumentoPdf.GRIS)));
        tabla.addCell(celdaEtiqueta);
        PdfPCell celdaValor = DocumentoPdf.celdaSinBorde();
        celdaValor.setPhrase(new Phrase(DocumentoPdf.texto(valor), DocumentoPdf.fuente(8.5f, Font.NORMAL, Color.DARK_GRAY)));
        tabla.addCell(celdaValor);
    }
}
