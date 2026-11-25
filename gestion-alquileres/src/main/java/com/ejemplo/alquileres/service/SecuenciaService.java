package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Secuencia;
import com.ejemplo.alquileres.repository.SecuenciaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecuenciaService {
    private final SecuenciaRepository repository;

    @Transactional
    public long siguiente(String clave) {
        if (clave == null || clave.isBlank())
            throw new IllegalArgumentException("La clave de secuencia es obligatoria");
        String normalizada = clave.trim().toUpperCase();
        if (repository.incrementar(normalizada) == 0) {
            try {
                repository.saveAndFlush(new Secuencia(null, normalizada, 0L));
            } catch (DataIntegrityViolationException ignorada) {
            }
            if (repository.incrementar(normalizada) == 0)
                throw new IllegalStateException(
                        "No se pudo inicializar la secuencia " + normalizada);
        }
        return repository.findByClave(normalizada).orElseThrow().getUltimoValor();
    }

    public String formatear(String prefijo, long valor, int ancho) {
        if (ancho < 1) throw new IllegalArgumentException("El ancho debe ser positivo");
        return prefijo + String.format("%0" + ancho + "d", valor);
    }
}
