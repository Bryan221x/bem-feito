package com.bemfeito.api.security;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import com.bemfeito.api.user.service.CurrentUserService;
import com.bemfeito.api.user.dto.UserResponse;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final CurrentUserService users;
    public AuthController(CurrentUserService users) { this.users=users; }
    public record CsrfResponse(String token,String headerName,String parameterName) {}
    @GetMapping("/csrf") public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(),token.getHeaderName(),token.getParameterName());
    }
    @GetMapping("/me") public UserResponse me() { return users.current(); }
}
