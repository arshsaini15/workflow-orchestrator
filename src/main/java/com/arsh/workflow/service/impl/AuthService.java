package com.arsh.workflow.service.impl;

import com.arsh.workflow.dto.request.LoginRequestDto;
import com.arsh.workflow.dto.request.RegisterRequestDto;
import com.arsh.workflow.dto.response.AuthResponseDto;
import com.arsh.workflow.enums.Role;
import com.arsh.workflow.mapper.UserMapper;
import com.arsh.workflow.model.User;
import com.arsh.workflow.repository.UserRepository;
import com.arsh.workflow.util.JwtUtil;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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


    public AuthResponseDto register(RegisterRequestDto req) {
        User user = UserMapper.toEntity(req);

        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(Role.USER);

        repo.save(user);

        return new AuthResponseDto(jwtUtil.generateToken(user.getUsername()));
    }

    public AuthResponseDto login(LoginRequestDto req) {

        Authentication authToken = new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword());
        authManager.authenticate(authToken);

        String token = jwtUtil.generateToken(req.getUsername());

        return new AuthResponseDto(token);
    }
}
