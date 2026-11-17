package com.ejemplo.alquileres;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.ejemplo.alquileres.controller.ContratoController;
import com.ejemplo.alquileres.controller.ContratoFormController;
import com.ejemplo.alquileres.controller.FichaInmuebleController;
import com.ejemplo.alquileres.controller.InmuebleController;
import com.ejemplo.alquileres.controller.MainController;
import com.ejemplo.alquileres.controller.MaquetaController;
import com.ejemplo.alquileres.controller.MantenimientoController;
import com.ejemplo.alquileres.controller.PagosController;
import com.ejemplo.alquileres.controller.PersonaController;
import com.ejemplo.alquileres.controller.PropietarioController;
import com.ejemplo.alquileres.controller.TipoInmuebleController;
import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.service.ConfiguracionService;
import com.ejemplo.alquileres.service.ContratoPdfService;
import com.ejemplo.alquileres.service.ContratoService;
import com.ejemplo.alquileres.service.CronogramaGenerador;
import com.ejemplo.alquileres.service.EstadoCuentaPdfService;
import com.ejemplo.alquileres.service.InmuebleService;
import com.ejemplo.alquileres.service.MantenimientoService;
import com.ejemplo.alquileres.service.MaquetaService;
import com.ejemplo.alquileres.service.PagoService;
import com.ejemplo.alquileres.service.PersonaService;
import com.ejemplo.alquileres.service.PropietarioService;
import com.ejemplo.alquileres.service.ReciboPdfService;
import com.ejemplo.alquileres.service.RecordatorioService;
import com.ejemplo.alquileres.service.ReporteContratosExcelService;
import com.ejemplo.alquileres.service.ReporteInmueblesExcelService;
import com.ejemplo.alquileres.service.ReporteMantenimientoExcelService;
import com.ejemplo.alquileres.service.ReportePagosExcelService;
import com.ejemplo.alquileres.service.RentabilidadService;
import com.ejemplo.alquileres.service.TipoInmuebleService;
import com.ejemplo.alquileres.ui.Exportador;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Tareas;
import java.time.Clock;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FxmlSmokeTest {
    @BeforeAll static void iniciar() {
        try { Platform.startup(() -> { }); } catch (IllegalStateException ignorado) { }
    }

    @AfterAll static void cerrar() { Platform.exit(); }

    @Test void cargaFormularioDeContrato() throws Exception {
        ContratoFormController controlador = new ContratoFormController(
                mock(ContratoService.class), mock(InmuebleService.class), mock(PersonaService.class),
                new CronogramaGenerador(), mock(ConfiguracionService.class), mock(Tareas.class),
                mock(Notificador.class), mock(MainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/contrato-form-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaListaDeContratos() throws Exception {
        ContratoController controlador = new ContratoController(
                mock(ContratoService.class), mock(RecordatorioService.class),
                mock(EstadoCuentaPdfService.class), mock(ContratoPdfService.class),
                mock(ReporteContratosExcelService.class), mock(Tareas.class),
                mock(Notificador.class), mock(Exportador.class), mock(MainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/contratos-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaFichaDeInmueble() throws Exception {
        InmuebleController controlador = new InmuebleController(
                mock(InmuebleService.class), mock(TipoInmuebleService.class),
                mock(PropietarioService.class), mock(ReporteInmueblesExcelService.class),
                mock(FichaInmuebleController.class), mock(ContratoService.class),
                mock(Tareas.class), mock(Notificador.class), mock(Exportador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/inmueble-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaListaDePagos() throws Exception {
        PagosController controlador = new PagosController(
                mock(PagoService.class), mock(ReciboPdfService.class), mock(ReportePagosExcelService.class),
                mock(Tareas.class), mock(Notificador.class), mock(Exportador.class), mock(MainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/pagos-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaFormularioDeMantenimiento() throws Exception {
        MantenimientoController controlador = new MantenimientoController(
                mock(MantenimientoService.class), mock(InmuebleService.class),
                mock(ReporteMantenimientoExcelService.class), mock(RentabilidadService.class),
                Clock.systemUTC(), mock(Tareas.class), mock(Notificador.class), mock(Exportador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/mantenimiento-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaMaqueta3d() throws Exception {
        MaquetaController controlador = new MaquetaController(
                mock(MaquetaService.class), mock(Tareas.class), mock(Notificador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/maqueta-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaTiposDeInmueble() throws Exception {
        TipoInmuebleController controlador = new TipoInmuebleController(
                mock(TipoInmuebleService.class), mock(Tareas.class), mock(Notificador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/tipo-inmueble-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaPersonas() throws Exception {
        PersonaController controlador = new PersonaController(
                mock(PersonaService.class), mock(Tareas.class), mock(Notificador.class),
                mock(Exportador.class), mock(DatosArrendador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/persona-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaPropietarios() throws Exception {
        PropietarioController controlador = new PropietarioController(
                mock(PropietarioService.class), mock(Tareas.class), mock(Notificador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/propietario-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }
}
