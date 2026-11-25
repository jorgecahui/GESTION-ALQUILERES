package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.ConfiguracionRepository;

import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

final class DatosFase6 {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-10T12:00:00Z"), ZoneOffset.UTC);
    static final LocalDate HOY = LocalDate.of(2026, 2, 10);

    private DatosFase6() {}

    static DatosArrendador arrendador() {
        return new DatosArrendador(configuracion());
    }

    static Configuracion configuracion() {
        return Configuracion.builder()
                .id(Configuracion.FILA_UNICA)
                .arrendadorRazonSocial("ARRENDADOR DE PRUEBA")
                .arrendadorTipoDocumento("RUC")
                .arrendadorDocumento("20600000001")
                .arrendadorDireccion("Av. Prueba 123, Lima")
                .arrendadorTelefono("999999999")
                .arrendadorEmail("pruebas@ejemplo.pe")
                .reciboSerie("R001")
                .reciboLeyenda("Documento interno sin validez tributaria")
                .moraTipo(TipoMora.PORCENTAJE_DIARIO)
                .moraValor(new BigDecimal("0.10"))
                .moraDiasGracia(3)
                .alertasDiasVencimiento(30)
                .alertaRecordatorioDias(3)
                .semaforoUmbralLeveDias(15)
                .semaforoCuotasRojo(2)
                .plantillaPreventivo(JlccPlantillasMensaje.JLCC_PREVENTIVO)
                .plantillaVencido(JlccPlantillasMensaje.JLCC_VENCIDO)
                .plantillaFinContrato(JlccPlantillasMensaje.JLCC_FIN_CONTRATO)
                .build();
    }

    static JlccConfiguracionService servicioConfiguracion() {
        return servicioConfiguracion(propiedades());
    }

    static JlccConfiguracionService servicioConfiguracion(AppProperties propiedades) {
        ConfiguracionRepository repositorio = Mockito.mock(ConfiguracionRepository.class);
        Mockito.when(repositorio.findById(Configuracion.FILA_UNICA)).thenReturn(Optional.empty());
        Mockito.when(repositorio.save(Mockito.any(Configuracion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        return new JlccConfiguracionService(repositorio, propiedades, permisosAdmin(), auditoria());
    }

    static JlccPermisos permisosAdmin() {
        return permisos(RolUsuario.ADMIN);
    }

    static JlccPermisos permisosOperador() {
        return permisos(RolUsuario.OPERADOR);
    }

    static JlccPermisos permisosConsulta() {
        return permisos(RolUsuario.CONSULTA);
    }

    static JlccPermisos permisos(RolUsuario rol) {
        JlccSesionActual sesion = new JlccSesionActual();
        sesion.jlccIniciar(
                Usuario.builder()
                        .id(1L)
                        .username("prueba")
                        .nombre("Usuario de prueba")
                        .rol(rol)
                        .activo(true)
                        .build());
        return new JlccPermisos(sesion);
    }

    static JlccAuditoriaService auditoria() {
        return Mockito.mock(JlccAuditoriaService.class);
    }

    static AppProperties propiedades() {
        AppProperties propiedades = new AppProperties();
        propiedades.getArrendador().setRazonSocial("ARRENDADOR DE PRUEBA");
        propiedades.getArrendador().setTipoDocumento("RUC");
        propiedades.getArrendador().setDocumento("20600000001");
        propiedades.getArrendador().setDireccion("Av. Prueba 123, Lima");
        propiedades.getArrendador().setTelefono("999999999");
        propiedades.getArrendador().setEmail("pruebas@ejemplo.pe");
        propiedades.getAlertas().setDiasVencimiento(30);
        propiedades.getAlertas().setRecordatorioDias(3);
        propiedades.getRecibos().setSerie("R001");
        propiedades.getRecibos().setLeyenda("Documento interno sin validez tributaria");
        propiedades.getMorosidad().setUmbralLeveDias(15);
        propiedades.getMorosidad().setCuotasRojo(2);
        return propiedades;
    }

    static Persona arrendatario(String telefono) {
        return Persona.builder()
                .id(1L)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("12345678")
                .nombres("Ana")
                .apellidos("Pérez")
                .telefono(telefono)
                .email("ana@ejemplo.pe")
                .build();
    }

    static Inmueble inmueble() {
        TipoInmueble tipo =
                TipoInmueble.builder().id(1L).codigo("DEP").nombre("Departamento").build();
        return Inmueble.builder()
                .id(1L)
                .codigo("INM-0001")
                .nombre("Departamento 101")
                .tipo(tipo)
                .direccion("Av. Los Álamos 456")
                .distrito("Miraflores")
                .areaM2(new BigDecimal("85.00"))
                .dormitorios(3)
                .banos(2)
                .estacionamientos(1)
                .amoblado(true)
                .rentaReferencial(new BigDecimal("1200.00"))
                .estado(EstadoInmueble.ALQUILADO)
                .serviciosIncluidos("Agua y mantenimiento")
                .build();
    }

    static Contrato contrato() {
        Contrato contrato =
                Contrato.builder()
                        .id(1L)
                        .codigo("CTR-000001")
                        .inmueble(inmueble())
                        .arrendatario(arrendatario("987654321"))
                        .fechaInicio(LocalDate.of(2026, 1, 1))
                        .fechaFin(LocalDate.of(2026, 12, 31))
                        .diaPago(5)
                        .rentaMensual(new BigDecimal("1200.00"))
                        .incrementoAnualPct(new BigDecimal("5.00"))
                        .garantiaMonto(new BigDecimal("1200.00"))
                        .garantiaEstado(GarantiaEstado.RECIBIDA)
                        .diasGracia(3)
                        .tipoMora(TipoMora.PORCENTAJE_DIARIO)
                        .valorMora(new BigDecimal("0.10"))
                        .estado(EstadoContrato.VIGENTE)
                        .build();
        Cuota enero = cuota(10L, 1, LocalDate.of(2026, 1, 5), "1200.00", "1200.00");
        Cuota febrero = cuota(11L, 2, LocalDate.of(2026, 2, 5), "1200.00", "0.00");
        contrato.agregarCuota(enero);
        contrato.agregarCuota(febrero);
        return contrato;
    }

    static Cuota cuota(Long id, int numero, LocalDate vencimiento, String monto, String pagado) {
        return Cuota.builder()
                .id(id)
                .numero(numero)
                .tipo(TipoCuota.RENTA)
                .concepto("Alquiler")
                .periodo(vencimiento.withDayOfMonth(1))
                .fechaVencimiento(vencimiento)
                .montoCuota(new BigDecimal(monto))
                .montoPagado(new BigDecimal(pagado))
                .moraAcumulada(BigDecimal.ZERO)
                .moraCobrada(BigDecimal.ZERO)
                .moraCondonada(BigDecimal.ZERO)
                .estado(EstadoCuota.PENDIENTE)
                .build();
    }

    static Pago pago(
            Contrato contrato, Cuota cuota, String recibo, String total, EstadoPago estado) {
        Pago pago =
                Pago.builder()
                        .id(1L)
                        .recibo(recibo)
                        .contrato(contrato)
                        .fechaPago(LocalDate.of(2026, 1, 5))
                        .medioPago(MedioPago.TRANSFERENCIA)
                        .estado(estado)
                        .arrendatarioNombre(contrato.getArrendatario().getNombreCompleto())
                        .arrendatarioDocumento(contrato.getArrendatario().getNumeroDocumento())
                        .inmuebleCodigo(contrato.getInmueble().getCodigo())
                        .inmuebleNombre(contrato.getInmueble().getNombre())
                        .montoCapital(new BigDecimal(total))
                        .montoMora(BigDecimal.ZERO)
                        .montoTotal(new BigDecimal(total))
                        .build();
        pago.agregarDetalle(
                PagoDetalle.builder()
                        .cuota(cuota)
                        .item(1)
                        .concepto(cuota.getConcepto())
                        .periodo(cuota.getPeriodo())
                        .fechaVencimiento(cuota.getFechaVencimiento())
                        .montoCapital(new BigDecimal(total))
                        .montoMora(BigDecimal.ZERO)
                        .montoMoraCondonada(BigDecimal.ZERO)
                        .build());
        return pago;
    }

    static JlccRentabilidadService.Resumen rentabilidad() {
        JlccRentabilidadService.Fila fila =
                new JlccRentabilidadService.Fila(
                        1L,
                        "INM-0001",
                        "Departamento 101",
                        new BigDecimal("1200.00"),
                        new BigDecimal("30.00"),
                        new BigDecimal("1230.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("1030.00"),
                        new BigDecimal("83.74"),
                        31,
                        new BigDecimal("100.00"));
        return new JlccRentabilidadService.Resumen(
                List.of(fila),
                new BigDecimal("1230.00"),
                new BigDecimal("200.00"),
                new BigDecimal("1030.00"),
                new BigDecimal("83.74"));
    }

    static Mantenimiento mantenimiento() {
        return Mantenimiento.builder()
                .id(1L)
                .codigo("MNT-000001")
                .inmueble(inmueble())
                .fechaReporte(LocalDate.of(2026, 1, 20))
                .titulo("Reparación de grifería")
                .categoria(CategoriaMantenimiento.PLOMERIA)
                .prioridad(PrioridadMantenimiento.MEDIA)
                .estado(EstadoMantenimiento.RESUELTO)
                .responsablePago(ResponsablePago.PROPIETARIO)
                .fechaResolucion(LocalDate.of(2026, 1, 22))
                .costoEstimado(new BigDecimal("150.00"))
                .costoReal(new BigDecimal("180.00"))
                .tecnico("Técnico de prueba")
                .fechaRegistro(LocalDate.of(2026, 1, 20))
                .build();
    }
}
