package com.bemfeito.api.location.city.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bemfeito.api.location.city.entity.City;

public interface CityRepository extends JpaRepository<City, Long> {
    @EntityGraph(attributePaths = "state")
    List<City> findAllByOrderByNameAscStateUfAsc();

    @EntityGraph(attributePaths = "state")
    List<City> findByStateUfIgnoreCaseOrderByNameAsc(String uf);

    @EntityGraph(attributePaths = "state")
    List<City> findByStateIdOrderByNameAsc(Long stateId);
}
