package com.bemfeito.api.location.state.dto;

import com.bemfeito.api.location.state.entity.State;

/**
 * Dados de um estado disponibilizados pela API.
 */
public record StateResponse(
    Long id,
    String name,
    String uf
) {

    public static StateResponse from(State state) {
        return new StateResponse(
            state.getId(),
            state.getName(),
            state.getUf()
        );
    }
}