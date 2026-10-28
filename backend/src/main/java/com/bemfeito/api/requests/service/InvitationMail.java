package com.bemfeito.api.requests.service;
import java.time.LocalDateTime;
/** Ephemeral event; never serialize or log its secret URL. */
public record InvitationMail(Long userId,String email,String name,String institution,String activationUrl,LocalDateTime expiresAt) {
    @Override public String toString() { return "InvitationMail[redacted]"; }
}
