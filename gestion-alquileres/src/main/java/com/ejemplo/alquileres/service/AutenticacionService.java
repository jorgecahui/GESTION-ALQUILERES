package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AutenticacionService {
    public static final int INTENTOS_MAXIMOS = 5;
    public static final int MINUTOS_BLOQUEO = 5;
    private static final String CREDENCIALES = "Usuario o contraseña incorrectos.";

    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder;
    private final AuditoriaService auditoria;
    private final SesionActual sesion;
    private final Clock clock;

    @Transactional(readOnly = true)
    public boolean hayUsuarios() {
        return usuarios.count() > 0;
    }

    @Transactional
    public Usuario crearPrimerAdmin(
            String username, String nombre, String password, String confirmacion) {
        if (hayUsuarios()) throw new NegocioException("Ya existe un usuario administrador.");
        validarPassword(password);
        if (!password.equals(confirmacion))
            throw new NegocioException("Las contraseñas no coinciden.");
        String usuario = normalizarUsername(username);
        if (nombre == null || nombre.isBlank())
            throw new NegocioException("El nombre es obligatorio.");
        if (usuarios.existsByUsernameIgnoreCase(usuario))
            throw new NegocioException("El usuario ya existe.");
        Usuario admin =
                usuarios.save(
                        Usuario.builder()
                                .username(usuario)
                                .nombre(nombre.trim())
                                .passwordHash(encoder.encode(password))
                                .rol(RolUsuario.ADMIN)
                                .activo(true)
                                .intentosFallidos(0)
                                .build());
        auditoria.registrarComo(
                usuario,
                AccionAuditoria.GESTION_USUARIO,
                "Usuario",
                admin.getId(),
                "Alta del primer administrador");
        return admin;
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public Usuario login(String username, String password) {
        String usuario = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        Usuario encontrado =
                usuario.isBlank() ? null : usuarios.findByUsernameIgnoreCase(usuario).orElse(null);
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (encontrado == null) {
            auditoria.registrarComo(
                    usuario.isBlank() ? "desconocido" : usuario,
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    null,
                    "Credenciales inválidas");
            throw new NegocioException(CREDENCIALES);
        }
        if (!encontrado.isActivo()) {
            auditoria.registrarComo(
                    encontrado.getUsername(),
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    encontrado.getId(),
                    "Usuario inactivo");
            throw new NegocioException(CREDENCIALES);
        }
        if (encontrado.estaBloqueado(ahora)) {
            auditoria.registrarComo(
                    encontrado.getUsername(),
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    encontrado.getId(),
                    "Cuenta bloqueada temporalmente");
            throw new NegocioException(
                    "La cuenta está bloqueada temporalmente. Intente nuevamente en unos minutos.");
        }
        if (encontrado.getBloqueadoHasta() != null) {
            encontrado.setBloqueadoHasta(null);
            encontrado.setIntentosFallidos(0);
        }
        if (password == null || !encoder.matches(password, encontrado.getPasswordHash())) {
            encontrado.setIntentosFallidos(encontrado.getIntentosFallidos() + 1);
            if (encontrado.getIntentosFallidos() >= INTENTOS_MAXIMOS) {
                encontrado.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
            }
            usuarios.save(encontrado);
            auditoria.registrarComo(
                    encontrado.getUsername(),
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    encontrado.getId(),
                    "Credenciales inválidas");
            throw new NegocioException(CREDENCIALES);
        }
        encontrado.setIntentosFallidos(0);
        encontrado.setBloqueadoHasta(null);
        encontrado.setUltimoAcceso(ahora);
        Usuario guardado = usuarios.save(encontrado);
        sesion.iniciar(guardado);
        auditoria.registrar(
                AccionAuditoria.INICIO_SESION, "Usuario", guardado.getId(), "Acceso correcto");
        return guardado;
    }

    @Transactional
    public void cerrarSesion() {
        Usuario actual = sesion.usuario();
        if (actual != null) {
            auditoria.registrar(
                    AccionAuditoria.CIERRE_SESION, "Usuario", actual.getId(), "Sesión finalizada");
        }
        sesion.cerrar();
    }

    @Transactional(readOnly = true)
    public boolean verificarPassword(String password) {
        Usuario actual = sesion.usuario();
        if (actual == null || password == null) return false;
        Usuario guardado = usuarios.findById(actual.getId()).orElse(null);
        return guardado != null && encoder.matches(password, guardado.getPasswordHash());
    }

    public String nombreUsuarioActual() {
        return sesion.usuario() == null ? "" : sesion.usuario().getNombre();
    }

    public String rolActual() {
        return sesion.rol() == null ? "" : sesion.rol().getEtiqueta();
    }

    public static void validarPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new NegocioException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new NegocioException("La contraseña debe incluir letras y números.");
        }
    }

    public static String normalizarUsername(String username) {
        String valor = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (!valor.matches("[a-z0-9._-]{3,60}")) {
            throw new NegocioException(
                    "El usuario debe tener entre 3 y 60 caracteres: letras minúsculas, números,"
                        + " punto, guion o guion bajo.");
        }
        return valor;
    }
}
