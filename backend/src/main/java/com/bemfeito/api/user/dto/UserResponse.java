package com.bemfeito.api.user.dto;
import com.bemfeito.api.user.entity.*;
public record UserResponse(Long id,String name,String email,PlatformRole platformRole,UserStatus status) {
    public static UserResponse from(User user) { return new UserResponse(user.getId(),user.getName(),user.getEmail(),user.getPlatformRole(),user.getStatus()); }
}
