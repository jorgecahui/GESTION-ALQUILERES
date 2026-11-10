package com.ejemplo.alquileres;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.ejemplo.alquileres.controller.ContratoController;
import com.ejemplo.alquileres.controller.ContratoFormController;
import com.ejemplo.alquileres.controller.MainController;
import com.ejemplo.alquileres.service.ConfiguracionService;
import com.ejemplo.alquileres.service.ContratoPdfService;
import com.ejemplo.alquileres.service.ContratoService;
import com.ejemplo.alquileres.service.CronogramaGenerador;
import com.ejemplo.alquileres.service.EstadoCuentaPdfService;
import com.ejemplo.alquileres.service.InmuebleService;
import com.ejemplo.alquileres.service.PersonaService;
import com.ejemplo.alquileres.service.RecordatorioService;
import com.ejemplo.alquileres.service.ReporteContratosExcelService;
import com.ejemplo.alquileres.ui.Exportador;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Tareas;
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
}
