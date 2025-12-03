package com.arsh.workflow.controller;

import com.arsh.workflow.dto.AuthResponseDto;
import com.arsh.workflow.dto.LoginRequestDto;
import com.arsh.workflow.dto.RegisterRequestDto;
import com.arsh.workflow.service.impl.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponseDto register(@RequestBody RegisterRequestDto req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody LoginRequestDto req) {
        return authService.login(req);
    }
}
