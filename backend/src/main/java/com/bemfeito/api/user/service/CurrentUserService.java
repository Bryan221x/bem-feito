package com.bemfeito.api.user.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import com.bemfeito.api.security.UserPrincipal;
import com.bemfeito.api.user.dto.UserResponse;
import com.bemfeito.api.user.repository.UserRepository;
@Service @Transactional(readOnly=true)
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users=users; }
    @PreAuthorize("isAuthenticated()")
    public UserResponse current() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !(auth.getPrincipal() instanceof UserPrincipal principal))
            throw new AuthenticationCredentialsNotFoundException("Autenticação necessária");
        return users.findById(principal.getId()).map(UserResponse::from)
            .orElseThrow(()->new AuthenticationCredentialsNotFoundException("Autenticação necessária"));
    }
}
