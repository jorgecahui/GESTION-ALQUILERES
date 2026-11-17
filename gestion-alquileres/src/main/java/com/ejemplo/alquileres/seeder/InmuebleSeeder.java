package com.ejemplo.alquileres.seeder;

import static com.ejemplo.alquileres.model.EstadoInmueble.ALQUILADO;
import static com.ejemplo.alquileres.model.EstadoInmueble.DISPONIBLE;
import static com.ejemplo.alquileres.model.EstadoInmueble.INACTIVO;
import static com.ejemplo.alquileres.model.EstadoInmueble.MANTENIMIENTO;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.Propietario;
import com.ejemplo.alquileres.model.TipoInmueble;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.PropietarioRepository;
import com.ejemplo.alquileres.repository.TipoInmuebleRepository;
import com.ejemplo.alquileres.service.InmuebleService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Edificio demo de 4 niveles por 5 unidades para el perfil dev. */
@Component
@RequiredArgsConstructor
public class InmuebleSeeder {
    private static final String DIRECCION = "Av. Los Andes 1245, Juliaca";
    private static final String DISTRITO = "Juliaca";
    private static final String PROVINCIA = "San Román";
    private static final String DEPARTAMENTO = "Puno";
    private static final String SERVICIOS = "Agua, desagüe y mantenimiento del edificio";

    private final InmuebleRepository repo;
    private final TipoInmuebleRepository tipos;
    private final PropietarioRepository propietarios;
    private final InmuebleService service;

    public void cargar() {
        if (repo.count() > 0) return;

        Map<String, TipoInmueble> tiposPorCodigo = tipos.findAll().stream()
                .collect(Collectors.toMap(TipoInmueble::getCodigo, tipo -> tipo));
        Propietario propietario = propietarios.findAll().stream().findFirst().orElse(null);

        for (Unidad unidad : unidades()) {
            EstadoInmueble estadoInicial = unidad.estado() == ALQUILADO ? DISPONIBLE : unidad.estado();
            Inmueble inmueble = service.guardar(Inmueble.builder()
                    .nombre(unidad.nombre())
                    .tipo(tiposPorCodigo.get(unidad.tipo()))
                    .propietario(propietario)
                    .direccion(DIRECCION)
                    .referencia(referencia(unidad))
                    .distrito(DISTRITO)
                    .provincia(PROVINCIA)
                    .departamento(DEPARTAMENTO)
                    .piso("Piso " + unidad.nivel())
                    .nivel(unidad.nivel())
                    .orden(unidad.orden())
                    .areaM2(dinero(unidad.area()))
                    .dormitorios(unidad.dormitorios())
                    .banos(unidad.banos())
                    .estacionamientos(unidad.estacionamientos())
                    .amoblado(unidad.amoblado())
                    .rentaReferencial(dinero(unidad.renta()))
                    .estado(estadoInicial)
                    .serviciosIncluidos(SERVICIOS)
                    .descripcion(unidad.descripcion())
                    .build());
            if (unidad.estado() == ALQUILADO) {
                inmueble.setEstado(ALQUILADO);
                repo.save(inmueble);
            }
        }
    }

    private List<Unidad> unidades() {
        return List.of(
                new Unidad(1, 1, "Local 101", "TI-LOC", ALQUILADO, "95.00", 0, 1, 0, false, "2800.00",
                        "Local con vitrina hacia la avenida principal."),
                new Unidad(1, 2, "Oficina 102", "TI-OFI", DISPONIBLE, "72.00", 0, 1, 1, false, "1800.00",
                        "Oficina con recepción y ventana al patio interior."),
                new Unidad(1, 3, "Local 103", "TI-LOC", MANTENIMIENTO, "90.00", 0, 1, 0, false, "2600.00",
                        "Local comercial en mantenimiento de instalaciones."),
                new Unidad(1, 4, "Oficina 104", "TI-OFI", ALQUILADO, "68.00", 0, 1, 1, true, "1900.00",
                        "Oficina amoblada con acceso independiente."),
                new Unidad(1, 5, "Local 105", "TI-LOC", INACTIVO, "110.00", 0, 2, 0, false, "3100.00",
                        "Local amplio con depósito, actualmente fuera de servicio."),

                new Unidad(2, 1, "Depa 201", "TI-DEP", ALQUILADO, "78.00", 3, 2, 1, true, "1450.00",
                        "Departamento familiar con balcón y vista a la calle."),
                new Unidad(2, 2, "Depa 202", "TI-DEP", ALQUILADO, "65.00", 2, 1, 0, false, "1150.00",
                        "Departamento de dos dormitorios con cocina equipada."),
                new Unidad(2, 3, "Depa 203", "TI-DEP", DISPONIBLE, "70.00", 2, 2, 1, false, "1250.00",
                        "Departamento con sala-comedor y patio interior."),
                new Unidad(2, 4, "Depa 204", "TI-DEP", MANTENIMIENTO, "82.00", 3, 2, 1, true, "1500.00",
                        "Departamento en mantenimiento de baños y pintura."),
                new Unidad(2, 5, "Depa 205", "TI-DEP", ALQUILADO, "60.00", 2, 1, 0, true, "1050.00",
                        "Departamento compacto y amoblado."),

                new Unidad(3, 1, "Depa 301", "TI-DEP", ALQUILADO, "88.00", 3, 2, 1, true, "1600.00",
                        "Departamento con terraza y vista a la ciudad."),
                new Unidad(3, 2, "Depa 302", "TI-DEP", DISPONIBLE, "74.00", 2, 2, 1, false, "1300.00",
                        "Departamento luminoso con dos baños completos."),
                new Unidad(3, 3, "Depa 303", "TI-DEP", ALQUILADO, "66.00", 2, 1, 0, true, "1100.00",
                        "Departamento interior, ideal para una pareja."),
                new Unidad(3, 4, "Depa 304", "TI-DEP", ALQUILADO, "90.00", 3, 2, 1, true, "1650.00",
                        "Departamento amplio con estacionamiento asignado."),
                new Unidad(3, 5, "Depa 305", "TI-DEP", INACTIVO, "58.00", 1, 1, 0, false, "950.00",
                        "Depa de un dormitorio, retirado temporalmente del mercado."),

                new Unidad(4, 1, "Depa 401", "TI-DEP", MANTENIMIENTO, "92.00", 3, 2, 1, false, "1700.00",
                        "Departamento en mantenimiento de ventanas y pintura."),
                new Unidad(4, 2, "Depa 402", "TI-DEP", ALQUILADO, "76.00", 2, 2, 1, true, "1350.00",
                        "Departamento con balcón y closet empotrado."),
                new Unidad(4, 3, "Depa 403", "TI-DEP", ALQUILADO, "85.00", 2, 2, 1, true, "1500.00",
                        "Departamento con sala amplia y vista despejada."),
                new Unidad(4, 4, "Depa 404", "TI-DEP", DISPONIBLE, "62.00", 2, 1, 0, false, "1080.00",
                        "Departamento de dos dormitorios, listo para entrega."),
                new Unidad(4, 5, "Depa 405", "TI-DEP", DISPONIBLE, "68.00", 2, 1, 0, true, "1180.00",
                        "Departamento amoblado con ingreso por ascensor."));
    }

    private String referencia(Unidad unidad) {
        String ubicacion = switch (unidad.nivel()) {
            case 1 -> "Acceso desde la avenida";
            case 2 -> "Frente al patio interior";
            case 3 -> "Vista a la calle lateral";
            default -> "Vista panorámica";
        };
        return ubicacion + " · Módulo " + unidad.orden();
    }

    private static BigDecimal dinero(String valor) {
        return new BigDecimal(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private record Unidad(int nivel, int orden, String nombre, String tipo,
                          EstadoInmueble estado, String area, int dormitorios, int banos,
                          int estacionamientos, boolean amoblado, String renta, String descripcion) {
    }
}
