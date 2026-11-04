package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.service.SesionActual;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/** Punto único de carga demostrativa; fuera de dev nunca se registra como bean. */
@Component @Profile("dev") @ConditionalOnProperty(prefix="app.seeder", name="enabled", havingValue="true") @RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {
 private final TipoInmuebleSeeder tipos; private final PropietarioSeeder propietarios; private final PersonaSeeder personas; private final InmuebleSeeder inmuebles; private final HistoriaDashboardSeeder historia; private final MantenimientoSeeder mantenimientos; private final SesionActual sesion;

 @Override public void run(String... args) {
   // Los seeders usan los mismos servicios que la UI y, por tanto, pasan por Permisos.
   // Se autentica un usuario técnico solo durante la carga del perfil dev.
   sesion.iniciar(Usuario.builder().id(0L).username("sistema").nombre("Carga de ejemplo")
           .rol(RolUsuario.ADMIN).activo(true).build());
   try {
     tipos.cargar(); propietarios.cargar(); personas.cargar(); inmuebles.cargar();
     historia.cargar(); mantenimientos.cargar();
   } finally {
     sesion.cerrar();
   }
 }
}
