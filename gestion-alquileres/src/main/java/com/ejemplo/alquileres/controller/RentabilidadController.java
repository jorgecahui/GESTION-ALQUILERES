package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.Periodo;
import com.ejemplo.alquileres.service.MantenimientoService;
import com.ejemplo.alquileres.service.RentabilidadService;
import com.ejemplo.alquileres.service.ReporteMantenimientoExcelService;
import com.ejemplo.alquileres.ui.*;
import java.math.BigDecimal;
import java.time.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

@Component public class RentabilidadController implements Refrescable {
 private final RentabilidadService service; private final MantenimientoService mantenimientos; private final ReporteMantenimientoExcelService excel; private final Clock clock; private final Tareas tareas; private final Notificador aviso; private final Exportador exportador; private final ObservableList<RentabilidadService.Fila> datos=FXCollections.observableArrayList(); private RentabilidadService.Resumen actual; private Periodo.Rango rango;
 @FXML private ComboBox<Periodo> periodo; @FXML private DatePicker desde,hasta; @FXML private Label ingresos,gastos,neto,margen; @FXML private TableView<RentabilidadService.Fila> tabla; @FXML private TableColumn<RentabilidadService.Fila,String> colInmueble; @FXML private TableColumn<RentabilidadService.Fila,BigDecimal> colIngresos,colGastos,colNeto,colMargen,colOcupacion;
 public RentabilidadController(RentabilidadService s,MantenimientoService m,ReporteMantenimientoExcelService e,Clock c,Tareas t,Notificador a,Exportador x){service=s;mantenimientos=m;excel=e;clock=c;tareas=t;aviso=a;exportador=x;}
 @FXML public void initialize(){periodo.setItems(FXCollections.observableArrayList(Periodo.values()));periodo.setValue(Periodo.ESTE_ANIO);colInmueble.setCellValueFactory(c->new SimpleStringProperty(c.getValue().codigo()+" · "+c.getValue().inmueble()));colIngresos.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().ingresos()));colGastos.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().gastos()));colNeto.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().neto()));colMargen.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().margen()));colOcupacion.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().ocupacion()));for(TableColumn<RentabilidadService.Fila,BigDecimal> c:java.util.List.of(colIngresos,colGastos,colNeto))c.setCellFactory(x->moneda());for(TableColumn<RentabilidadService.Fila,BigDecimal> c:java.util.List.of(colMargen,colOcupacion))c.setCellFactory(x->porcentaje());tabla.setItems(datos);}
 public void refrescar(){Periodo.Rango r=periodo.getValue().resolver(clock,desde.getValue(),hasta.getValue());rango=r;tareas.ejecutar(()->service.porInmueble(r.desde(),r.hasta()),x->{actual=x;datos.setAll(x.filas());ingresos.setText(Tablas.moneda(x.ingresos()));gastos.setText(Tablas.moneda(x.gastos()));neto.setText(Tablas.moneda(x.neto()));margen.setText(x.margen()+" %");},e->aviso.error(e.getMessage()));}
 @FXML private void onActualizar(){refrescar();} @FXML private void onExportar(){if(actual==null||rango==null){aviso.error("Actualiza el reporte antes de exportarlo.");return;}java.io.File destino=exportador.elegirDestino(tabla.getScene()==null?null:tabla.getScene().getWindow(),"reporte-rentabilidad-"+java.time.LocalDate.now()+".xlsx","Excel","xlsx");if(destino==null)return;tareas.ejecutar(()->excel.generar(mantenimientos.gastosPropietario(rango.desde(),rango.hasta()),actual,rango.desde(),rango.hasta()),bytes->{exportador.escribir(destino,bytes);aviso.info("Excel generado.");},e->aviso.error(e.getMessage()));}private TableCell<RentabilidadService.Fila,BigDecimal> moneda(){return new TableCell<>(){protected void updateItem(BigDecimal v,boolean e){super.updateItem(v,e);setText(e?"":Tablas.moneda(v));}};}private TableCell<RentabilidadService.Fila,BigDecimal> porcentaje(){return new TableCell<>(){protected void updateItem(BigDecimal v,boolean e){super.updateItem(v,e);setText(e?"":v+" %");}};}
}
