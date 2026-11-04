package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.repository.ConfiguracionRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.TipoInmuebleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PermisosTest {
    @Test void matrizDeRoles() {
        Permisos admin = DatosFase6.permisosAdmin();
        Permisos operador = DatosFase6.permisosOperador();
        Permisos consulta = DatosFase6.permisosConsulta();

        assertTrue(admin.esAdmin());
        assertTrue(admin.puedeGestionar());
        assertFalse(operador.esAdmin());
        assertTrue(operador.puedeGestionar());
        assertFalse(consulta.puedeGestionar());
        assertTrue(consulta.puedeConsultar());
    }

    @Test void operadorNoPuedeCambiarConfiguracion() {
        ConfiguracionService servicio = new ConfiguracionService(Mockito.mock(ConfiguracionRepository.class),
                new AppProperties(), DatosFase6.permisosOperador(), DatosFase6.auditoria());

        NegocioException error = assertThrows(NegocioException.class,
                () -> servicio.guardar(Configuracion.builder().id(1L).build()));
        assertTrue(error.getMessage().contains("permiso"));
    }

    @Test void consultaNoPuedeCrearCatalogos() {
        TipoInmuebleService servicio = new TipoInmuebleService(Mockito.mock(TipoInmuebleRepository.class),
                Mockito.mock(InmuebleRepository.class), DatosFase6.permisosConsulta());

        assertThrows(NegocioException.class, () -> servicio.guardar(com.ejemplo.alquileres.model.TipoInmueble.builder()
                .codigo("DEP").nombre("Departamento").build()));
    }

    @Test void adminSiPuedeOperar() {
        ConfiguracionRepository repo = Mockito.mock(ConfiguracionRepository.class);
        Mockito.when(repo.findById(Configuracion.FILA_UNICA)).thenReturn(java.util.Optional.empty());
        Mockito.when(repo.save(Mockito.any(Configuracion.class))).thenAnswer(i -> i.getArgument(0));
        AppProperties propiedades = DatosFase6.propiedades();
        ConfiguracionService servicio = new ConfiguracionService(repo, propiedades,
                DatosFase6.permisosAdmin(), DatosFase6.auditoria());
        Configuracion cambios = servicio.actual();

        assertDoesNotThrow(() -> servicio.guardar(cambios));
    }

    @Test void sinSesionNoHayPermisos() {
        SesionActual sesion = new SesionActual();
        Permisos permisos = new Permisos(sesion);
        assertFalse(permisos.esAdmin());
        assertFalse(permisos.puedeGestionar());
        assertThrows(NegocioException.class, permisos::exigirGestion);
        assertThrows(NegocioException.class, permisos::exigirAdmin);
    }
}
