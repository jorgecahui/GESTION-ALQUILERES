package com.ejemplo.alquileres.config;

import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.service.JlccConfiguracionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class DatosArrendador {
    private final JlccConfiguracionService configuracion;
    private final Configuracion fija;

    @Autowired
    public DatosArrendador(JlccConfiguracionService configuracion) {
        this.configuracion = configuracion;
        this.fija = null;
    }

    public DatosArrendador(Configuracion fija) {
        this.configuracion = null;
        this.fija = fija;
    }

    public String razonSocial() {
        return actual().getArrendadorRazonSocial();
    }

    public String tipoDocumento() {
        return actual().getArrendadorTipoDocumento();
    }

    public String documento() {
        return actual().getArrendadorDocumento();
    }

    public String documentoConTipo() {
        String tipo = tipoDocumento();
        String numero = documento();
        return tipo == null || tipo.isBlank() ? numero : tipo + " " + numero;
    }

    public String direccion() {
        return actual().getArrendadorDireccion();
    }

    public String telefono() {
        return actual().getArrendadorTelefono();
    }

    public String email() {
        return actual().getArrendadorEmail();
    }

    public Path logo() {
        if (configuracion != null) return configuracion.jlccRutaLogo();
        return null;
    }

    public String leyendaRecibo() {
        if (configuracion != null) return configuracion.jlccLeyendaRecibo();
        String leyenda = fija == null ? null : fija.getReciboLeyenda();
        return leyenda == null || leyenda.isBlank()
                ? "Documento interno sin validez tributaria"
                : leyenda;
    }

    private Configuracion actual() {
        Configuracion valor = configuracion == null ? fija : configuracion.jlccActual();
        return valor == null ? new Configuracion() : valor;
    }
}
