package com.bemfeito.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.bemfeito.api.location.city.dto.CityResponse;
import com.bemfeito.api.location.city.service.CityService;
import com.bemfeito.api.location.state.service.StateService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verificação opt-in, somente leitura, do schema e da carga reais no MySQL. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql")
@EnabledIfEnvironmentVariable(named = "MYSQL_REFERENCE_TESTS", matches = "true")
class MySqlReferenceDataTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CityService cities;
    @Autowired StateService states;

    @Test
    void realSchemaAndAllPublicEndpointsWork() throws Exception {
        assertThat(states.findAll()).hasSize(27);
        for (String endpoint : new String[]{"states", "cities", "institution-categories", "service-areas", "need-categories"}) {
            mvc.perform(get("/api/" + endpoint)).andExpect(status().isOk());
        }
        for (String uf : jdbc.queryForList("SELECT uf FROM states", String.class)) {
            List<CityResponse> result = cities.findAll(uf, null);
            assertThat(result).isNotEmpty().allSatisfy(city -> assertThat(city.state().uf()).isEqualTo(uf));
            assertThat(result.stream().map(CityResponse::name).toList()).isEqualTo(
                jdbc.queryForList("SELECT c.name FROM cities c JOIN states s ON s.id=c.state_id WHERE s.uf=? ORDER BY c.name", String.class, uf));
        }
        assertThat(cities.findAll("ZZ", null)).isEmpty();
        Long sp = jdbc.queryForObject("SELECT id FROM states WHERE uf='SP'", Long.class);
        assertThat(cities.findAll(null, sp)).isEqualTo(cities.findAll("SP", null));
    }

    @Test
    void institutionalQueriesUseRealSchemaWithoutWritingOperationalData() throws Exception {
        var tables = List.of("institutions", "units", "institution_contacts", "unit_contacts",
            "unit_opening_hours", "unit_service_areas", "needs", "institution_history_images");
        var before = tables.stream().map(table -> jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)).toList();
        for (String endpoint : List.of("/api/institutions", "/api/cities/available", "/api/management/institutions")) {
            mvc.perform(get(endpoint)).andExpect(status().isOk());
        }
        var after = tables.stream().map(table -> jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)).toList();
        assertThat(after).isEqualTo(before);
    }
}
