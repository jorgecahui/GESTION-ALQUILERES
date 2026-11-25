package com.ejemplo.alquileres.config;

import com.ejemplo.alquileres.service.ConfiguracionService;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InicializadorConfiguracion implements ApplicationRunner {
    private final ConfiguracionService configuracion;

    @Override
    public void run(ApplicationArguments args) {
        configuracion.inicializar();
    }
}
