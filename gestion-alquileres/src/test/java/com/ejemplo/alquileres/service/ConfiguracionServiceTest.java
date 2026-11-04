package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.Persona;
import com.ejemplo.alquileres.model.TipoDocumento;
import com.ejemplo.alquileres.model.TipoMora;
import com.ejemplo.alquileres.repository.ConfiguracionRepository;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.CuotaRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.PersonaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfiguracionServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

    @Test void construyeValoresPorDefectoDesdeLasPropiedades() {
        ConfiguracionService servicio = servicio(propiedades());

        Configuracion c = servicio.actual();

        assertEquals("ARRENDADOR DE PRUEBA", c.getArrendadorRazonSocial());
        assertEquals("R001", c.getReciboSerie());
        assertEquals(TipoMora.PORCENTAJE_DIARIO, c.getMoraTipo());
        assertEquals(new BigDecimal("0.1000"), c.getMoraValor());
        assertEquals(3, c.getMoraDiasGracia());
        assertEquals(60, c.getAlertasDiasVencimiento());
        assertEquals(15, c.getSemaforoUmbralLeveDias());
        assertEquals(2, servicio.cuotasRojo());
        assertNotNull(c.getPlantillaPreventivo());
    }

    @Test void guardaEInvalidaLaCacheSinVolverALaBase() {
        ConfiguracionRepository repo = repositorio();
        ConfiguracionService servicio = new ConfiguracionService(repo, propiedades(), DatosFase6.permisosAdmin(), DatosFase6.auditoria());
        Configuracion cambios = servicio.actual();
        cambios.setArrendadorRazonSocial("NUEVA INMOBILIARIA S.A.C.");
        cambios.setReciboSerie("r002");
        cambios.setMoraValor(new BigDecimal("0.25"));

        Configuracion guardada = servicio.guardar(cambios);

        assertEquals("NUEVA INMOBILIARIA S.A.C.", guardada.getArrendadorRazonSocial());
        assertEquals("R002", guardada.getReciboSerie());
        assertEquals(new BigDecimal("0.2500"), guardada.getMoraValor());
        assertEquals("NUEVA INMOBILIARIA S.A.C.", servicio.actual().getArrendadorRazonSocial());
        verify(repo, times(1)).findById(Configuracion.FILA_UNICA);
    }

    @Test void rechazaSerieInvalidaYNoGuarda() {
        ConfiguracionRepository repo = repositorio();
        ConfiguracionService servicio = new ConfiguracionService(repo, propiedades(), DatosFase6.permisosAdmin(), DatosFase6.auditoria());
        Configuracion cambios = servicio.actual();
        cambios.setReciboSerie("R 001!");

        assertThrows(NegocioException.class, () -> servicio.guardar(cambios));
    }

    @Test void aplicaLaMoraPorDefectoAUnContratoNuevo() {
        AppProperties propiedades = propiedades();
        propiedades.getMora().setTipo("MONTO_DIARIO");
        propiedades.getMora().setValor(new BigDecimal("2.50"));
        propiedades.getMora().setDiasGracia(5);
        ConfiguracionService configuracion = servicio(propiedades);

        ContratoRepository contratos = mock(ContratoRepository.class);
        InmuebleRepository inmuebles = mock(InmuebleRepository.class);
        PersonaRepository personas = mock(PersonaRepository.class);
        SecuenciaService secuencias = mock(SecuenciaService.class);
        when(contratos.existeTraslape(any(), any(), any(), any())).thenReturn(false);
        when(contratos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(inmuebles.findById(1L)).thenReturn(Optional.of(Inmueble.builder().id(1L).codigo("INM-0001")
                .nombre("Departamento").estado(EstadoInmueble.DISPONIBLE).build()));
        when(personas.findById(2L)).thenReturn(Optional.of(Persona.builder().id(2L).nombres("Ana")
                .tipoDocumento(TipoDocumento.DNI).numeroDocumento("12345678").activo(true).build()));
        when(secuencias.siguiente("CONTRATO")).thenReturn(1L);
        when(secuencias.formatear("CTR-", 1L, 6)).thenReturn("CTR-000001");
        ContratoService contratosService = new ContratoService(contratos, mock(CuotaRepository.class),
                inmuebles, personas, secuencias, new CronogramaGenerador(), configuracion,
                DatosFase6.permisosAdmin(), DatosFase6.auditoria(), CLOCK);

        var contrato = contratosService.crear(new ContratoService.Solicitud(1L, 2L, null,
                LocalDate.of(2026, 3, 1), 6, 5, new BigDecimal("1200.00"), null, BigDecimal.ZERO,
                false, null, null, null, null));

        assertEquals(TipoMora.MONTO_DIARIO, contrato.getTipoMora());
        assertEquals(new BigDecimal("2.5000"), contrato.getValorMora());
        assertEquals(5, contrato.getDiasGracia());
    }

    private ConfiguracionService servicio(AppProperties propiedades) {
        return new ConfiguracionService(repositorio(), propiedades, DatosFase6.permisosAdmin(), DatosFase6.auditoria());
    }

    private ConfiguracionRepository repositorio() {
        ConfiguracionRepository repo = mock(ConfiguracionRepository.class);
        when(repo.findById(Configuracion.FILA_UNICA)).thenReturn(Optional.empty());
        when(repo.save(any(Configuracion.class))).thenAnswer(i -> i.getArgument(0));
        return repo;
    }

    private AppProperties propiedades() {
        AppProperties p = new AppProperties();
        p.getArrendador().setRazonSocial("ARRENDADOR DE PRUEBA");
        p.getArrendador().setTipoDocumento("RUC");
        p.getArrendador().setDocumento("20600000001");
        p.getArrendador().setDireccion("Lima");
        p.getArrendador().setTelefono("999999999");
        p.getArrendador().setEmail("pruebas@ejemplo.pe");
        p.getRecibos().setSerie("R001");
        p.getRecibos().setLeyenda("Documento interno");
        p.getMora().setTipo("PORCENTAJE_DIARIO");
        p.getMora().setValor(new BigDecimal("0.10"));
        p.getMora().setDiasGracia(3);
        p.getAlertas().setDiasVencimiento(60);
        p.getAlertas().setRecordatorioDias(3);
        p.getMorosidad().setUmbralLeveDias(15);
        p.getMorosidad().setCuotasRojo(2);
        return p;
    }
}
