package com.ejemplo.alquileres.model;

public enum AccionAuditoria {
    INICIO_SESION("Inicio de sesión"),
    CIERRE_SESION("Cierre de sesión"),
    LOGIN_FALLIDO("Intento de acceso fallido"),
    CREACION_PAGO("Registro de pago"),
    ANULACION_PAGO("Anulación de pago"),
    CONDONACION_MORA("Condonación de mora"),
    CREACION_CONTRATO("Creación de contrato"),
    FINALIZACION_CONTRATO("Finalización de contrato"),
    RESCISION_CONTRATO("Rescisión de contrato"),
    RENOVACION_CONTRATO("Renovación de contrato"),
    CAMBIO_CONFIGURACION("Cambio de configuración"),
    GESTION_USUARIO("Gestión de usuario"),
    RESPALDO("Respaldo de base de datos"),
    RESTAURACION("Restauración de base de datos");

    private final String etiqueta;

    AccionAuditoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
