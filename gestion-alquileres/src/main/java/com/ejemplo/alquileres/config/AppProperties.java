package com.ejemplo.alquileres.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public class AppProperties {
    private String dataDir;
    private final Seeder seeder = new Seeder();
    private final Alertas alertas = new Alertas();
    private final Morosidad morosidad = new Morosidad();
    private final Mora mora = new Mora();
    private final Recibos recibos = new Recibos();
    private final Arrendador arrendador = new Arrendador();
    private final Plantillas plantillas = new Plantillas();
    private final Seguridad seguridad = new Seguridad();
    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    public Seeder getSeeder() { return seeder; } public Alertas getAlertas() { return alertas; }
    public Morosidad getMorosidad() { return morosidad; } public Mora getMora() { return mora; }
    public Recibos getRecibos() { return recibos; } public Arrendador getArrendador() { return arrendador; }
    public Plantillas getPlantillas() { return plantillas; } public Seguridad getSeguridad() { return seguridad; }
    public static class Seeder { private boolean enabled; public boolean isEnabled(){return enabled;} public void setEnabled(boolean enabled){this.enabled=enabled;} }
    public static class Alertas { private int diasVencimiento; private int recordatorioDias; public int getDiasVencimiento(){return diasVencimiento;} public void setDiasVencimiento(int v){diasVencimiento=v;} public int getRecordatorioDias(){return recordatorioDias;} public void setRecordatorioDias(int v){recordatorioDias=v;} }
    public static class Morosidad { private int umbralLeveDias; private int cuotasRojo; public int getUmbralLeveDias(){return umbralLeveDias;} public void setUmbralLeveDias(int v){umbralLeveDias=v;} public int getCuotasRojo(){return cuotasRojo;} public void setCuotasRojo(int v){cuotasRojo=v;} }
    public static class Mora { private String tipo; private BigDecimal valor; private int diasGracia; public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;} public BigDecimal getValor(){return valor;} public void setValor(BigDecimal v){valor=v;} public int getDiasGracia(){return diasGracia;} public void setDiasGracia(int v){diasGracia=v;} }
    public static class Recibos { private String serie; private String leyenda; public String getSerie(){return serie;} public void setSerie(String serie){this.serie=serie;} public String getLeyenda(){return leyenda;} public void setLeyenda(String leyenda){this.leyenda=leyenda;} }
    public static class Arrendador { private String razonSocial, tipoDocumento, documento, direccion, telefono, email; public String getRazonSocial(){return razonSocial;} public void setRazonSocial(String v){razonSocial=v;} public String getTipoDocumento(){return tipoDocumento;} public void setTipoDocumento(String v){tipoDocumento=v;} public String getDocumento(){return documento;} public void setDocumento(String v){documento=v;} public String getDireccion(){return direccion;} public void setDireccion(String v){direccion=v;} public String getTelefono(){return telefono;} public void setTelefono(String v){telefono=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} }
    public static class Seguridad { private int inactividadMinutos; public int getInactividadMinutos(){return inactividadMinutos;} public void setInactividadMinutos(int v){inactividadMinutos=v;} }
    /** Plantillas de recordatorio; si están vacías se usan los textos por defecto. */
    public static class Plantillas {
        private String preventivo, vencido, finContrato;
        public String getPreventivo(){return preventivo;} public void setPreventivo(String v){preventivo=v;}
        public String getVencido(){return vencido;} public void setVencido(String v){vencido=v;}
        public String getFinContrato(){return finContrato;} public void setFinContrato(String v){finContrato=v;}
    }
}
