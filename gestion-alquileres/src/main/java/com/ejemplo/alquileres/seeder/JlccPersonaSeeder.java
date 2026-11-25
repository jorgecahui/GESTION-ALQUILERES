package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.PersonaRepository;
import com.ejemplo.alquileres.service.JlccPersonaService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JlccPersonaSeeder {
    private final PersonaRepository jlccRepo;
    private final JlccPersonaService jlccService;

    public void jlccCargar() {
        String[][] d = {
            {"Ana", "García", "73456781"},
            {"Luis", "Paredes", "73456782"},
            {"Rosa", "Vega", "73456783"},
            {"Carlos", "Luna", "73456784"},
            {"Elena", "Ríos", "73456785"},
            {"Marco", "Torres", "73456786"},
            {"Julia", "Campos", "73456787"},
            {"Diego", "Mendoza", "73456788"},
            {"Paola", "Navarro", "73456789"},
            {"Hugo", "Soto", "73456790"},
            {"Carmen", "Flores", "73456791"},
            {"Renzo", "Silva", "73456792"}
        };
        for (int i = 0; i < d.length; i++)
            if (!jlccRepo.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, d[i][2]))
                jlccService.jlccGuardar(
                        Persona.builder()
                                .tipoDocumento(TipoDocumento.DNI)
                                .numeroDocumento(d[i][2])
                                .nombres(d[i][0])
                                .apellidos(d[i][1])
                                .telefono("999000" + String.format("%03d", i))
                                .email("persona" + (i + 1) + "@ejemplo.pe")
                                .activo(true)
                                .build());
    }
}
