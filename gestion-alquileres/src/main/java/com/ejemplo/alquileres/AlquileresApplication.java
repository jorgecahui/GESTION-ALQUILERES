package com.ejemplo.alquileres;

import com.ejemplo.alquileres.config.AppProperties;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class AlquileresApplication {}
