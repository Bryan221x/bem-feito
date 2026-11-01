package com.bemfeito.api.security.recovery;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
@RestController @RequestMapping("/api/password-recovery")
public class PasswordRecoveryController {
    private final PasswordRecoveryService service;
    public PasswordRecoveryController(PasswordRecoveryService service) { this.service=service; }
    public record Request(@NotBlank @Email @Size(max=255) String email) { @Override public String toString() { return "Request[redacted]"; } }
    public record Reset(String token,String password,String confirmPassword) { @Override public String toString() { return "Reset[redacted]"; } }
    @PostMapping("/request") public Map<String,String> request(@Valid @RequestBody Request input) {
        try { service.request(input.email()).exceptionally(error -> null); } catch (org.springframework.core.task.TaskRejectedException ignored) { /* Mesmo resultado público quando a fila está cheia. */ }
        return Map.of("message","Se houver uma conta elegível e o envio estiver disponível, você receberá as instruções. Aguarde antes de solicitar novamente.");
    }
    @PostMapping("/reset") public Map<String,String> reset(@RequestBody Reset input) {
        service.reset(input.token(),input.password(),input.confirmPassword());return Map.of("message","Senha redefinida. Entre novamente com a nova senha.");
    }
}
