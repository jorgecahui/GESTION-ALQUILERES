package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoContrato;
import com.ejemplo.alquileres.model.EstadoCuota;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.Persona;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.model.TipoCuota;
import com.ejemplo.alquileres.model.TipoDocumento;
import com.ejemplo.alquileres.model.TipoInmueble;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.ui.BloqueMaqueta;
import com.ejemplo.alquileres.ui.BloqueMaqueta.CategoriaMora;
import com.ejemplo.alquileres.ui.BloqueMaqueta.CategoriaVencimiento;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class MaquetaServiceTest {
    private final InmuebleRepository inmuebles = mock(InmuebleRepository.class);
    private final ContratoRepository contratos = mock(ContratoRepository.class);
    private final MorosidadService morosidad =
            new MorosidadService(DatosFase6.servicioConfiguracion(), DatosFase6.CLOCK);
    private final MaquetaService service =
            new MaquetaService(
                    inmuebles, contratos, morosidad, new CalculadoraMora(), DatosFase6.CLOCK);

    @Test
    void armaBloquesConSemaforosEInconsistencias() {
        TipoInmueble local =
                TipoInmueble.builder().id(1L).codigo("TI-LOC").nombre("Local comercial").build();
        TipoInmueble depa =
                TipoInmueble.builder().id(2L).codigo("TI-DEP").nombre("Departamento").build();

        Inmueble i1 = inmueble(1L, "INM-0001", "Local 101", local, EstadoInmueble.ALQUILADO, 1, 1);
        Inmueble i2 = inmueble(2L, "INM-0002", "Local 102", local, EstadoInmueble.ALQUILADO, 1, 2);
        Inmueble i3 = inmueble(3L, "INM-0003", "Depa 201", depa, EstadoInmueble.ALQUILADO, 2, 1);
        Inmueble i4 = inmueble(4L, "INM-0004", "Depa 202", depa, EstadoInmueble.DISPONIBLE, 2, 2);
        Inmueble i5 = inmueble(5L, "INM-0005", "Depa 301", depa, EstadoInmueble.ALQUILADO, 3, 1);
        Inmueble i6 =
                inmueble(6L, "INM-0006", "Depa 302", depa, EstadoInmueble.MANTENIMIENTO, 3, 2);
        Inmueble i7 =
                inmueble(7L, "INM-0007", "Sin nivel", depa, EstadoInmueble.ALQUILADO, null, 1);
        Inmueble i8 =
                inmueble(8L, "INM-0008", "Sin orden", depa, EstadoInmueble.ALQUILADO, 4, null);
        Inmueble i9 = inmueble(9L, "INM-0009", "Local 901", local, EstadoInmueble.INACTIVO, 4, 1);

        Contrato c1 = contrato(101L, i1, "Ana Pérez", "12345678", LocalDate.of(2026, 3, 5));
        Contrato c2 = contrato(102L, i2, "Luis Paredes", "22345678", LocalDate.of(2026, 2, 5));
        Contrato c3 = contrato(103L, i3, "Rosa Vega", "32345678", LocalDate.of(2025, 12, 20));

        when(inmuebles.listarParaMaqueta()).thenReturn(List.of(i1, i2, i3, i4, i5, i6, i7, i8, i9));
        when(contratos.vigentesConDetalle()).thenReturn(List.of(c1, c2, c3));

        MaquetaService.ResultadoMaqueta resultado = service.cargar();

        assertEquals(7, resultado.bloques().size());
        assertEquals(2, resultado.sinUbicacion());
        assertEquals(7, service.listarBloques().size());

        BloqueMaqueta b1 = bloque(resultado, 1L);
        assertEquals("INM-0001", b1.codigo());
        assertEquals("Local 101", b1.nombre());
        assertEquals("Local comercial", b1.tipo());
        assertEquals(1, b1.nivel());
        assertEquals(1, b1.orden());
        assertEquals(EstadoInmueble.ALQUILADO, b1.estado());
        assertEquals(101L, b1.contratoId());
        assertEquals("Ana Pérez", b1.arrendatario());
        assertEquals(SemaforoMorosidad.AL_DIA, b1.semaforo());
        assertFalse(b1.inconsistencia());
        assertEquals(new BigDecimal("55.00"), b1.areaM2());
        assertEquals(2, b1.dormitorios());
        assertEquals(1, b1.banos());
        assertFalse(b1.amoblado());
        assertEquals(new BigDecimal("1200.00"), b1.rentaReferencial());
        assertEquals(LocalDate.of(2027, 1, 31), b1.fechaFinContrato());
        assertEquals(355L, b1.diasParaVencer());
        assertEquals(CategoriaMora.AL_DIA, b1.categoriaMora());
        assertEquals(CategoriaVencimiento.MAS_DE_90_DIAS, b1.categoriaVencimiento());

        assertEquals(SemaforoMorosidad.ATRASO_LEVE, bloque(resultado, 2L).semaforo());
        assertEquals(SemaforoMorosidad.MOROSO, bloque(resultado, 3L).semaforo());

        BloqueMaqueta b4 = bloque(resultado, 4L);
        assertEquals(EstadoInmueble.DISPONIBLE, b4.estado());
        assertNull(b4.semaforo());
        assertNull(b4.contratoId());
        assertFalse(b4.inconsistencia());
        assertEquals(new BigDecimal("1200.00"), b4.rentaReferencial());
        assertNull(b4.fechaFinContrato());
        assertNull(b4.diasParaVencer());
        assertEquals(CategoriaMora.SIN_MORA, b4.categoriaMora());
        assertEquals(CategoriaVencimiento.SIN_CONTRATO, b4.categoriaVencimiento());

        BloqueMaqueta b5 = bloque(resultado, 5L);
        assertEquals(EstadoInmueble.ALQUILADO, b5.estado());
        assertNull(b5.semaforo());
        assertTrue(b5.inconsistencia());
        assertNull(b5.fechaFinContrato());
        assertNull(b5.diasParaVencer());
        assertEquals(CategoriaMora.SIN_MORA, b5.categoriaMora());
        assertEquals(CategoriaVencimiento.SIN_CONTRATO, b5.categoriaVencimiento());

        assertNull(bloque(resultado, 6L).semaforo());
        assertNull(bloque(resultado, 9L).semaforo());
        assertFalse(resultado.bloques().stream().anyMatch(b -> b.id() == 7L || b.id() == 8L));

        for (RecordComponent componente : BloqueMaqueta.class.getRecordComponents()) {
            assertFalse(Inmueble.class.isAssignableFrom(componente.getType()));
            assertFalse(Contrato.class.isAssignableFrom(componente.getType()));
        }
    }

    @Test
    void detalleDeDisponibleSinContrato() {
        Inmueble inmueble =
                inmuebleDetalle(1L, "INM-0001", "Depa 101", EstadoInmueble.DISPONIBLE, 1, 1);
        prepararDetalle(inmueble);

        MaquetaService.DetalleMaqueta detalle = service.detalle(1L);

        assertEquals("INM-0001", detalle.codigo());
        assertEquals("Depa 101", detalle.nombre());
        assertEquals("Departamento", detalle.tipo());
        assertEquals(1, detalle.nivel());
        assertEquals(1, detalle.orden());
        assertEquals("Av. Los Andes 1245", detalle.direccion());
        assertEquals("Juliaca", detalle.distrito());
        assertEquals(new BigDecimal("80.00"), detalle.areaM2());
        assertEquals(2, detalle.dormitorios());
        assertEquals(1, detalle.banos());
        assertEquals(new BigDecimal("1200.00"), detalle.rentaReferencial());
        assertEquals(EstadoInmueble.DISPONIBLE, detalle.estado());
        assertNull(detalle.contrato());
        assertFalse(detalle.inconsistencia());
    }

    @Test
    void detalleDeAlquiladoAlDia() {
        Inmueble inmueble =
                inmuebleDetalle(2L, "INM-0002", "Depa 201", EstadoInmueble.ALQUILADO, 2, 1);
        Contrato contrato =
                contrato(201L, inmueble, "Ana Pérez", "12345678", LocalDate.of(2026, 3, 5));
        prepararDetalle(inmueble, contrato);

        MaquetaService.DetalleMaqueta detalle = service.detalle(2L);

        assertNotNull(detalle.contrato());
        assertEquals(SemaforoMorosidad.AL_DIA, detalle.contrato().semaforo());
        assertEquals(0, detalle.contrato().cuotasVencidas());
        assertEquals(new BigDecimal("1000.00"), detalle.contrato().montoPendiente());
        assertEquals("Ana Pérez", detalle.contrato().arrendatario());
        assertEquals("12345678", detalle.contrato().documento());
        assertEquals(new BigDecimal("1000.00"), detalle.contrato().rentaMensual());
        assertEquals(LocalDate.of(2026, 2, 1), detalle.contrato().fechaInicio());
        assertEquals(LocalDate.of(2027, 1, 31), detalle.contrato().fechaFin());
        assertEquals(355L, detalle.contrato().diasParaVencer());
        assertFalse(detalle.inconsistencia());
    }

    @Test
    void detalleDeAlquiladoConAtrasoLeve() {
        Inmueble inmueble =
                inmuebleDetalle(3L, "INM-0003", "Depa 202", EstadoInmueble.ALQUILADO, 2, 2);
        Contrato contrato =
                contrato(202L, inmueble, "Luis Paredes", "22345678", LocalDate.of(2026, 2, 5));
        prepararDetalle(inmueble, contrato);

        MaquetaService.DetalleMaqueta detalle = service.detalle(3L);

        assertEquals(SemaforoMorosidad.ATRASO_LEVE, detalle.contrato().semaforo());
        assertEquals(1, detalle.contrato().cuotasVencidas());
        assertEquals(new BigDecimal("1005.00"), detalle.contrato().montoPendiente());
    }

    @Test
    void detalleDeAlquiladoMoroso() {
        Inmueble inmueble =
                inmuebleDetalle(4L, "INM-0004", "Depa 203", EstadoInmueble.ALQUILADO, 2, 3);
        Contrato contrato =
                contrato(203L, inmueble, "Rosa Vega", "32345678", LocalDate.of(2025, 12, 20));
        prepararDetalle(inmueble, contrato);

        MaquetaService.DetalleMaqueta detalle = service.detalle(4L);

        assertEquals(SemaforoMorosidad.MOROSO, detalle.contrato().semaforo());
        assertEquals(1, detalle.contrato().cuotasVencidas());
        assertEquals(new BigDecimal("1052.00"), detalle.contrato().montoPendiente());
    }

    @Test
    void detalleDeAlquiladoSinContratoVigente() {
        Inmueble inmueble =
                inmuebleDetalle(5L, "INM-0005", "Depa 204", EstadoInmueble.ALQUILADO, 2, 4);
        prepararDetalle(inmueble);

        MaquetaService.DetalleMaqueta detalle = service.detalle(5L);

        assertNull(detalle.contrato());
        assertTrue(detalle.inconsistencia());
        assertEquals(EstadoInmueble.ALQUILADO, detalle.estado());
        for (RecordComponent componente :
                MaquetaService.DetalleMaqueta.class.getRecordComponents()) {
            assertFalse(Inmueble.class.isAssignableFrom(componente.getType()));
            assertFalse(Contrato.class.isAssignableFrom(componente.getType()));
        }
        for (RecordComponent componente :
                MaquetaService.ContratoDetalle.class.getRecordComponents()) {
            assertFalse(Inmueble.class.isAssignableFrom(componente.getType()));
            assertFalse(Contrato.class.isAssignableFrom(componente.getType()));
        }
    }

    @Test
    void categoriasDeVencimientoRespetanLosLimites() {
        assertEquals(
                CategoriaVencimiento.VENCIDO, bloqueConVencimiento(-1L).categoriaVencimiento());
        assertEquals(
                CategoriaVencimiento.HASTA_30_DIAS,
                bloqueConVencimiento(0L).categoriaVencimiento());
        assertEquals(
                CategoriaVencimiento.HASTA_30_DIAS,
                bloqueConVencimiento(30L).categoriaVencimiento());
        assertEquals(
                CategoriaVencimiento.DE_31_A_90_DIAS,
                bloqueConVencimiento(31L).categoriaVencimiento());
        assertEquals(
                CategoriaVencimiento.DE_31_A_90_DIAS,
                bloqueConVencimiento(90L).categoriaVencimiento());
        assertEquals(
                CategoriaVencimiento.MAS_DE_90_DIAS,
                bloqueConVencimiento(91L).categoriaVencimiento());
        assertEquals(
                CategoriaVencimiento.SIN_CONTRATO,
                bloqueConVencimiento(null).categoriaVencimiento());
        assertEquals(CategoriaVencimiento.SIN_CONTRATO, bloqueSinContrato().categoriaVencimiento());
    }

    @Test
    void categoriasDeMoraSoloAplicanAAlquiladosConSemaforo() {
        assertEquals(
                CategoriaMora.AL_DIA,
                bloqueMora(EstadoInmueble.ALQUILADO, SemaforoMorosidad.AL_DIA).categoriaMora());
        assertEquals(
                CategoriaMora.ATRASO_LEVE,
                bloqueMora(EstadoInmueble.ALQUILADO, SemaforoMorosidad.ATRASO_LEVE)
                        .categoriaMora());
        assertEquals(
                CategoriaMora.MOROSO,
                bloqueMora(EstadoInmueble.ALQUILADO, SemaforoMorosidad.MOROSO).categoriaMora());
        assertEquals(
                CategoriaMora.SIN_MORA, bloqueMora(EstadoInmueble.ALQUILADO, null).categoriaMora());
        assertEquals(
                CategoriaMora.SIN_MORA,
                bloqueMora(EstadoInmueble.DISPONIBLE, null).categoriaMora());
    }

    @Test
    void degradadoDeRentaManejaRangosIgualesYDistintos() {
        BigDecimal mil = new BigDecimal("1000.00");
        BigDecimal dosMil = new BigDecimal("2000.00");

        assertEquals(0, MaquetaService.tonoRenta(mil, mil, dosMil, 8));
        assertEquals(4, MaquetaService.tonoRenta(new BigDecimal("1500.00"), mil, dosMil, 8));
        assertEquals(7, MaquetaService.tonoRenta(dosMil, mil, dosMil, 8));
        assertEquals(0, MaquetaService.tonoRenta(new BigDecimal("900.00"), mil, dosMil, 8));
        assertEquals(7, MaquetaService.tonoRenta(new BigDecimal("2100.00"), mil, dosMil, 8));
        assertEquals(3, MaquetaService.tonoRenta(mil, mil, mil, 8));
        assertEquals(-1, MaquetaService.tonoRenta(null, mil, dosMil, 8));
    }

    private BloqueMaqueta bloqueConVencimiento(Long dias) {
        return new BloqueMaqueta(
                1L,
                "INM-0001",
                "Depa 101",
                "Departamento",
                1,
                1,
                EstadoInmueble.ALQUILADO,
                10L,
                "Ana Pérez",
                SemaforoMorosidad.AL_DIA,
                false,
                new BigDecimal("1000.00"),
                LocalDate.of(2027, 1, 1),
                dias);
    }

    private BloqueMaqueta bloqueSinContrato() {
        return new BloqueMaqueta(
                2L,
                "INM-0002",
                "Depa 102",
                "Departamento",
                1,
                2,
                EstadoInmueble.DISPONIBLE,
                null,
                null,
                null,
                false,
                new BigDecimal("1000.00"),
                null,
                null);
    }

    private BloqueMaqueta bloqueMora(EstadoInmueble estado, SemaforoMorosidad semaforo) {
        return new BloqueMaqueta(
                3L,
                "INM-0003",
                "Depa 103",
                "Departamento",
                1,
                3,
                estado,
                null,
                null,
                semaforo,
                false,
                new BigDecimal("1000.00"),
                null,
                null);
    }

    private Inmueble inmueble(
            Long id,
            String codigo,
            String nombre,
            TipoInmueble tipo,
            EstadoInmueble estado,
            Integer nivel,
            Integer orden) {
        return Inmueble.builder()
                .id(id)
                .codigo(codigo)
                .nombre(nombre)
                .tipo(tipo)
                .estado(estado)
                .nivel(nivel)
                .orden(orden)
                .areaM2(new BigDecimal("55.00"))
                .dormitorios(2)
                .banos(1)
                .amoblado(false)
                .rentaReferencial(new BigDecimal("1200.00"))
                .build();
    }

    private Inmueble inmuebleDetalle(
            Long id,
            String codigo,
            String nombre,
            EstadoInmueble estado,
            Integer nivel,
            Integer orden) {
        return Inmueble.builder()
                .id(id)
                .codigo(codigo)
                .nombre(nombre)
                .tipo(TipoInmueble.builder().id(2L).codigo("TI-DEP").nombre("Departamento").build())
                .estado(estado)
                .nivel(nivel)
                .orden(orden)
                .direccion("Av. Los Andes 1245")
                .distrito("Juliaca")
                .areaM2(new BigDecimal("80.00"))
                .dormitorios(2)
                .banos(1)
                .rentaReferencial(new BigDecimal("1200.00"))
                .build();
    }

    private Contrato contrato(
            Long id, Inmueble inmueble, String nombres, String documento, LocalDate vencimiento) {
        Persona arrendatario =
                Persona.builder()
                        .id(id)
                        .tipoDocumento(TipoDocumento.DNI)
                        .numeroDocumento(documento)
                        .nombres(nombres)
                        .build();
        LocalDate inicio = vencimiento.minusMonths(1).withDayOfMonth(1);
        Contrato contrato =
                Contrato.builder()
                        .id(id)
                        .codigo("CTR-" + id)
                        .inmueble(inmueble)
                        .arrendatario(arrendatario)
                        .fechaInicio(inicio)
                        .fechaFin(inicio.plusMonths(12).minusDays(1))
                        .rentaMensual(new BigDecimal("1000.00"))
                        .diasGracia(3)
                        .tipoMora(com.ejemplo.alquileres.model.TipoMora.PORCENTAJE_DIARIO)
                        .valorMora(new BigDecimal("0.10"))
                        .estado(EstadoContrato.VIGENTE)
                        .build();
        contrato.agregarCuota(
                Cuota.builder()
                        .numero(1)
                        .tipo(TipoCuota.RENTA)
                        .fechaVencimiento(vencimiento)
                        .montoCuota(new BigDecimal("1000.00"))
                        .montoPagado(BigDecimal.ZERO)
                        .estado(EstadoCuota.PENDIENTE)
                        .build());
        return contrato;
    }

    private void prepararDetalle(Inmueble inmueble, Contrato... contratosVigentes) {
        when(inmuebles.detallePorId(inmueble.getId())).thenReturn(Optional.of(inmueble));
        when(contratos.vigentesPorInmuebleConDetalle(inmueble.getId()))
                .thenReturn(List.of(contratosVigentes));
    }

    private BloqueMaqueta bloque(MaquetaService.ResultadoMaqueta resultado, Long id) {
        return resultado.bloques().stream()
                .filter(b -> b.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
