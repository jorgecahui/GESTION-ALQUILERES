package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.ui.BloqueMaqueta;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma los DTO de la maqueta sin exponer entidades JPA fuera de la transacción. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaquetaService {
    private final InmuebleRepository inmuebles;
    private final ContratoRepository contratos;
    private final MorosidadService morosidad;

    public record ResultadoMaqueta(List<BloqueMaqueta> bloques, int sinUbicacion) {
        public ResultadoMaqueta {
            bloques = List.copyOf(bloques);
        }
    }

    public List<BloqueMaqueta> listarBloques() {
        return cargar().bloques();
    }

    public ResultadoMaqueta cargar() {
        List<Inmueble> todos = inmuebles.listarParaMaqueta();
        Map<Long, Contrato> vigentes = new HashMap<>();
        for (Contrato contrato : contratos.vigentesConDetalle()) {
            if (contrato.getInmueble() != null) {
                vigentes.putIfAbsent(contrato.getInmueble().getId(), contrato);
            }
        }

        List<BloqueMaqueta> bloques = new ArrayList<>();
        int sinUbicacion = 0;
        for (Inmueble inmueble : todos) {
            if (inmueble.getNivel() == null || inmueble.getOrden() == null) {
                sinUbicacion++;
                continue;
            }
            Contrato contrato = vigentes.get(inmueble.getId());
            boolean alquilado = inmueble.getEstado() == EstadoInmueble.ALQUILADO;
            boolean inconsistencia = alquilado && contrato == null;
            SemaforoMorosidad semaforo = alquilado && contrato != null
                    ? morosidad.semaforo(contrato)
                    : null;
            String tipo = inmueble.getTipo() == null ? "Sin tipo" : inmueble.getTipo().getNombre();
            String arrendatario = contrato == null || contrato.getArrendatario() == null
                    ? null
                    : contrato.getArrendatario().getNombreCompleto();
            bloques.add(new BloqueMaqueta(
                    inmueble.getId(),
                    inmueble.getCodigo(),
                    inmueble.getNombre(),
                    tipo,
                    inmueble.getNivel(),
                    inmueble.getOrden(),
                    inmueble.getEstado(),
                    contrato == null ? null : contrato.getId(),
                    arrendatario,
                    semaforo,
                    inconsistencia));
        }
        return new ResultadoMaqueta(bloques, sinUbicacion);
    }
}
