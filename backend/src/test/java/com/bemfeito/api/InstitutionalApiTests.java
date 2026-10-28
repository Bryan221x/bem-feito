package com.bemfeito.api;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Dados operacionais são exclusivos do H2 e desfeitos ao final de cada teste. */
@SpringBootTest(properties={"spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.datasource.url=jdbc:h2:mem:institutional-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:reference-schema.sql",
    "spring.sql.init.data-locations=classpath:reference-fixtures.sql",
    "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.orm.session=OFF"})
@AutoConfigureMockMvc
@Transactional
@Sql("/institutional-fixtures.sql")
class InstitutionalApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    private Long institution(String name) { return jdbc.queryForObject("SELECT id FROM institutions WHERE display_name=?",Long.class,name); }
    private Long unit() { return jdbc.queryForObject("SELECT id FROM units WHERE name='Unidade Luz'",Long.class); }
    private Long city(String name) { return jdbc.queryForObject("SELECT id FROM cities WHERE name=?",Long.class,name); }
    private Long category() { return jdbc.queryForObject("SELECT id FROM institution_categories WHERE name='Educação'",Long.class); }
    private Long needCategory() { return jdbc.queryForObject("SELECT id FROM need_categories WHERE name='Alimentos'",Long.class); }
    private String instPath() { return "/api/institutions/"+institution("Alpha Teste"); }
    private String unitPath() { return "/api/units/"+unit(); }
    private ResultActions json(MockHttpServletRequestBuilder request,String body) throws Exception {
        return mvc.perform(request.contentType("application/json").content(body));
    }
    private Long id(String response) { return ((Number)JsonPath.read(response,"$.id")).longValue(); }
    private String institutionRequest() {
        return """
            {"categoryId":%d,"displayName":"Nova Teste","legalName":"Nova Razão Teste",
             "cnpj":"12345678000195","objective":"Objetivo novo","status":"ACTIVE"}
            """.formatted(category());
    }
    private String unitRequest() {
        return """
            {"cityId":%d,"name":"Nova Unidade Teste","postalCode":"35595000","street":"Rua de Teste",
             "number":"50","neighborhood":"Centro","status":"ACTIVE"}
            """.formatted(city("Luz"));
    }
    private String contact(boolean isPublic,String value) {
        return """
            {"type":"EMAIL","value":"%s","label":"Contato teste","isPrimary":false,"isPublic":%s,"displayOrder":0}
            """.formatted(value,isPublic);
    }
    private String needRequest() {
        return """
            {"categoryId":%d,"title":"Nova necessidade teste","quantity":2.5,"unitLabel":"kg","priority":"HIGH","status":"ACTIVE"}
            """.formatted(needCategory());
    }

    @Test void publicListIsLightOrderedAndPaginated() throws Exception {
        mvc.perform(get("/api/institutions")).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].displayName",contains("Alpha Teste","Gamma Teste")))
            .andExpect(jsonPath("$.content[0].legalName").doesNotExist())
            .andExpect(jsonPath("$.content[0].cnpj").doesNotExist())
            .andExpect(jsonPath("$.content[0].history").doesNotExist())
            .andExpect(jsonPath("$.totalElements",is(2)));
        mvc.perform(get("/api/institutions").param("size","1").param("page","1"))
            .andExpect(jsonPath("$.content[0].displayName",is("Gamma Teste")))
            .andExpect(jsonPath("$.totalPages",is(2)));
    }

    @Test void cityFilterRequiresActiveUnitAndActiveInstitutionWithoutDuplicates() throws Exception {
        json(post(instPath()+"/units"),unitRequest()).andExpect(status().isCreated());
        mvc.perform(get("/api/institutions").param("cityId",city("Luz").toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[*].displayName",contains("Alpha Teste")));
        mvc.perform(get("/api/institutions").param("cityId",city("Belo Horizonte").toString()))
            .andExpect(jsonPath("$.content",empty()));
        mvc.perform(get("/api/institutions").param("cityId",Long.toString(Long.MAX_VALUE)))
            .andExpect(jsonPath("$.content",empty()));
    }

    @Test void availableCitiesDifferFromTerritorialCatalog() throws Exception {
        mvc.perform(get("/api/cities/available")).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name",contains("Luz","São Paulo")));
        mvc.perform(get("/api/cities")).andExpect(jsonPath("$[*].name",hasItem("Belo Horizonte")));
        json(patch(instPath()),"{\"status\":\"INACTIVE\"}").andExpect(status().isOk());
        mvc.perform(get("/api/cities/available")).andExpect(content().json("[]"));
    }

    @Test void publicDetailsFilterPrivateContactsAndNonActiveNeeds() throws Exception {
        mvc.perform(get(instPath())).andExpect(status().isOk())
            .andExpect(jsonPath("$.contacts[*].value",contains("3133334444","public@example.org")))
            .andExpect(jsonPath("$.units[0].contacts[*].value",contains("unit-public@example.org")))
            .andExpect(jsonPath("$.units[0].needs[*].name",contains("Alimentos prioridade alta","Alimentos prioridade baixa")))
            .andExpect(jsonPath("$.units[0].address.city",is("Luz")))
            .andExpect(jsonPath("$.units[0].document").doesNotExist())
            .andExpect(jsonPath("$.cnpj").doesNotExist()).andExpect(jsonPath("$.legalName").doesNotExist());
        mvc.perform(get(instPath()+"/units")).andExpect(jsonPath("$",hasSize(2)));
        mvc.perform(get(unitPath())).andExpect(jsonPath("$.needs",hasSize(2)));
    }

    @Test void managementContainsPrivateDataAndInactiveChildren() throws Exception {
        mvc.perform(get("/api/management/institutions/"+institution("Alpha Teste")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.cnpj",is("11222333000181")))
            .andExpect(jsonPath("$.contacts",hasSize(3)))
            .andExpect(jsonPath("$.units[0].contacts",hasSize(2)))
            .andExpect(jsonPath("$.units[0].needs",hasSize(4)));
        mvc.perform(get("/api/management/institutions/"+institution("Gamma Teste")))
            .andExpect(jsonPath("$.units[0].status",is("INACTIVE")));
        mvc.perform(get("/api/management/institutions")).andExpect(jsonPath("$.content",hasSize(3)));
    }

    @Test void missingAndHiddenResourcesReturnConsistent404() throws Exception {
        for(String path:new String[]{"/api/institutions/9223372036854775807", "/api/units/9223372036854775807",
                "/api/institutions/"+institution("Beta Teste"),
                "/api/institutions/"+institution("Beta Teste")+"/units"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.trace").doesNotExist());
        }
        Long hidden=jdbc.queryForObject("SELECT id FROM units WHERE name='Unidade Oculta'",Long.class);
        mvc.perform(get("/api/units/"+hidden)).andExpect(status().isNotFound());
        mvc.perform(get("/api/institutions/"+institution("Gamma Teste"))).andExpect(jsonPath("$.units",empty()));
    }

    @Test void emptyPublicCatalogIsNotAnError() throws Exception {
        jdbc.update("UPDATE institutions SET status='INACTIVE'");
        mvc.perform(get("/api/institutions")).andExpect(status().isOk()).andExpect(jsonPath("$.content",empty()));
        mvc.perform(get("/api/cities/available")).andExpect(content().json("[]"));
    }

    @Test void institutionCanBeCreatedAndFullyEdited() throws Exception {
        String result=json(post("/api/institutions"),institutionRequest()).andExpect(status().isCreated())
            .andExpect(header().exists("Location")).andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        json(put("/api/institutions/"+id(result)),institutionRequest().replace("Nova Teste","Editada Teste"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName",is("Editada Teste")));
    }

    @Test void institutionPatchPreservesOmittedFieldsAndCollections() throws Exception {
        json(patch(instPath()),"{\"history\":\"História alterada\"}").andExpect(status().isOk())
            .andExpect(jsonPath("$.objective",is("Objetivo preservado")))
            .andExpect(jsonPath("$.description",is("Descrição preservada")))
            .andExpect(jsonPath("$.website",is("https://example.org")))
            .andExpect(jsonPath("$.contacts",hasSize(3))).andExpect(jsonPath("$.units",hasSize(2)));
        json(patch(instPath()),"{\"history\":null,\"website\":null}").andExpect(status().isOk())
            .andExpect(jsonPath("$.history",nullValue())).andExpect(jsonPath("$.website",nullValue()))
            .andExpect(jsonPath("$.objective",is("Objetivo preservado")));
        json(patch(instPath()),"{}").andExpect(status().isOk()).andExpect(jsonPath("$.displayName",is("Alpha Teste")));
    }

    @Test void textCapacityMatchesMysqlBytesWithoutReducingAsciiHistory() throws Exception {
        String longHistory="a".repeat(65535);
        json(patch(instPath()),"{\"history\":\""+longHistory+"\"}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.history",is(longHistory)));
        json(patch(instPath()),"{\"history\":\""+"a".repeat(65536)+"\"}")
            .andExpect(status().isBadRequest());
        json(patch(instPath()),"{\"history\":\""+"é".repeat(32768)+"\"}")
            .andExpect(status().isBadRequest());
    }

    @Test void requiredNullAndUnknownPatchFieldsAreRejected() throws Exception {
        for(String body:new String[]{"{\"displayName\":null}","{\"categoryId\":null}","{\"status\":null}",
                "{\"contacts\":[]}","{\"unrecognized\":true}","{\"displayName\":\" \"}"}) {
            json(patch(instPath()),body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status",is(400)));
        }
        mvc.perform(get(instPath())).andExpect(jsonPath("$.displayName",is("Alpha Teste")));
    }

    @Test void institutionValidationCoversDocumentsUrlsAndReferences() throws Exception {
        json(post("/api/institutions"),institutionRequest().replace("12345678000195","00000000000000"))
            .andExpect(status().isBadRequest());
        for(String url:new String[]{"javascript:alert(1)","ftp://example.org","https://user:secret@example.org"}) {
            json(patch(instPath()),"{\"website\":\""+url+"\"}").andExpect(status().isBadRequest());
        }
        json(patch(instPath()),"{\"categoryId\":9223372036854775807}").andExpect(status().isNotFound());
        Long inactive=jdbc.queryForObject("SELECT id FROM institution_categories WHERE name='Outros'",Long.class);
        json(patch(instPath()),"{\"categoryId\":"+inactive+"}").andExpect(status().isBadRequest());
    }

    @Test void duplicateCnpjReturns409WithoutSqlDetails() throws Exception {
        json(post("/api/institutions"),institutionRequest().replace("12345678000195","11222333000181"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message",not(containsString("INSERT"))))
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test void unitCreateReplaceAndPatchPreserveAddressAndChildren() throws Exception {
        String result=json(post(instPath()+"/units"),unitRequest()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.address.city",is("Luz"))).andReturn().getResponse().getContentAsString();
        json(put("/api/units/"+id(result)),unitRequest().replace("Nova Unidade Teste","Editada Teste"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name",is("Editada Teste")));
        json(patch(unitPath()),"{\"description\":\"Descrição nova\"}").andExpect(status().isOk())
            .andExpect(jsonPath("$.address.street",is("Rua de Teste")))
            .andExpect(jsonPath("$.address.postalCode",is("35595000")))
            .andExpect(jsonPath("$.contacts",hasSize(2))).andExpect(jsonPath("$.needs",hasSize(4)));
        json(patch(unitPath()),"{\"latitude\":-19.791,\"longitude\":-45.680}").andExpect(status().isOk());
        json(patch(unitPath()),"{\"latitude\":null,\"longitude\":null}").andExpect(status().isOk())
            .andExpect(jsonPath("$.latitude",nullValue()));
    }

    @Test void unitValidatesCoordinatePairsPrecisionPostalCodeAndCity() throws Exception {
        for(String body:new String[]{"{\"latitude\":10}","{\"latitude\":91,\"longitude\":10}",
                "{\"latitude\":10,\"longitude\":181}","{\"latitude\":1.123456789,\"longitude\":10}",
                "{\"postalCode\":\"35595-000\"}","{\"name\":null}","{\"document\":\"123\"}"}) {
            json(patch(unitPath()),body).andExpect(status().isBadRequest());
        }
        json(patch(unitPath()),"{\"cityId\":9223372036854775807}").andExpect(status().isNotFound());
        json(post("/api/institutions/9223372036854775807/units"),unitRequest()).andExpect(status().isNotFound());
    }

    @Test void contactsSupportCreatePatchReplaceDeleteAndParentScope() throws Exception {
        for(String parent:new String[]{instPath(),unitPath()}) {
            String result=json(post(parent+"/contacts"),contact(false,"created@example.org"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String path=parent+"/contacts/"+id(result);
            json(patch(path),"{\"isPublic\":true,\"label\":null}").andExpect(status().isOk())
                .andExpect(jsonPath("$.value",is("created@example.org"))).andExpect(jsonPath("$.label",nullValue()));
            json(put(path),contact(false,"replaced@example.org")).andExpect(status().isOk());
            mvc.perform(delete(path)).andExpect(status().isNoContent());
            mvc.perform(get(path.replace("/api/","/api/management/"))).andExpect(status().isNotFound());
        }
        Long foreign=jdbc.queryForObject("SELECT id FROM institution_contacts WHERE value='public@example.org'",Long.class);
        json(patch("/api/institutions/"+institution("Beta Teste")+"/contacts/"+foreign),"{\"label\":\"x\"}")
            .andExpect(status().isNotFound());
    }

    @Test void contactValuesAreValidated() throws Exception {
        for(String value:new String[]{"invalid-email",""}) {
            json(post(unitPath()+"/contacts"),contact(true,value)).andExpect(status().isBadRequest());
        }
        String phone=contact(true,"3133334444").replace("EMAIL","PHONE");
        json(post(unitPath()+"/contacts"),phone).andExpect(status().isCreated());
        json(post(unitPath()+"/contacts"),phone.replace("3133334444","+553133334444").replace("PHONE","WHATSAPP"))
            .andExpect(status().isCreated());
        json(post(unitPath()+"/contacts"),phone.replace("3133334444","letters"))
            .andExpect(status().isBadRequest());
    }

    @Test void duplicateContactReturns409() throws Exception {
        json(post(instPath()+"/contacts"),contact(true,"public@example.org")).andExpect(status().isConflict());
    }

    @Test void needLifecyclePreservesPartialFieldsAndRestrictsDeletion() throws Exception {
        String result=json(post(unitPath()+"/needs"),needRequest()).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String path=unitPath()+"/needs/"+id(result);
        json(patch(path),"{\"quantity\":null}").andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity",nullValue())).andExpect(jsonPath("$.name",is("Nova necessidade teste")));
        mvc.perform(delete(path)).andExpect(status().isConflict());
        json(put(path),needRequest()).andExpect(status().isOk());
        json(patch(path),"{\"status\":\"FULFILLED\"}").andExpect(status().isOk());
        mvc.perform(get(unitPath())).andExpect(jsonPath("$.needs[*].id",not(hasItem(id(result).intValue()))));
        json(patch(path),"{\"status\":\"INACTIVE\"}").andExpect(status().isOk());
        mvc.perform(delete(path)).andExpect(status().isNoContent());
    }

    @Test void needsValidateQuantityEnumsAndCategory() throws Exception {
        for(String body:new String[]{needRequest().replace("2.5","-1"),needRequest().replace("2.5","1.234"),
                needRequest().replace("HIGH","URGENT"),needRequest().replace("ACTIVE","UNKNOWN")}) {
            json(post(unitPath()+"/needs"),body).andExpect(status().isBadRequest());
        }
        json(post(unitPath()+"/needs"),needRequest().replace("\"categoryId\":"+needCategory(),"\"categoryId\":9223372036854775807"))
            .andExpect(status().isNotFound());
    }

    @Test void hoursSupportAllSevenDaysAndUpsertWithoutDuplicate() throws Exception {
        for(int day=1;day<=7;day++) {
            json(put(unitPath()+"/opening-hours/"+day),"{\"closed\":false,\"opensAt\":\"08:00:00\",\"closesAt\":\"18:00:00\"}")
                .andExpect(status().isOk());
        }
        Long old=jdbc.queryForObject("SELECT id FROM unit_opening_hours WHERE unit_id=? AND day_of_week=1",Long.class,unit());
        json(put(unitPath()+"/opening-hours/1"),"{\"closed\":true,\"note\":\"Feriado\"}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.id",is(old.intValue())))
            .andExpect(jsonPath("$.opensAt",nullValue()));
        mvc.perform(get("/api/management/units/"+unit()+"/opening-hours"))
            .andExpect(jsonPath("$[*].dayOfWeek",contains(1,2,3,4,5,6,7)));
        mvc.perform(get(unitPath())).andExpect(jsonPath("$.openingHours",hasSize(7)));
    }

    @Test void hoursRejectInvalidDaysAndTimeCombinations() throws Exception {
        for(String body:new String[]{"{\"closed\":false}","{\"closed\":true,\"opensAt\":\"08:00\"}",
                "{\"closed\":false,\"opensAt\":\"18:00\",\"closesAt\":\"08:00\"}",
                "{\"closed\":false,\"opensAt\":\"08:00\",\"closesAt\":\"08:00\"}"}) {
            json(put(unitPath()+"/opening-hours/1"),body).andExpect(status().isBadRequest());
        }
        for(int day:new int[]{0,8}) json(put(unitPath()+"/opening-hours/"+day),"{\"closed\":true}")
            .andExpect(status().isBadRequest());
    }

    @Test void serviceAreasReplaceAssociationsAtomicallyAndCanBeCleared() throws Exception {
        Long area=jdbc.queryForObject("SELECT id FROM service_areas WHERE name='Educação'",Long.class);
        for(int iteration=0;iteration<2;iteration++) {
            json(put(unitPath()+"/service-areas"),"{\"serviceAreaIds\":["+area+"]}").andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name",contains("Educação")));
        }
        json(put(unitPath()+"/service-areas"),"{\"serviceAreaIds\":[9223372036854775807]}").andExpect(status().isNotFound());
        mvc.perform(get(unitPath())).andExpect(jsonPath("$.serviceAreas[*].name",contains("Educação")));
        json(put(unitPath()+"/service-areas"),"{\"serviceAreaIds\":[]}").andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void historyWorksWithoutImagesAndReferencesHaveFullLifecycle() throws Exception {
        mvc.perform(get(instPath())).andExpect(jsonPath("$.history",is("História sem fotos")))
            .andExpect(jsonPath("$.historyImages",empty()));
        String body="{\"imageUrl\":\"https://example.org/photo.jpg\",\"altText\":\"Foto de teste\",\"caption\":\"Legenda\",\"displayOrder\":2}";
        String result=json(post(instPath()+"/history-images"),body).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String path=instPath()+"/history-images/"+id(result);
        json(patch(path),"{\"caption\":null,\"displayOrder\":0}").andExpect(status().isOk())
            .andExpect(jsonPath("$.url",is("https://example.org/photo.jpg"))).andExpect(jsonPath("$.caption",nullValue()));
        json(put(path),body).andExpect(status().isOk());
        json(post(instPath()+"/history-images"),body.replace("photo.jpg","first.jpg").replace(":2}",":1}"))
            .andExpect(status().isCreated());
        mvc.perform(get(instPath())).andExpect(jsonPath("$.historyImages[*].order",contains(1,2)))
            .andExpect(jsonPath("$.historyImages[0].alt",is("Foto de teste")));
        mvc.perform(delete(path)).andExpect(status().isNoContent());
        mvc.perform(get(instPath())).andExpect(jsonPath("$.historyImages",hasSize(1)));
    }

    @Test void invalidImageMetadataAndUrlsAreRejected() throws Exception {
        for(String body:new String[]{"{\"imageUrl\":\"javascript:alert(1)\",\"altText\":\"Foto\",\"displayOrder\":0}",
                "{\"imageUrl\":\"https://example.org/x.jpg\",\"altText\":\"\",\"displayOrder\":0}",
                "{\"imageUrl\":\"https://example.org/x.jpg\",\"altText\":\"Foto\",\"displayOrder\":-1}"}) {
            json(post(instPath()+"/history-images"),body).andExpect(status().isBadRequest());
        }
    }

    @Test void invalidPaginationAndMalformedJsonAre400() throws Exception {
        mvc.perform(get("/api/institutions").param("size","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/institutions").param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/institutions").param("cityId","0")).andExpect(status().isBadRequest());
        json(patch(instPath()),"{").andExpect(status().isBadRequest());
        json(patch(instPath()),"{\"status\":0}").andExpect(status().isBadRequest());
        json(post("/api/institutions"),institutionRequest().replace("\"status\":\"ACTIVE\"","\"status\":\"ACTIVE\",\"unknown\":true"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/institutions").contentType("text/plain").content("invalid"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status",is(415)));
    }

    @Test void inactiveTaxonomiesCannotBeNewlyAssociated() throws Exception {
        Long inactive=jdbc.queryForObject("SELECT id FROM service_areas WHERE name='Outros'",Long.class);
        json(put(unitPath()+"/service-areas"),"{\"serviceAreaIds\":["+inactive+"]}").andExpect(status().isBadRequest());
        Long inactiveNeed=jdbc.queryForObject("SELECT id FROM need_categories WHERE name='Outros'",Long.class);
        json(post(unitPath()+"/needs"),needRequest().replace("\"categoryId\":"+needCategory(),"\"categoryId\":"+inactiveNeed))
            .andExpect(status().isBadRequest());
    }

    @Test void suspendedInstitutionsAreUnavailablePublicly() throws Exception {
        json(patch(instPath()),"{\"status\":\"SUSPENDED\"}").andExpect(status().isOk());
        mvc.perform(get(instPath())).andExpect(status().isNotFound());
        mvc.perform(get(unitPath())).andExpect(status().isNotFound());
        mvc.perform(get("/api/cities/available")).andExpect(content().json("[]"));
    }

    @Test void aggregateQueriesDoNotGrowWithUnitCount() throws Exception {
        var statistics=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        em.flush();em.clear();statistics.clear();
        mvc.perform(get(instPath())).andExpect(status().isOk());
        long initial=statistics.getPrepareStatementCount();
        for(int index=0;index<6;index++) json(post(instPath()+"/units"),unitRequest()).andExpect(status().isCreated());
        em.flush();em.clear();statistics.clear();
        mvc.perform(get(instPath())).andExpect(status().isOk()).andExpect(jsonPath("$.units",hasSize(8)));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(initial).isLessThanOrEqualTo(9);
    }
}
