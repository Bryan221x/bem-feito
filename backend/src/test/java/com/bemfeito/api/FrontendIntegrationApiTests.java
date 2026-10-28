package com.bemfeito.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import javax.imageio.ImageIO;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"app.upload.dir=target/test-history-uploads","app.upload.max-bytes=1024"})
@AutoConfigureMockMvc @Transactional @Sql("/institutional-fixtures.sql")
@WithMockUser(roles="PLATFORM_ADMIN")
class FrontendIntegrationApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;
    private long owner() { return jdbc.queryForObject("select id from institutions where display_name='Alpha Teste'",Long.class); }
    private MockMultipartFile image() throws Exception { var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);return new MockMultipartFile("file","photo.png","image/png",out.toByteArray()); }
    private String upload() { return "/api/institutions/"+owner()+"/history-images/upload"; }
    @Test void summaryUsesRealCounts() throws Exception {
        mvc.perform(get("/api/admin/summary")).andExpect(status().isOk()).andExpect(jsonPath("$.institutions",is(3))).andExpect(jsonPath("$.units",is(4))).andExpect(jsonPath("$.managers",is(0))).andExpect(jsonPath("$.institutionsByCity",hasSize(3)));
    }
    @Test void administrativeListHasCountsLegalDataAndPagination() throws Exception {
        mvc.perform(get("/api/admin/institutions").param("q","Alpha").param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].cnpj",is("11222333000181"))).andExpect(jsonPath("$.content[0].unitCount",is(2))).andExpect(jsonPath("$.content[0].responsibleCount",is(0))).andExpect(jsonPath("$.totalElements",is(1)));
        mvc.perform(get("/api/admin/institutions").param("status","INACTIVE")).andExpect(jsonPath("$.content[0].displayName",is("Beta Teste")));
        mvc.perform(get("/api/admin/institutions").param("q","%")).andExpect(jsonPath("$.content",empty()));
    }
    @Test void invalidPaginationIsRejected() throws Exception {
        mvc.perform(get("/api/admin/institutions").param("size","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/units/map").param("page","-1")).andExpect(status().isBadRequest());
    }
    @Test @WithMockUser void nonAdminCannotReadSummaryOrAdminList() throws Exception {
        mvc.perform(get("/api/admin/summary")).andExpect(status().isForbidden());mvc.perform(get("/api/admin/institutions")).andExpect(status().isForbidden());
    }
    @Test void mapContainsOnlyPublishedUnitsWithRealCoordinates() throws Exception {
        mvc.perform(get("/api/units/map")).andExpect(jsonPath("$.content",empty()));
        jdbc.update("update units set latitude=-20,longitude=-45");
        mvc.perform(get("/api/units/map").param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements",is(2))).andExpect(jsonPath("$.content",hasSize(1))).andExpect(jsonPath("$.content[0].latitude",is(-20.0))).andExpect(jsonPath("$.content[0].contacts").doesNotExist());
    }
    @Test void uploadRequiresCsrf() throws Exception { mvc.perform(multipart(upload()).file(image()).param("altText","Descrição")).andExpect(status().isForbidden()); }
    @Test @WithMockUser void uploadRequiresInstitutionMembership() throws Exception { mvc.perform(multipart(upload()).file(image()).param("altText","Descrição").with(csrf())).andExpect(status().isForbidden()); }
    @Test void uploadRequiresAlternativeText() throws Exception { mvc.perform(multipart(upload()).file(image()).param("altText","").with(csrf())).andExpect(status().isBadRequest()); }
    @Test void uploadReturnsUsableResourceAndSupportsMetadataPatch() throws Exception {
        String body=mvc.perform(multipart(upload()).file(image()).param("altText","Descrição acessível").param("displayOrder","2").with(csrf())).andExpect(status().isCreated()).andExpect(jsonPath("$.order",is(2))).andReturn().getResponse().getContentAsString();
        String url=JsonPath.read(body,"$.url"); Number id=JsonPath.read(body,"$.id");
        mvc.perform(get(url).with(anonymous())).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(patch("/api/institutions/"+owner()+"/history-images/"+id).with(csrf()).contentType("application/json").content("{\"caption\":\"Nova legenda\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.url",is(url)));
        jdbc.update("update institutions set status='SUSPENDED' where id=?",owner()); em.clear();
        mvc.perform(get(url).with(anonymous())).andExpect(status().isNotFound());
    }
    @Test void deletedImageIsNotServedAndFileCleanupRunsAfterCommit() throws Exception {
        String body=mvc.perform(multipart(upload()).file(image()).param("altText","Foto").with(csrf())).andReturn().getResponse().getContentAsString();
        String url=JsonPath.read(body,"$.url"); Number id=JsonPath.read(body,"$.id");
        Path file=Path.of("target/test-history-uploads",url.substring("/api/history-media/".length()));assertThat(file).exists();
        mvc.perform(delete("/api/institutions/"+owner()+"/history-images/"+id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());assertThat(file).exists();
        org.springframework.transaction.support.TransactionSynchronizationUtils.triggerAfterCommit();assertThat(file).doesNotExist();
    }
    @Test void oversizedUploadAndWrongTypeAreRejected() throws Exception {
        mvc.perform(multipart(upload()).file(new MockMultipartFile("file",new byte[1025])).param("altText","Foto").with(csrf())).andExpect(status().isPayloadTooLarge());
        mvc.perform(multipart(upload()).file(new MockMultipartFile("file","file.png","image/png","<svg/>".getBytes())).param("altText","Foto").with(csrf())).andExpect(status().isBadRequest());
    }
}
