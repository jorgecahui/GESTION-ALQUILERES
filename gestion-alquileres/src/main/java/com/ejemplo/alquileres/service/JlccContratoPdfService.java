package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.exception.JlccNegocioException;
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

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccContratoPdfService {
    private final ContratoRepository jlccContratos;
    private final DatosArrendador jlccArrendador;
    private final Clock jlccClock;

    @Transactional(readOnly = true)
    public byte[] jlccGenerar(Long jlccContratoId) {
        Contrato jlccContrato =
                jlccContratos
                        .detalle(jlccContratoId)
                        .orElseThrow(() -> new JlccNegocioException("El contrato no existe."));
        return jlccEscribir(jlccContrato);
    }

    public byte[] jlccEscribir(Contrato jlccContrato) {
        try (ByteArrayOutputStream jlccSalida = new ByteArrayOutputStream()) {
            Document jlccDocumento = JlccDocumentoPdf.jlccDocumento();
            PdfWriter jlccWriter = PdfWriter.getInstance(jlccDocumento, jlccSalida);
            JlccDocumentoPdf.jlccAgregarPie(
                    jlccDocumento,
                    jlccWriter,
                    "Hoja resumen. No reemplaza el contrato firmado por las partes.");
            jlccDocumento.open();
            jlccDocumento.add(jlccCabecera(jlccContrato));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(12));
            jlccDocumento.add(jlccSeccionPartes(jlccContrato));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(12));
            jlccDocumento.add(jlccSeccionInmueble(jlccContrato));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(12));
            jlccDocumento.add(jlccSeccionCondiciones(jlccContrato));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(12));
            jlccDocumento.add(jlccTitulo("CRONOGRAMA DE CUOTAS"));
            jlccDocumento.add(jlccTablaCronograma(jlccContrato));
            jlccDocumento.add(JlccDocumentoPdf.jlccEspacio(18));
            jlccDocumento.add(jlccNotaFinal());
            jlccDocumento.close();
            return jlccSalida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar la hoja de contrato PDF.", e);
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
                        "HOJA DE CONTRATO",
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

    private PdfPTable jlccSeccionPartes(Contrato jlccContrato) {
        PdfPTable jlccTabla = new PdfPTable(1);
        jlccTabla.setWidthPercentage(100);
        jlccTabla.addCell(
                JlccDocumentoPdf.jlccCelda(
                        "PARTES DEL CONTRATO",
                        JlccDocumentoPdf.jlccFuente(9, Font.BOLD, Color.WHITE),
                        JlccDocumentoPdf.JLCC_AZUL));
        PdfPTable jlccContenido = new PdfPTable(new float[] {18, 32, 18, 32});
        jlccContenido.setWidthPercentage(100);
        jlccDato(jlccContenido, "ARRENDADOR", jlccArrendador.razonSocial());
        jlccDato(jlccContenido, "DOCUMENTO", jlccArrendador.documento());
        jlccDato(jlccContenido, "ARRENDATARIO", jlccContrato.getArrendatario().getNombreCompleto());
        jlccDato(
                jlccContenido,
                "DOCUMENTO",
                jlccContrato.getArrendatario().getTipoDocumento()
                        + " "
                        + jlccContrato.getArrendatario().getNumeroDocumento());
        jlccDato(jlccContenido, "TELÉFONO", jlccContrato.getArrendatario().getTelefono());
        jlccDato(jlccContenido, "EMAIL", jlccContrato.getArrendatario().getEmail());
        jlccDato(
                jlccContenido,
                "GARANTE",
                jlccContrato.getGarante() == null
                        ? "Sin garante"
                        : jlccContrato.getGarante().getNombreCompleto());
        jlccDato(
                jlccContenido,
                "DOCUMENTO",
                jlccContrato.getGarante() == null
                        ? "—"
                        : jlccContrato.getGarante().getTipoDocumento()
                                + " "
                                + jlccContrato.getGarante().getNumeroDocumento());
        PdfPCell jlccCaja = new PdfPCell(jlccContenido);
        jlccCaja.setPadding(8);
        jlccCaja.setBorderColor(JlccDocumentoPdf.JLCC_BORDE);
        jlccTabla.addCell(jlccCaja);
        return jlccTabla;
    }

    private PdfPTable jlccSeccionInmueble(Contrato jlccContrato) {
        PdfPTable jlccTabla = new PdfPTable(1);
        jlccTabla.setWidthPercentage(100);
        jlccTabla.addCell(
                JlccDocumentoPdf.jlccCelda(
                        "INMUEBLE ARRENDADO",
                        JlccDocumentoPdf.jlccFuente(9, Font.BOLD, Color.WHITE),
                        JlccDocumentoPdf.JLCC_AZUL));
        PdfPTable jlccContenido = new PdfPTable(new float[] {18, 32, 18, 32});
        jlccContenido.setWidthPercentage(100);
        jlccDato(jlccContenido, "CÓDIGO", jlccContrato.getInmueble().getCodigo());
        jlccDato(jlccContenido, "NOMBRE", jlccContrato.getInmueble().getNombre());
        jlccDato(jlccContenido, "DIRECCIÓN", jlccContrato.getInmueble().getDireccion());
        jlccDato(jlccContenido, "DISTRITO", jlccContrato.getInmueble().getDistrito());
        jlccDato(
                jlccContenido,
                "ÁREA",
                jlccContrato.getInmueble().getAreaM2() == null
                        ? "—"
                        : jlccContrato.getInmueble().getAreaM2() + " m²");
        jlccDato(jlccContenido, "CARACTERÍSTICAS", jlccCaracteristicas(jlccContrato));
        jlccDato(jlccContenido, "SERVICIOS", jlccContrato.getInmueble().getServiciosIncluidos());
        jlccDato(
                jlccContenido,
                "TIPO",
                jlccContrato.getInmueble().getTipo() == null
                        ? "—"
                        : jlccContrato.getInmueble().getTipo().getNombre());
        PdfPCell jlccCaja = new PdfPCell(jlccContenido);
        jlccCaja.setPadding(8);
        jlccCaja.setBorderColor(JlccDocumentoPdf.JLCC_BORDE);
        jlccTabla.addCell(jlccCaja);
        return jlccTabla;
    }

    private PdfPTable jlccSeccionCondiciones(Contrato jlccContrato) {
        PdfPTable jlccTabla = new PdfPTable(1);
        jlccTabla.setWidthPercentage(100);
        jlccTabla.addCell(
                JlccDocumentoPdf.jlccCelda(
                        "CONDICIONES ECONÓMICAS",
                        JlccDocumentoPdf.jlccFuente(9, Font.BOLD, Color.WHITE),
                        JlccDocumentoPdf.JLCC_AZUL));
        PdfPTable jlccContenido = new PdfPTable(new float[] {22, 28, 22, 28});
        jlccContenido.setWidthPercentage(100);
        jlccDato(
                jlccContenido,
                "VIGENCIA",
                JlccDocumentoPdf.jlccFecha(jlccContrato.getFechaInicio())
                        + " al "
                        + JlccDocumentoPdf.jlccFecha(jlccContrato.getFechaFin()));
        jlccDato(jlccContenido, "DÍA DE PAGO", "Día " + jlccContrato.getDiaPago() + " de cada mes");
        jlccDato(jlccContenido, "RENTA MENSUAL", JlccDocumentoPdf.jlccSoles(jlccContrato.getRentaMensual()));
        jlccDato(jlccContenido, "INCREMENTO ANUAL", jlccContrato.getIncrementoAnualPct() + " %");
        jlccDato(
                jlccContenido,
                "GARANTÍA",
                JlccDocumentoPdf.jlccSoles(jlccContrato.getGarantiaMonto()) + " · " + jlccEstadoGarantia(jlccContrato));
        jlccDato(jlccContenido, "POLÍTICA DE MORA", jlccPoliticaMora(jlccContrato));
        jlccDato(jlccContenido, "DÍAS DE GRACIA", String.valueOf(jlccContrato.getDiasGracia()));
        jlccDato(jlccContenido, "EMITIDO", JlccDocumentoPdf.jlccFecha(LocalDate.now(jlccClock)));
        PdfPCell jlccCaja = new PdfPCell(jlccContenido);
        jlccCaja.setPadding(8);
        jlccCaja.setBorderColor(JlccDocumentoPdf.JLCC_BORDE);
        jlccTabla.addCell(jlccCaja);
        if (jlccContrato.getObservaciones() != null && !jlccContrato.getObservaciones().isBlank()) {
            PdfPCell jlccObservaciones =
                    JlccDocumentoPdf.jlccCelda(
                            "Observaciones: " + jlccContrato.getObservaciones(),
                            JlccDocumentoPdf.jlccFuente(8, Font.NORMAL, JlccDocumentoPdf.JLCC_GRIS),
                            JlccDocumentoPdf.JLCC_SUAVE);
            jlccObservaciones.setColspan(4);
            jlccTabla.addCell(jlccObservaciones);
        }
        return jlccTabla;
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

    private PdfPTable jlccTablaCronograma(Contrato jlccContrato) {
        PdfPTable jlccTabla = new PdfPTable(new float[] {6, 32, 16, 16, 16, 14});
        jlccTabla.setWidthPercentage(100);
        for (String jlccTitulo :
                new String[] {"#", "CONCEPTO", "PERIODO", "VENCIMIENTO", "MONTO", "ESTADO"}) {
            jlccTabla.addCell(
                    JlccDocumentoPdf.jlccCelda(
                            jlccTitulo,
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.BOLD, Color.WHITE),
                            JlccDocumentoPdf.JLCC_AZUL));
        }
        List<Cuota> jlccCuotas =
                jlccContrato.getCuotas().stream()
                        .sorted(
                                Comparator.comparing(Cuota::getFechaVencimiento)
                                        .thenComparing(Cuota::getNumero))
                        .toList();
        int jlccIndice = 0;
        for (Cuota jlccCuota : jlccCuotas) {
            Color jlccFondo = jlccIndice++ % 2 == 0 ? Color.WHITE : JlccDocumentoPdf.JLCC_SUAVE;
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
                            jlccCuota.getPeriodo() == null
                                    ? "—"
                                    : jlccCuota.getPeriodo()
                                            .format(
                                                    java.time.format.DateTimeFormatter.ofPattern(
                                                            "MM/yyyy")),
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
                            jlccCuota.getEstado().getEtiqueta(),
                            JlccDocumentoPdf.jlccFuente(7.5f, Font.NORMAL, Color.DARK_GRAY),
                            jlccFondo));
        }
        return jlccTabla;
    }

    private PdfPTable jlccNotaFinal() {
        PdfPTable jlccTabla = new PdfPTable(1);
        jlccTabla.setWidthPercentage(100);
        PdfPCell jlccNota =
                JlccDocumentoPdf.jlccCelda(
                        "Hoja resumen. No reemplaza el contrato firmado por las partes.",
                        JlccDocumentoPdf.jlccFuente(9, Font.BOLD, JlccDocumentoPdf.JLCC_GRIS),
                        JlccDocumentoPdf.JLCC_SUAVE);
        jlccNota.setHorizontalAlignment(Element.ALIGN_CENTER);
        jlccNota.setPadding(10);
        jlccTabla.addCell(jlccNota);
        return jlccTabla;
    }

    private String jlccCaracteristicas(Contrato jlccContrato) {
        StringBuilder jlccTexto = new StringBuilder();
        if (jlccContrato.getInmueble().getDormitorios() != null)
            jlccTexto.append(jlccContrato.getInmueble().getDormitorios()).append(" dorm. ");
        if (jlccContrato.getInmueble().getBanos() != null)
            jlccTexto.append(jlccContrato.getInmueble().getBanos()).append(" baños ");
        if (jlccContrato.getInmueble().getEstacionamientos() != null)
            jlccTexto.append(jlccContrato.getInmueble().getEstacionamientos()).append(" estac. ");
        if (jlccContrato.getInmueble().isAmoblado()) jlccTexto.append("amoblado");
        return jlccTexto.toString().trim();
    }

    private String jlccEstadoGarantia(Contrato jlccContrato) {
        if (jlccContrato.getGarantiaEstado() == null) return "—";
        return switch (jlccContrato.getGarantiaEstado()) {
            case SIN_GARANTIA -> "sin garantía";
            case PENDIENTE -> "pendiente de entrega";
            case RECIBIDA -> "recibida";
            case DEVUELTA -> "devuelta";
            case RETENIDA_PARCIAL -> "retenida parcialmente";
        };
    }

    private String jlccPoliticaMora(Contrato jlccContrato) {
        if (jlccContrato.getTipoMora() == null || jlccContrato.getValorMora() == null) return "—";
        return jlccContrato.getTipoMora() == TipoMora.PORCENTAJE_DIARIO
                ? jlccContrato.getValorMora() + " % diario"
                : JlccDocumentoPdf.jlccSoles(jlccContrato.getValorMora()) + " diario";
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
