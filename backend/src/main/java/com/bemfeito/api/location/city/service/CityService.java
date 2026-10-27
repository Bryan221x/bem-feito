package com.bemfeito.api.location.city.service;

import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.bemfeito.api.location.city.dto.CityResponse;
import com.bemfeito.api.location.city.entity.City;
import com.bemfeito.api.location.city.repository.CityRepository;

@Service
public class CityService {
    private final CityRepository repository;

    public CityService(CityRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CityResponse> findAll(String state, Long stateId) {
        if (state != null && stateId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use apenas state ou stateId.");
        }
        List<City> cities;
        if (state != null) {
            String uf = state.trim().toUpperCase(Locale.ROOT);
            if (!uf.matches("[A-Z]{2}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "state deve ser uma UF de duas letras.");
            }
            cities = repository.findByStateUfIgnoreCaseOrderByNameAsc(uf);
        } else if (stateId != null) {
            if (stateId <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stateId deve ser positivo.");
            }
            cities = repository.findByStateIdOrderByNameAsc(stateId);
        } else {
            cities = repository.findAllByOrderByNameAscStateUfAsc();
        }
        return cities.stream().map(CityResponse::from).toList();
    }
}
