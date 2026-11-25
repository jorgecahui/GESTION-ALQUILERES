package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccUsuarioService {
    private final UsuarioRepository jlccUsuarios;
    private final BCryptPasswordEncoder jlccEncoder;
    private final JlccPermisos jlccPermisos;
    private final JlccAuditoriaService jlccAuditoria;

    @Transactional(readOnly = true)
    public List<Usuario> jlccListar() {
        jlccPermisos.jlccExigirAdmin();
        return jlccUsuarios.findAllByOrderByUsernameAsc();
    }

    @Transactional
    public Usuario jlccCrear(
            String jlccUsername,
            String jlccNombre,
            String jlccPassword,
            String jlccConfirmacion,
            RolUsuario jlccRol,
            boolean jlccActivo) {
        jlccPermisos.jlccExigirAdmin();
        JlccAutenticacionService.jlccValidarPassword(jlccPassword);
        if (!jlccPassword.equals(jlccConfirmacion))
            throw new JlccNegocioException("Las contraseñas no coinciden.");
        if (jlccNombre == null || jlccNombre.isBlank())
            throw new JlccNegocioException("El nombre es obligatorio.");
        if (jlccRol == null) throw new JlccNegocioException("Selecciona un rol.");
        String jlccUsuario = JlccAutenticacionService.jlccNormalizarUsername(jlccUsername);
        if (jlccUsuarios.existsByUsernameIgnoreCase(jlccUsuario))
            throw new JlccNegocioException("El usuario ya existe.");
        Usuario jlccCreado =
                jlccGuardar(
                        Usuario.builder()
                                .username(jlccUsuario)
                                .nombre(jlccNombre.trim())
                                .passwordHash(jlccEncoder.encode(jlccPassword))
                                .rol(jlccRol)
                                .activo(jlccActivo)
                                .intentosFallidos(0)
                                .build());
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.GESTION_USUARIO,
                "Usuario",
                jlccCreado.getId(),
                "Alta de usuario " + jlccUsuario + " (" + jlccRol.name() + ")");
        return jlccCreado;
    }

    @Transactional
    public Usuario jlccActualizar(Long jlccId, String jlccNombre, RolUsuario jlccRol, boolean jlccActivo) {
        jlccPermisos.jlccExigirAdmin();
        if (jlccNombre == null || jlccNombre.isBlank())
            throw new JlccNegocioException("El nombre es obligatorio.");
        if (jlccRol == null) throw new JlccNegocioException("Selecciona un rol.");
        Usuario jlccUsuario =
                jlccUsuarios.findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El usuario no existe."));
        boolean jlccDejaDeSerAdminActivo =
                jlccUsuario.isActivo() && jlccUsuario.esAdmin() && (jlccRol != RolUsuario.ADMIN || !jlccActivo);
        if (jlccDejaDeSerAdminActivo && jlccUsuarios.countByRolAndActivoTrue(RolUsuario.ADMIN) <= 1) {
            throw new JlccNegocioException(
                    "No se puede desactivar ni degradar al último ADMIN activo.");
        }
        jlccUsuario.setNombre(jlccNombre.trim());
        jlccUsuario.setRol(jlccRol);
        jlccUsuario.setActivo(jlccActivo);
        Usuario jlccGuardado = jlccGuardar(jlccUsuario);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.GESTION_USUARIO,
                "Usuario",
                jlccId,
                "Actualización de usuario "
                        + jlccUsuario.getUsername()
                        + " ("
                        + jlccRol.name()
                        + ", "
                        + (jlccActivo ? "activo" : "inactivo")
                        + ")");
        return jlccGuardado;
    }

    @Transactional
    public void jlccRestablecerPassword(Long jlccId, String jlccPassword, String jlccConfirmacion) {
        jlccPermisos.jlccExigirAdmin();
        JlccAutenticacionService.jlccValidarPassword(jlccPassword);
        if (!jlccPassword.equals(jlccConfirmacion))
            throw new JlccNegocioException("Las contraseñas no coinciden.");
        Usuario jlccUsuario =
                jlccUsuarios.findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El usuario no existe."));
        jlccUsuario.setPasswordHash(jlccEncoder.encode(jlccPassword));
        jlccUsuario.setIntentosFallidos(0);
        jlccUsuario.setBloqueadoHasta(null);
        jlccGuardar(jlccUsuario);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.GESTION_USUARIO,
                "Usuario",
                jlccId,
                "Restablecimiento de contraseña de " + jlccUsuario.getUsername());
    }

    private Usuario jlccGuardar(Usuario jlccUsuario) {
        try {
            return jlccUsuarios.save(jlccUsuario);
        } catch (ObjectOptimisticLockingFailureException jlccError) {
            throw new JlccNegocioException(
                    "El usuario fue modificado por otra operación. Actualice e intente de nuevo.");
        }
    }
}
