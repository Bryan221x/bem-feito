package com.bemfeito.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercita HTTP, services, queries JPA e DTOs no banco isolado, sem mocks. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferenceDataApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void statesRemainAvailableInAlphabeticalOrder() throws Exception {
        mvc.perform(get("/api/states"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].uf", contains("AC", "MG", "SP")))
            .andExpect(jsonPath("$[0].id", isA(Number.class)));
    }

    @Test
    void citiesCanBeFilteredByUfWithNestedStateDto() throws Exception {
        mvc.perform(get("/api/cities").param("state", " mg "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Belo Horizonte", "Luz")))
            .andExpect(jsonPath("$[*].state.uf", everyItem(is("MG"))))
            .andExpect(jsonPath("$[0].state.name", is("Minas Gerais")))
            .andExpect(jsonPath("$[0].state.id", isA(Number.class)));
    }

    @Test
    void allCitiesAreSortedAndStateIdUsesDatabaseIdentity() throws Exception {
        mvc.perform(get("/api/cities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Belo Horizonte", "Luz", "São Paulo")));
        Long stateId = jdbc.queryForObject("SELECT id FROM states WHERE uf = 'SP'", Long.class);
        mvc.perform(get("/api/cities").param("stateId", stateId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("São Paulo")));
    }

    @Test
    void citiesReturnEmptyArrayWhenNoMatch() throws Exception {
        for (String uf : new String[]{"AC", "ZZ"}) {
            mvc.perform(get("/api/cities").param("state", uf))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        }
        mvc.perform(get("/api/cities").param("stateId", "9223372036854775807"))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        jdbc.update("DELETE FROM cities");
        mvc.perform(get("/api/cities"))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void invalidOrAmbiguousCityFiltersAreRejected() throws Exception {
        for (String uf : new String[]{"", "M", "MINAS", "1!"}) {
            mvc.perform(get("/api/cities").param("state", uf)).andExpect(status().isBadRequest());
        }
        for (String id : new String[]{"0", "-1", "abc"}) {
            mvc.perform(get("/api/cities").param("stateId", id)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/cities").param("state", "MG").param("stateId", "1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void onlyActiveTaxonomiesAreReturnedInOrder() throws Exception {
        for (String endpoint : new String[]{"institution-categories", "service-areas", "need-categories"}) {
            String[] expected = endpoint.equals("need-categories")
                ? new String[]{"Alimentos", "Roupas e Calçados"} : new String[]{"Educação", "Saúde"};
            mvc.perform(get("/api/" + endpoint))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains(expected)))
                .andExpect(jsonPath("$[0].id", isA(Number.class)))
                .andExpect(jsonPath("$[0].description", nullValue()))
                .andExpect(jsonPath("$[0].active").doesNotExist());
        }
    }

    @Test
    void inactiveOnlyTaxonomiesReturnEmptyArraysWithoutChangingProductionData() throws Exception {
        for (String table : new String[]{"institution_categories", "service_areas", "need_categories"}) {
            jdbc.update("UPDATE " + table + " SET active = FALSE");
            mvc.perform(get("/api/" + table.replace('_', '-')))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        }
    }

    @Test
    void referenceEndpointsDoNotAllowCreation() throws Exception {
        for (String endpoint : new String[]{"cities", "institution-categories", "service-areas", "need-categories"}) {
            mvc.perform(post("/api/" + endpoint)).andExpect(status().isMethodNotAllowed());
        }
    }

    @Test
    void cityRelationshipEnforcesStateForeignKey() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cities c JOIN states s ON s.id = c.state_id", Long.class))
            .isEqualTo(jdbc.queryForObject("SELECT COUNT(*) FROM cities", Long.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO cities (state_id, name) VALUES (?, ?)", Long.MAX_VALUE, "Luz"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
