package com.bemfeito.api.location.state.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bemfeito.api.location.state.dto.StateResponse;
import com.bemfeito.api.location.state.service.StateService;

/**
 * Endpoints públicos relacionados aos estados disponíveis no Bem-Feito.
 */
@RestController
@RequestMapping("/api/states")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping
    public List<StateResponse> findAll() {
        return stateService.findAll();
    }
}