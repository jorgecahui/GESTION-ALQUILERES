package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.service.*;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class FichaInmuebleController {
    private final ContratoRepository contratos;
    private final MantenimientoService mantenimientos;
    private final RentabilidadService rentabilidad;

    public FichaInmuebleController(
            ContratoRepository c, MantenimientoService m, RentabilidadService r) {
        contratos = c;
        mantenimientos = m;
        rentabilidad = r;
    }

    public record Ficha(
            Inmueble inmueble,
            List<Contrato> contratos,
            List<Mantenimiento> mantenimientos,
            RentabilidadService.Resumen finanzas) {}

    @Transactional(readOnly = true)
    public Ficha cargar(Inmueble inmueble) {
        return new Ficha(
                inmueble,
                contratos.listarPorInmueble(inmueble.getId()),
                mantenimientos.porInmueble(inmueble.getId()),
                rentabilidad.anoActual(inmueble.getId()));
    }
}
