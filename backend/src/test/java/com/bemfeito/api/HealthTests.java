package com.bemfeito.api;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.bemfeito.api.common.controller.HealthController;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class HealthTests {
    @Test void databaseFailureMakesReadinessUnavailableWithoutLeakingDetails() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private detail"));
        assertThatThrownBy(() -> new HealthController(jdbc).health())
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .hasMessageContaining("503").hasMessageNotContaining("private detail");
    }
    @Test void healthyDatabasePreservesPublicContract() {
        var jdbc = mock(JdbcTemplate.class); when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        assertThat(new HealthController(jdbc).health()).containsEntry("status", "UP");
    }
}
