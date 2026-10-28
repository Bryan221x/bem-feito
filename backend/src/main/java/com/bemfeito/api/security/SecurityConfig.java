package com.bemfeito.api.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.*;
import com.bemfeito.api.user.repository.UserRepository;
import com.bemfeito.api.user.dto.UserResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean org.springframework.web.filter.CorsFilter corsFilter(
            @Value("${app.cors.allowed-origins:http://127.0.0.1:5500,http://localhost:5500}") String origins,SecurityJson json) {
        var source=corsConfigurationSource(origins);
        var filter=new org.springframework.web.filter.CorsFilter(source);
        filter.setCorsProcessor(new DefaultCorsProcessor() {
            @Override protected void rejectRequest(org.springframework.http.server.ServerHttpResponse response) throws java.io.IOException {
                json.error(((org.springframework.http.server.ServletServerHttpResponse)response).getServletResponse(),
                    403,"Origem ou preflight CORS não permitido");
                response.flush();
            }
        });
        return filter;
    }
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.CorsFilter> corsRegistration(
            org.springframework.web.filter.CorsFilter filter) {
        var registration=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);return registration; // Executado apenas pela cadeia de segurança.
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource(String configuredOrigins) {
        var origins=Arrays.stream(configuredOrigins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList();
        for(String origin:origins) {
            var uri=java.net.URI.create(origin);
            if(origin.contains("*") || uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null
                    || uri.getFragment()!=null || (uri.getPath()!=null && !uri.getPath().isEmpty())
                    || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())))
                throw new IllegalStateException("Origin CORS inválida; use origens HTTP(S) exatas");
        }
        var config=new CorsConfiguration();config.setAllowedOrigins(origins);config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET","HEAD","OPTIONS","POST","PUT","PATCH","DELETE"));
        config.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN"));config.setMaxAge(3600L);
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",config);return source;
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,SecurityJson json,UserRepository users,org.springframework.web.filter.CorsFilter corsFilter) throws Exception {
        http.addFilterAt(corsFilter,org.springframework.web.filter.CorsFilter.class).csrf(csrf->{})
            .requestCache(cache->cache.disable())
            .authorizeHttpRequests(auth->auth
                .requestMatchers(HttpMethod.GET,"/api/health","/api/states","/api/cities","/api/cities/available",
                    "/api/institution-categories","/api/service-areas","/api/need-categories","/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/institutions","/api/institutions/{id}","/api/institutions/{id}/units","/api/units/{id}").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/institutions/access-search").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/registration-requests","/api/access-requests",
                    "/api/account-invitations/inspect","/api/account-invitations/accept").permitAll()
                .requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/institutions").hasRole("PLATFORM_ADMIN")
                .requestMatchers("/api/auth/**","/api/me/**","/api/management/**","/api/institutions/**","/api/units/**").authenticated()
                .anyRequest().hasRole("PLATFORM_ADMIN"))
            .exceptionHandling(errors->errors
                .authenticationEntryPoint((request,response,error)->json.error(response,401,"Autenticação necessária"))
                .accessDeniedHandler((request,response,error)->json.error(response,403,"Acesso negado ou token CSRF inválido")))
            .formLogin(login->login.loginPage("/api/auth/login").loginProcessingUrl("/api/auth/login").usernameParameter("email").passwordParameter("password")
                .authenticationDetailsSource(request->{
                    String password=request.getParameter("password");
                    if(password!=null && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
                        throw new org.springframework.security.authentication.BadCredentialsException("Credenciais inválidas");
                    return new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request);
                })
                .successHandler((request,response,auth)-> {
                    var principal=(UserPrincipal)auth.getPrincipal();
                    var user=users.findById(principal.getId()).orElseThrow();
                    json.write(response,200,java.util.Map.of("authenticated",true,"user",UserResponse.from(user)));
                })
                .failureHandler((request,response,error)->json.error(response,401,"E-mail ou senha inválidos.")))
            .logout(logout->logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).clearAuthentication(true)
                .deleteCookies("JSESSIONID").logoutSuccessHandler((request,response,auth)->json.write(response,200,java.util.Map.of("authenticated",false))))
            .sessionManagement(session->session.sessionFixation(fixation->fixation.changeSessionId()))
            .addFilterAfter(new CurrentAccountFilter(users),SecurityContextHolderFilter.class);
        return http.build();
    }
}
