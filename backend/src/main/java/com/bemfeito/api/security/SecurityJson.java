package com.bemfeito.api.security;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import com.bemfeito.api.common.error.ApiError;
@Component
public class SecurityJson {
    private final ObjectMapper mapper;
    public SecurityJson(ObjectMapper mapper) { this.mapper=mapper; }
    public void write(HttpServletResponse response,int status,Object body) throws IOException {
        response.setStatus(status);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(),body);
    }
    public void error(HttpServletResponse response,int status,String message) throws IOException {
        write(response,status,new ApiError(Instant.now(),status,message,Map.of()));
    }
}
