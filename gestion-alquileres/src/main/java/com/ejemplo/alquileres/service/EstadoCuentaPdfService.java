package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.exception.NegocioException;
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
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Estado de cuenta por contrato con cuotas, pagos y resumen de saldos. */
@Service
@RequiredArgsConstructor
public class EstadoCuentaPdfService {
    private final ContratoRepository contratos;
    private final PagoRepository pagos;
    private final CalculadoraMora mora;
    private final DatosArrendador arrendador;
    private final Clock clock;

    @Transactional(readOnly = true)
    public byte[] generar(Long contratoId) {
        Contrato contrato = contratos.detalle(contratoId)
                .orElseThrow(() -> new NegocioException("El contrato no existe."));
        return escribir(contrato, pagos.porContrato(contratoId));
    }

    /** Expuesto para pruebas y para reutilizar datos ya cargados. */
    public byte[] escribir(Contrato contrato, List<Pago> listaPagos) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = DocumentoPdf.documento();
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            DocumentoPdf.agregarPie(documento, writer, "Estado de cuenta interno · no es un comprobante electrónico SUNAT");
            documento.open();
            documento.add(cabecera(contrato));
            documento.add(DocumentoPdf.espacio(12));
            documento.add(datos(contrato));
            documento.add(DocumentoPdf.espacio(14));
            documento.add(titulo("CUOTAS DEL CONTRATO"));
            documento.add(tablaCuotas(contrato));
            documento.add(DocumentoPdf.espacio(14));
            documento.add(titulo("PAGOS REGISTRADOS"));
            documento.add(tablaPagos(listaPagos));
            documento.add(DocumentoPdf.espacio(14));
            documento.add(resumen(contrato));
            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el estado de cuenta PDF.", e);
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
        caja.addCell(DocumentoPdf.celda("ESTADO DE CUENTA", DocumentoPdf.fuente(10, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL));
        caja.addCell(DocumentoPdf.celda(contrato.getCodigo(), DocumentoPdf.fuente(15, Font.BOLD, DocumentoPdf.AZUL), Color.WHITE));
        PdfPCell marco = new PdfPCell(caja);
        marco.setPadding(0);
        marco.setBorderColor(DocumentoPdf.AZUL);
        marco.setBorderWidth(1.2f);
        tabla.addCell(marco);
        return tabla;
    }

    private PdfPTable datos(Contrato contrato) {
        PdfPTable tabla = new PdfPTable(new float[]{18, 32, 18, 32});
        tabla.setWidthPercentage(100);
        dato(tabla, "ARRENDATARIO", contrato.getArrendatario().getNombreCompleto());
        dato(tabla, "DOCUMENTO", contrato.getArrendatario().getTipoDocumento() + " " + contrato.getArrendatario().getNumeroDocumento());
        dato(tabla, "INMUEBLE", contrato.getInmueble().getCodigo() + " · " + contrato.getInmueble().getNombre());
        dato(tabla, "DIRECCIÓN", contrato.getInmueble().getDireccion());
        dato(tabla, "VIGENCIA", DocumentoPdf.fecha(contrato.getFechaInicio()) + " al " + DocumentoPdf.fecha(contrato.getFechaFin()));
        dato(tabla, "RENTA MENSUAL", DocumentoPdf.soles(contrato.getRentaMensual()));
        dato(tabla, "DÍA DE PAGO", String.valueOf(contrato.getDiaPago()));
        dato(tabla, "EMITIDO", DocumentoPdf.fecha(LocalDate.now(clock)));
        PdfPCell caja = new PdfPCell(tabla);
        caja.setBorderColor(DocumentoPdf.BORDE);
        caja.setBackgroundColor(DocumentoPdf.SUAVE);
        caja.setPadding(8);
        PdfPTable marco = new PdfPTable(1);
        marco.setWidthPercentage(100);
        marco.addCell(caja);
        return marco;
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

    private PdfPTable tablaCuotas(Contrato contrato) {
        PdfPTable tabla = new PdfPTable(new float[]{6, 27, 13, 13, 12, 12, 12, 15});
        tabla.setWidthPercentage(100);
        String[] titulos = {"#", "CONCEPTO", "VENCIMIENTO", "MONTO", "PAGADO", "MORA", "SALDO", "ESTADO"};
        for (String titulo : titulos) {
            tabla.addCell(DocumentoPdf.celda(titulo, DocumentoPdf.fuente(7.5f, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL));
        }
        LocalDate hoy = LocalDate.now(clock);
        List<Cuota> cuotas = contrato.getCuotas().stream()
                .sorted(Comparator.comparing(Cuota::getFechaVencimiento).thenComparing(Cuota::getNumero)).toList();
        int indice = 0;
        for (Cuota cuota : cuotas) {
            Color fondo = indice++ % 2 == 0 ? Color.WHITE : DocumentoPdf.SUAVE;
            BigDecimal saldo = mora.saldoCapital(cuota);
            BigDecimal moraPendiente = cuota.getEstado() == EstadoCuota.ANULADA ? BigDecimal.ZERO : mora.pendiente(cuota, contrato, hoy);
            tabla.addCell(DocumentoPdf.celda(String.valueOf(cuota.getNumero()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(cuota.getConcepto(), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.fecha(cuota.getFechaVencimiento()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.soles(cuota.getMontoCuota()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.soles(cuota.getMontoPagado()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.soles(moraPendiente), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.soles(saldo), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(cuota.getEstado().name(), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
        }
        return tabla;
    }

    private PdfPTable tablaPagos(List<Pago> listaPagos) {
        PdfPTable tabla = new PdfPTable(new float[]{20, 16, 20, 18, 26});
        tabla.setWidthPercentage(100);
        String[] titulos = {"RECIBO", "FECHA", "MEDIO", "MONTO", "ESTADO"};
        for (String titulo : titulos) {
            tabla.addCell(DocumentoPdf.celda(titulo, DocumentoPdf.fuente(7.5f, Font.BOLD, Color.WHITE), DocumentoPdf.AZUL));
        }
        if (listaPagos == null || listaPagos.isEmpty()) {
            PdfPCell vacio = DocumentoPdf.celda("Sin pagos registrados para este contrato.", DocumentoPdf.fuente(8, Font.NORMAL, DocumentoPdf.GRIS), Color.WHITE);
            vacio.setColspan(5);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(vacio);
            return tabla;
        }
        int indice = 0;
        for (Pago pago : listaPagos) {
            Color fondo = indice++ % 2 == 0 ? Color.WHITE : DocumentoPdf.SUAVE;
            tabla.addCell(DocumentoPdf.celda(pago.getRecibo(), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.fecha(pago.getFechaPago()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(pago.getMedioPago().name().replace('_', ' '), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(DocumentoPdf.soles(pago.getMontoTotal()), DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
            tabla.addCell(DocumentoPdf.celda(pago.getEstado() == EstadoPago.ANULADO ? "ANULADO" : "EMITIDO", DocumentoPdf.fuente(7.5f, Font.NORMAL, Color.DARK_GRAY), fondo));
        }
        return tabla;
    }

    private PdfPTable resumen(Contrato contrato) {
        LocalDate hoy = LocalDate.now(clock);
        BigDecimal facturado = BigDecimal.ZERO, pagado = BigDecimal.ZERO, moraCobrada = BigDecimal.ZERO,
                saldo = BigDecimal.ZERO, moraEstimada = BigDecimal.ZERO;
        for (Cuota cuota : contrato.getCuotas()) {
            if (cuota.getEstado() == EstadoCuota.ANULADA) continue;
            facturado = facturado.add(cuota.getMontoCuota());
            pagado = pagado.add(cuota.getMontoPagado());
            moraCobrada = moraCobrada.add(cuota.getMoraCobrada());
            saldo = saldo.add(mora.saldoCapital(cuota));
            moraEstimada = moraEstimada.add(mora.pendiente(cuota, contrato, hoy));
        }
        PdfPTable tabla = new PdfPTable(new float[]{55, 45});
        tabla.setWidthPercentage(100);
        PdfPCell nota = DocumentoPdf.celdaSinBorde();
        nota.addElement(new Paragraph("Resumen del contrato", DocumentoPdf.fuente(9, Font.BOLD, DocumentoPdf.AZUL)));
        nota.addElement(new Paragraph("La mora estimada se calcula a la fecha de emisión y puede variar hasta el pago.", DocumentoPdf.fuente(8, Font.NORMAL, DocumentoPdf.GRIS)));
        nota.addElement(new Paragraph("Documento interno para control administrativo.", DocumentoPdf.fuente(8, Font.NORMAL, DocumentoPdf.GRIS)));
        tabla.addCell(nota);
        PdfPTable valores = new PdfPTable(new float[]{58, 42});
        valores.setWidthPercentage(100);
        filaResumen(valores, "Total facturado", facturado, false);
        filaResumen(valores, "Total pagado", pagado, false);
        filaResumen(valores, "Mora cobrada", moraCobrada, false);
        filaResumen(valores, "Saldo pendiente", saldo, true);
        filaResumen(valores, "Mora estimada a hoy", moraEstimada, false);
        tabla.addCell(new PdfPCell(valores));
        return tabla;
    }

    private void filaResumen(PdfPTable tabla, String etiqueta, BigDecimal valor, boolean destacado) {
        Color fondo = destacado ? DocumentoPdf.AZUL : Color.WHITE;
        Font fuente = DocumentoPdf.fuente(9, destacado ? Font.BOLD : Font.NORMAL, destacado ? Color.WHITE : Color.DARK_GRAY);
        tabla.addCell(DocumentoPdf.celda(etiqueta, fuente, fondo));
        tabla.addCell(DocumentoPdf.celda(DocumentoPdf.soles(valor), fuente, fondo));
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
