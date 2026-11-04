package com.ejemplo.alquileres.config;

import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.service.ConfiguracionService;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Datos del arrendador impresos en documentos internos, tomados de la configuración. */
@Component
public class DatosArrendador {
    private final ConfiguracionService configuracion;
    private final Configuracion fija;

    @Autowired
    public DatosArrendador(ConfiguracionService configuracion) {
        this.configuracion = configuracion;
        this.fija = null;
    }

    /** Constructor para pruebas con una configuración ya resuelta. */
    public DatosArrendador(Configuracion fija) {
        this.configuracion = null;
        this.fija = fija;
    }

    public String razonSocial() { return actual().getArrendadorRazonSocial(); }
    public String tipoDocumento() { return actual().getArrendadorTipoDocumento(); }
    public String documento() { return actual().getArrendadorDocumento(); }
    public String documentoConTipo() {
        String tipo = tipoDocumento();
        String numero = documento();
        return tipo == null || tipo.isBlank() ? numero : tipo + " " + numero;
    }
    public String direccion() { return actual().getArrendadorDireccion(); }
    public String telefono() { return actual().getArrendadorTelefono(); }
    public String email() { return actual().getArrendadorEmail(); }

    public Path logo() {
        if (configuracion != null) return configuracion.rutaLogo();
        return null;
    }

    public String leyendaRecibo() {
        if (configuracion != null) return configuracion.leyendaRecibo();
        String leyenda = fija == null ? null : fija.getReciboLeyenda();
        return leyenda == null || leyenda.isBlank() ? "Documento interno sin validez tributaria" : leyenda;
    }

    private Configuracion actual() {
        Configuracion valor = configuracion == null ? fija : configuracion.actual();
        return valor == null ? new Configuracion() : valor;
    }
}
