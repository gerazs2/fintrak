package com.jgmagalop.services;

import com.jgmagalop.domain.Usuario;
import com.jgmagalop.domain.UsuarioRepository;
import com.jgmagalop.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.jgmagalop.config.JwtUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioActual usuarioActual;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public PerfilService(UsuarioRepository usuarioRepository,
                         UsuarioActual usuarioActual,
                         PasswordEncoder passwordEncoder,
                         JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioActual = usuarioActual;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtener() {
        return PerfilResponse.from(usuarioActual.get());
    }

    @Transactional
    public PerfilResponse actualizar(ActualizarPerfilRequest req) {
        Usuario u = usuarioActual.get();
        u.setNombre(req.nombre().trim());
        return PerfilResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public AuthResponse cambiarPassword(CambiarPasswordRequest req) {
        Usuario u = usuarioActual.get();

        if (!passwordEncoder.matches(req.passwordActual(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        if (passwordEncoder.matches(req.passwordNueva(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe ser diferente a la actual");
        }

        u.setPassword(passwordEncoder.encode(req.passwordNueva()));
        // El JWT guarda la fecha de emisión en segundos, así que se trunca igual para comparar
        u.setPasswordCambiadaEn(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        usuarioRepository.save(u);

        // Token nuevo para que la sesión actual siga activa
        return new AuthResponse(jwtUtil.generarToken(u.getEmail()));
    }
}