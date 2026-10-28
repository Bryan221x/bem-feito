package com.bemfeito.api.security;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.bemfeito.api.user.entity.UserStatus;
import com.bemfeito.api.user.repository.UserRepository;
/** Revalida contas e roles em sessões existentes, sem guardar o hash na sessão. */
public class CurrentAccountFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public CurrentAccountFilter(UserRepository users) { this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication!=null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            var user=users.findById(principal.getId()).orElse(null);
            if(user==null || user.getStatus()!=UserStatus.ACTIVE || !user.hasUsablePasswordHash()) {
                SecurityContextHolder.clearContext();
                var session=request.getSession(false);if(session!=null) session.invalidate();
            } else {
                var refreshed=new UserPrincipal(user);refreshed.eraseCredentials();
                SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(refreshed,null,refreshed.getAuthorities()));
            }
        }
        chain.doFilter(request,response);
    }
}
