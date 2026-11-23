package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta y mantenimiento de usuarios; protege al último administrador activo. */
@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder;
    private final Permisos permisos;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        permisos.exigirAdmin();
        return usuarios.findAllByOrderByUsernameAsc();
    }

    @Transactional
    public Usuario crear(String username, String nombre, String password, String confirmacion, RolUsuario rol, boolean activo) {
        permisos.exigirAdmin();
        AutenticacionService.validarPassword(password);
        if (!password.equals(confirmacion)) throw new NegocioException("Las contraseñas no coinciden.");
        if (nombre == null || nombre.isBlank()) throw new NegocioException("El nombre es obligatorio.");
        if (rol == null) throw new NegocioException("Selecciona un rol.");
        String usuario = AutenticacionService.normalizarUsername(username);
        if (usuarios.existsByUsernameIgnoreCase(usuario)) throw new NegocioException("El usuario ya existe.");
        Usuario creado = guardar(Usuario.builder()
                .username(usuario)
                .nombre(nombre.trim())
                .passwordHash(encoder.encode(password))
                .rol(rol)
                .activo(activo)
                .intentosFallidos(0)
                .build());
        auditoria.registrar(AccionAuditoria.GESTION_USUARIO, "Usuario", creado.getId(),
                "Alta de usuario " + usuario + " (" + rol.name() + ")");
        return creado;
    }

    @Transactional
    public Usuario actualizar(Long id, String nombre, RolUsuario rol, boolean activo) {
        permisos.exigirAdmin();
        if (nombre == null || nombre.isBlank()) throw new NegocioException("El nombre es obligatorio.");
        if (rol == null) throw new NegocioException("Selecciona un rol.");
        Usuario usuario = usuarios.findById(id).orElseThrow(() -> new NegocioException("El usuario no existe."));
        boolean dejaDeSerAdminActivo = usuario.isActivo() && usuario.esAdmin() && (rol != RolUsuario.ADMIN || !activo);
        if (dejaDeSerAdminActivo && usuarios.countByRolAndActivoTrue(RolUsuario.ADMIN) <= 1) {
            throw new NegocioException("No se puede desactivar ni degradar al último ADMIN activo.");
        }
        usuario.setNombre(nombre.trim());
        usuario.setRol(rol);
        usuario.setActivo(activo);
        Usuario guardado = guardar(usuario);
        auditoria.registrar(AccionAuditoria.GESTION_USUARIO, "Usuario", id,
                "Actualización de usuario " + usuario.getUsername() + " (" + rol.name() + ", "
                        + (activo ? "activo" : "inactivo") + ")");
        return guardado;
    }

    @Transactional
    public void restablecerPassword(Long id, String password, String confirmacion) {
        permisos.exigirAdmin();
        AutenticacionService.validarPassword(password);
        if (!password.equals(confirmacion)) throw new NegocioException("Las contraseñas no coinciden.");
        Usuario usuario = usuarios.findById(id).orElseThrow(() -> new NegocioException("El usuario no existe."));
        usuario.setPasswordHash(encoder.encode(password));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        guardar(usuario);
        auditoria.registrar(AccionAuditoria.GESTION_USUARIO, "Usuario", id,
                "Restablecimiento de contraseña de " + usuario.getUsername());
    }

    private Usuario guardar(Usuario usuario) {
        try {
            return usuarios.save(usuario);
        } catch (ObjectOptimisticLockingFailureException error) {
            throw new NegocioException(
                    "El usuario fue modificado por otra operación. Actualice e intente de nuevo.");
        }
    }
}
