package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
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
public class JlccAutenticacionService {
    public static final int JLCC_INTENTOS_MAXIMOS = 5;
    public static final int JLCC_MINUTOS_BLOQUEO = 5;
    private static final String JLCC_CREDENCIALES = "Usuario o contraseña incorrectos.";

    private final UsuarioRepository jlccUsuarios;
    private final BCryptPasswordEncoder jlccEncoder;
    private final JlccAuditoriaService jlccAuditoria;
    private final JlccSesionActual jlccSesion;
    private final Clock jlccClock;

    @Transactional(readOnly = true)
    public boolean jlccHayUsuarios() {
        return jlccUsuarios.count() > 0;
    }

    @Transactional
    public Usuario jlccCrearPrimerAdmin(
            String jlccUsername, String jlccNombre, String jlccPassword, String jlccConfirmacion) {
        if (jlccHayUsuarios()) throw new JlccNegocioException("Ya existe un usuario administrador.");
        jlccValidarPassword(jlccPassword);
        if (!jlccPassword.equals(jlccConfirmacion))
            throw new JlccNegocioException("Las contraseñas no coinciden.");
        String jlccUsuario = jlccNormalizarUsername(jlccUsername);
        if (jlccNombre == null || jlccNombre.isBlank())
            throw new JlccNegocioException("El nombre es obligatorio.");
        if (jlccUsuarios.existsByUsernameIgnoreCase(jlccUsuario))
            throw new JlccNegocioException("El usuario ya existe.");
        Usuario jlccAdmin =
                jlccUsuarios.save(
                        Usuario.builder()
                                .username(jlccUsuario)
                                .nombre(jlccNombre.trim())
                                .passwordHash(jlccEncoder.encode(jlccPassword))
                                .rol(RolUsuario.ADMIN)
                                .activo(true)
                                .intentosFallidos(0)
                                .build());
        jlccAuditoria.jlccRegistrarComo(
                jlccUsuario,
                AccionAuditoria.GESTION_USUARIO,
                "Usuario",
                jlccAdmin.getId(),
                "Alta del primer administrador");
        return jlccAdmin;
    }

    @Transactional(noRollbackFor = JlccNegocioException.class)
    public Usuario jlccLogin(String jlccUsername, String jlccPassword) {
        String jlccUsuario = jlccUsername == null ? "" : jlccUsername.trim().toLowerCase(Locale.ROOT);
        Usuario jlccEncontrado =
                jlccUsuario.isBlank() ? null : jlccUsuarios.findByUsernameIgnoreCase(jlccUsuario).orElse(null);
        LocalDateTime jlccAhora = LocalDateTime.now(jlccClock);
        if (jlccEncontrado == null) {
            jlccAuditoria.jlccRegistrarComo(
                    jlccUsuario.isBlank() ? "desconocido" : jlccUsuario,
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    null,
                    "Credenciales inválidas");
            throw new JlccNegocioException(JLCC_CREDENCIALES);
        }
        if (!jlccEncontrado.isActivo()) {
            jlccAuditoria.jlccRegistrarComo(
                    jlccEncontrado.getUsername(),
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    jlccEncontrado.getId(),
                    "Usuario inactivo");
            throw new JlccNegocioException(JLCC_CREDENCIALES);
        }
        if (jlccEncontrado.estaBloqueado(jlccAhora)) {
            jlccAuditoria.jlccRegistrarComo(
                    jlccEncontrado.getUsername(),
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    jlccEncontrado.getId(),
                    "Cuenta bloqueada temporalmente");
            throw new JlccNegocioException(
                    "La cuenta está bloqueada temporalmente. Intente nuevamente en unos minutos.");
        }
        if (jlccEncontrado.getBloqueadoHasta() != null) {
            jlccEncontrado.setBloqueadoHasta(null);
            jlccEncontrado.setIntentosFallidos(0);
        }
        if (jlccPassword == null || !jlccEncoder.matches(jlccPassword, jlccEncontrado.getPasswordHash())) {
            jlccEncontrado.setIntentosFallidos(jlccEncontrado.getIntentosFallidos() + 1);
            if (jlccEncontrado.getIntentosFallidos() >= JLCC_INTENTOS_MAXIMOS) {
                jlccEncontrado.setBloqueadoHasta(jlccAhora.plusMinutes(JLCC_MINUTOS_BLOQUEO));
            }
            jlccUsuarios.save(jlccEncontrado);
            jlccAuditoria.jlccRegistrarComo(
                    jlccEncontrado.getUsername(),
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    jlccEncontrado.getId(),
                    "Credenciales inválidas");
            throw new JlccNegocioException(JLCC_CREDENCIALES);
        }
        jlccEncontrado.setIntentosFallidos(0);
        jlccEncontrado.setBloqueadoHasta(null);
        jlccEncontrado.setUltimoAcceso(jlccAhora);
        Usuario jlccGuardado = jlccUsuarios.save(jlccEncontrado);
        jlccSesion.jlccIniciar(jlccGuardado);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.INICIO_SESION, "Usuario", jlccGuardado.getId(), "Acceso correcto");
        return jlccGuardado;
    }

    @Transactional
    public void jlccCerrarSesion() {
        Usuario jlccActual = jlccSesion.jlccUsuario();
        if (jlccActual != null) {
            jlccAuditoria.jlccRegistrar(
                    AccionAuditoria.CIERRE_SESION, "Usuario", jlccActual.getId(), "Sesión finalizada");
        }
        jlccSesion.jlccCerrar();
    }

    @Transactional(readOnly = true)
    public boolean jlccVerificarPassword(String jlccPassword) {
        Usuario jlccActual = jlccSesion.jlccUsuario();
        if (jlccActual == null || jlccPassword == null) return false;
        Usuario jlccGuardado = jlccUsuarios.findById(jlccActual.getId()).orElse(null);
        return jlccGuardado != null && jlccEncoder.matches(jlccPassword, jlccGuardado.getPasswordHash());
    }

    public String jlccNombreUsuarioActual() {
        return jlccSesion.jlccUsuario() == null ? "" : jlccSesion.jlccUsuario().getNombre();
    }

    public String jlccRolActual() {
        return jlccSesion.jlccRol() == null ? "" : jlccSesion.jlccRol().getEtiqueta();
    }

    public static void jlccValidarPassword(String jlccPassword) {
        if (jlccPassword == null || jlccPassword.length() < 8) {
            throw new JlccNegocioException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (!jlccPassword.matches(".*[A-Za-z].*") || !jlccPassword.matches(".*\\d.*")) {
            throw new JlccNegocioException("La contraseña debe incluir letras y números.");
        }
    }

    public static String jlccNormalizarUsername(String jlccUsername) {
        String jlccValor = jlccUsername == null ? "" : jlccUsername.trim().toLowerCase(Locale.ROOT);
        if (!jlccValor.matches("[a-z0-9._-]{3,60}")) {
            throw new JlccNegocioException(
                    "El usuario debe tener entre 3 y 60 caracteres: letras minúsculas, números,"
                        + " punto, guion o guion bajo.");
        }
        return jlccValor;
    }
}
