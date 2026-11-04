package com.ejemplo.alquileres.ui;
import javafx.scene.control.TextField;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
/** Contrato liviano para el autocompletado reutilizable de las próximas fases. */
public class Autocompletar<T> {
    public Autocompletar(TextField campo, Function<String, List<T>> buscador, Consumer<T> alSeleccionar) { }
}
