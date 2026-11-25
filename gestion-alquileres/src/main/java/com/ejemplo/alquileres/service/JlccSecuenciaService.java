package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Secuencia;
import com.ejemplo.alquileres.repository.SecuenciaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JlccSecuenciaService {
    private final SecuenciaRepository jlccRepository;

    @Transactional
    public long jlccSiguiente(String jlccClave) {
        if (jlccClave == null || jlccClave.isBlank())
            throw new IllegalArgumentException("La clave de secuencia es obligatoria");
        String jlccNormalizada = jlccClave.trim().toUpperCase();
        if (jlccRepository.incrementar(jlccNormalizada) == 0) {
            try {
                jlccRepository.saveAndFlush(new Secuencia(null, jlccNormalizada, 0L));
            } catch (DataIntegrityViolationException jlccIgnorada) {
            }
            if (jlccRepository.incrementar(jlccNormalizada) == 0)
                throw new IllegalStateException(
                        "No se pudo inicializar la secuencia " + jlccNormalizada);
        }
        return jlccRepository.findByClave(jlccNormalizada).orElseThrow().getUltimoValor();
    }

    public String jlccFormatear(String jlccPrefijo, long jlccValor, int jlccAncho) {
        if (jlccAncho < 1) throw new IllegalArgumentException("El ancho debe ser positivo");
        return jlccPrefijo + String.format("%0" + jlccAncho + "d", jlccValor);
    }
}
