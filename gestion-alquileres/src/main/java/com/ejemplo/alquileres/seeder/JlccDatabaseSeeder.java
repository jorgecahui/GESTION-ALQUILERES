package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.service.JlccSesionActual;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.seeder", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class JlccDatabaseSeeder implements CommandLineRunner {
    private final JlccTipoInmuebleSeeder jlccTipos;
    private final JlccPropietarioSeeder jlccPropietarios;
    private final JlccPersonaSeeder jlccPersonas;
    private final JlccInmuebleSeeder jlccInmuebles;
    private final JlccHistoriaDashboardSeeder jlccHistoria;
    private final JlccMantenimientoSeeder jlccMantenimientos;
    private final JlccSesionActual jlccSesion;

    @Override
    public void run(String... args) {
        jlccSesion.jlccIniciar(
                Usuario.builder()
                        .id(0L)
                        .username("sistema")
                        .nombre("Carga de ejemplo")
                        .rol(RolUsuario.ADMIN)
                        .activo(true)
                        .build());
        try {
            jlccTipos.jlccCargar();
            jlccPropietarios.jlccCargar();
            jlccPersonas.jlccCargar();
            jlccInmuebles.jlccCargar();
            jlccHistoria.jlccCargar();
            jlccMantenimientos.jlccCargar();
        } finally {
            jlccSesion.jlccCerrar();
        }
    }
}
