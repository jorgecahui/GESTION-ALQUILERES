package com.ejemplo.alquileres.ui;

import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class Navegacion {
    public record Entrada(
            String clave, String titulo, String descripcion, int fase, Feather icono) {}

    private final List<Entrada> entradas =
            List.of(
                    new Entrada(
                            "dashboard",
                            "Dashboard",
                            "Indicadores y resumen de la administración.",
                            4,
                            Feather.GRID),
                    new Entrada(
                            "maqueta-3d",
                            "Maqueta 3D",
                            "Vista 3D del estado de los inmuebles.",
                            8,
                            Feather.BOX),
                    new Entrada(
                            "nuevo-contrato",
                            "Nuevo contrato",
                            "Registra un contrato de alquiler.",
                            2,
                            Feather.FILE_PLUS),
                    new Entrada(
                            "contratos",
                            "Contratos",
                            "Consulta los contratos registrados.",
                            2,
                            Feather.FILE_TEXT),
                    new Entrada(
                            "nuevo-pago",
                            "Nuevo pago",
                            "Registra un pago de alquiler.",
                            3,
                            Feather.DOLLAR_SIGN),
                    new Entrada(
                            "pagos",
                            "Pagos",
                            "Consulta los pagos y recibos.",
                            3,
                            Feather.CREDIT_CARD),
                    new Entrada(
                            "cuentas-cobrar",
                            "Cuentas por cobrar",
                            "Revisa cuotas pendientes y vencidas.",
                            3,
                            Feather.ALERT_CIRCLE),
                    new Entrada(
                            "mantenimiento",
                            "Mantenimiento",
                            "Registra incidencias y gastos.",
                            5,
                            Feather.TOOL),
                    new Entrada(
                            "rentabilidad",
                            "Rentabilidad",
                            "Analiza ingresos y gastos por inmueble.",
                            5,
                            Feather.BAR_CHART_2),
                    new Entrada(
                            "inmuebles",
                            "Inmuebles",
                            "Administra los inmuebles disponibles.",
                            1,
                            Feather.HOME),
                    new Entrada(
                            "tipos-inmueble",
                            "Tipos de inmueble",
                            "Clasifica los inmuebles.",
                            1,
                            Feather.TAG),
                    new Entrada(
                            "personas",
                            "Personas",
                            "Administra arrendatarios y garantes.",
                            1,
                            Feather.USERS),
                    new Entrada(
                            "propietarios",
                            "Propietarios",
                            "Administra los propietarios.",
                            1,
                            Feather.KEY),
                    new Entrada(
                            "configuracion",
                            "Configuración",
                            "Administra los parámetros de la aplicación.",
                            7,
                            Feather.SETTINGS));
    private final Map<String, Entrada> porClave =
            entradas.stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(Entrada::clave, e -> e));

    public Entrada obtener(String clave) {
        return porClave.get(clave);
    }

    public List<Entrada> entradas() {
        return entradas;
    }

    public String recurso(String clave) {
        if (!porClave.containsKey(clave))
            throw new IllegalArgumentException("Clave desconocida: " + clave);
        return switch (clave) {
            case "dashboard" -> "/view/dashboard-view.fxml";
            case "maqueta-3d" -> "/view/maqueta-view.fxml";
            case "nuevo-contrato" -> "/view/contrato-form-view.fxml";
            case "contratos" -> "/view/contratos-view.fxml";
            case "nuevo-pago" -> "/view/pago-form-view.fxml";
            case "pagos" -> "/view/pagos-view.fxml";
            case "cuentas-cobrar" -> "/view/cuentas-por-cobrar-view.fxml";
            case "mantenimiento" -> "/view/mantenimiento-view.fxml";
            case "rentabilidad" -> "/view/rentabilidad-view.fxml";
            case "inmuebles" -> "/view/inmueble-view.fxml";
            case "tipos-inmueble" -> "/view/tipo-inmueble-view.fxml";
            case "personas" -> "/view/persona-view.fxml";
            case "propietarios" -> "/view/propietario-view.fxml";
            case "configuracion" -> "/view/configuracion-view.fxml";
            default -> "/view/placeholder-view.fxml";
        };
    }
}
