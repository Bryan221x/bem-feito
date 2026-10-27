package com.bemfeito.api.location.city.dto;

import com.bemfeito.api.location.city.entity.City;
import com.bemfeito.api.location.state.dto.StateResponse;

public record CityResponse(Long id, String name, StateResponse state) {
    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName(), StateResponse.from(city.getState()));
    }
}
