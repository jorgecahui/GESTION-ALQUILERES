package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.PropietarioRepository;
import com.ejemplo.alquileres.service.JlccPropietarioService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JlccPropietarioSeeder {
    private final PropietarioRepository jlccRepo;
    private final JlccPropietarioService jlccService;

    public void jlccCargar() {
        jlccCrear(TipoDocumento.DNI, "45671234", "María Elena Salazar", "999 111 222");
        jlccCrear(TipoDocumento.RUC, "20123456789", "Inversiones Costa Sur S.A.C.", "+51 988 333 444");
    }

    private void jlccCrear(TipoDocumento t, String d, String n, String jlccTel) {
        if (!jlccRepo.existsByTipoDocumentoAndNumeroDocumento(t, d))
            jlccService.jlccGuardar(
                    Propietario.builder()
                            .tipoDocumento(t)
                            .numeroDocumento(d)
                            .nombre(n)
                            .telefono(jlccTel)
                            .email("contacto@ejemplo.pe")
                            .banco("BCP")
                            .cuenta("CCI ficticia")
                            .activo(true)
                            .build());
    }
}
