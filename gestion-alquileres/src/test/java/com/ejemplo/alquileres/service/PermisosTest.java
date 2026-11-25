package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.repository.ConfiguracionRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.TipoInmuebleRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PermisosTest {
    @Test
    void matrizDeRoles() {
        JlccPermisos admin = DatosFase6.permisosAdmin();
        JlccPermisos operador = DatosFase6.permisosOperador();
        JlccPermisos consulta = DatosFase6.permisosConsulta();

        assertTrue(admin.jlccEsAdmin());
        assertTrue(admin.jlccPuedeGestionar());
        assertFalse(operador.jlccEsAdmin());
        assertTrue(operador.jlccPuedeGestionar());
        assertFalse(consulta.jlccPuedeGestionar());
        assertTrue(consulta.jlccPuedeConsultar());
    }

    @Test
    void operadorNoPuedeCambiarConfiguracion() {
        JlccConfiguracionService servicio =
                new JlccConfiguracionService(
                        Mockito.mock(ConfiguracionRepository.class),
                        new AppProperties(),
                        DatosFase6.permisosOperador(),
                        DatosFase6.auditoria());

        JlccNegocioException error =
                assertThrows(
                        JlccNegocioException.class,
                        () -> servicio.jlccGuardar(Configuracion.builder().id(1L).build()));
        assertTrue(error.getMessage().contains("permiso"));
    }

    @Test
    void consultaNoPuedeCrearCatalogos() {
        JlccTipoInmuebleService servicio =
                new JlccTipoInmuebleService(
                        Mockito.mock(TipoInmuebleRepository.class),
                        Mockito.mock(InmuebleRepository.class),
                        DatosFase6.permisosConsulta(),
                        DatosFase6.auditoria());

        assertThrows(
                JlccNegocioException.class,
                () ->
                        servicio.jlccGuardar(
                                com.ejemplo.alquileres.model.TipoInmueble.builder()
                                        .codigo("DEP")
                                        .nombre("Departamento")
                                        .build()));
    }

    @Test
    void adminSiPuedeOperar() {
        ConfiguracionRepository repo = Mockito.mock(ConfiguracionRepository.class);
        Mockito.when(repo.findById(Configuracion.FILA_UNICA))
                .thenReturn(java.util.Optional.empty());
        Mockito.when(repo.save(Mockito.any(Configuracion.class))).thenAnswer(i -> i.getArgument(0));
        AppProperties propiedades = DatosFase6.propiedades();
        JlccConfiguracionService servicio =
                new JlccConfiguracionService(
                        repo, propiedades, DatosFase6.permisosAdmin(), DatosFase6.auditoria());
        Configuracion cambios = servicio.jlccActual();

        assertDoesNotThrow(() -> servicio.jlccGuardar(cambios));
    }

    @Test
    void sinSesionNoHayPermisos() {
        JlccSesionActual sesion = new JlccSesionActual();
        JlccPermisos permisos = new JlccPermisos(sesion);
        assertFalse(permisos.jlccEsAdmin());
        assertFalse(permisos.jlccPuedeGestionar());
        assertThrows(JlccNegocioException.class, permisos::jlccExigirGestion);
        assertThrows(JlccNegocioException.class, permisos::jlccExigirAdmin);
    }
}
