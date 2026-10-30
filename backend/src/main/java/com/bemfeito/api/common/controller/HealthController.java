package com.bemfeito.api.common.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint simples utilizado para verificar se a API está disponível.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public HealthController(org.springframework.jdbc.core.JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public Map<String, String> health() {
        try { jdbc.queryForObject("SELECT 1", Integer.class); }
        catch (org.springframework.dao.DataAccessException unavailable) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Serviço temporariamente indisponível");
        }
        return Map.of(
            "status", "UP",
            "application", "Bem-Feito API"
        );
    }
}
