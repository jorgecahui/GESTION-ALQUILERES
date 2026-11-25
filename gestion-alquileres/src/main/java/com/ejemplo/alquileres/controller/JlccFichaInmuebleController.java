package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.service.*;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class JlccFichaInmuebleController {
    private final ContratoRepository jlccContratos;
    private final JlccMantenimientoService jlccMantenimientos;
    private final JlccRentabilidadService jlccRentabilidad;

    public JlccFichaInmuebleController(
            ContratoRepository c, JlccMantenimientoService m, JlccRentabilidadService r) {
        jlccContratos = c;
        jlccMantenimientos = m;
        jlccRentabilidad = r;
    }

    public record Ficha(
            Inmueble inmueble,
            List<Contrato> contratos,
            List<Mantenimiento> mantenimientos,
            JlccRentabilidadService.Resumen finanzas) {}

    @Transactional(readOnly = true)
    public Ficha jlccCargar(Inmueble jlccInmueble) {
        return new Ficha(
                jlccInmueble,
                jlccContratos.listarPorInmueble(jlccInmueble.getId()),
                jlccMantenimientos.jlccPorInmueble(jlccInmueble.getId()),
                jlccRentabilidad.jlccAnoActual(jlccInmueble.getId()));
    }
}
