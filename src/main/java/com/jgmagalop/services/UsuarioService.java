package com.jgmagalop.services;

import com.jgmagalop.config.JwtUtil;
import com.jgmagalop.domain.Usuario;
import com.jgmagalop.domain.UsuarioRepository;
import com.jgmagalop.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void registrar(RegisterRequest req) {
        if (usuarioRepository.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }
        Usuario u = new Usuario();
        u.setEmail(req.email());
        u.setNombre(req.nombre());
        u.setPassword(passwordEncoder.encode(req.password()));
        usuarioRepository.save(u);
    }

    public AuthResponse login(LoginRequest req) {
        return usuarioRepository.findByEmail(req.email())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .map(u -> new AuthResponse(jwtUtil.generarToken(u.getEmail())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
    }
}