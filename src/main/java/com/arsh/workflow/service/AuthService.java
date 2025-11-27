package com.arsh.workflow.service;

import com.arsh.workflow.dto.*;
import com.arsh.workflow.model.Role;
import com.arsh.workflow.model.User;
import com.arsh.workflow.repository.UserRepository;
import com.arsh.workflow.util.JwtUtil;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;


@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authManager, UserRepository repo, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.authManager = authManager;
        this.repo = repo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }


    public void register(RegisterRequestDto req) {

        User user = User.builder()
                .username(req.getUsername())
                .password(encoder.encode(req.getPassword()))
                .role(Role.USER)
                .build();

        repo.save(user);
    }


    public AuthResponseDto login(LoginRequestDto req) {
        Authentication authToken = new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword());
        authManager.authenticate(authToken);

        String token = jwtUtil.generateToken(req.getUsername());

        return new AuthResponseDto(token);
    }
}
