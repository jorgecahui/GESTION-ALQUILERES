package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.TipoInmueble;
import com.ejemplo.alquileres.repository.TipoInmuebleRepository;
import com.ejemplo.alquileres.service.JlccTipoInmuebleService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JlccTipoInmuebleSeeder {
    private final TipoInmuebleRepository jlccRepo;
    private final JlccTipoInmuebleService jlccService;

    public void jlccCargar() {
        String[][] jlccDatos = {
            {"TI-DEP", "Departamento"},
            {"TI-CAS", "Casa"},
            {"TI-LOC", "Local comercial"},
            {"TI-OFI", "Oficina"},
            {"TI-COC", "Cochera"},
            {"TI-HAB", "Habitación"},
            {"TI-TER", "Terreno"}
        };
        for (String[] d : jlccDatos)
            if (!jlccRepo.existsByCodigoIgnoreCase(d[0]))
                jlccService.jlccGuardar(
                        TipoInmueble.builder()
                                .codigo(d[0])
                                .nombre(d[1])
                                .descripcion("Tipo de inmueble")
                                .activo(true)
                                .build());
    }
}
