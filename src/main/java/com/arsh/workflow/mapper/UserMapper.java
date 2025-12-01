package com.arsh.workflow.mapper;

import com.arsh.workflow.dto.AuthResponseDto;
import com.arsh.workflow.dto.RegisterRequestDto;
import com.arsh.workflow.model.User;

public class UserMapper {

    public static User toEntity(RegisterRequestDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        return user;
    }

    public static AuthResponseDto toAuthResponse(User user, String token) {
        AuthResponseDto res = new AuthResponseDto();
        res.setToken(token);
        return res;
    }
}
