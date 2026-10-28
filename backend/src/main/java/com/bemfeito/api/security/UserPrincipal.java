package com.bemfeito.api.security;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.bemfeito.api.user.entity.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
public final class UserPrincipal implements UserDetails,CredentialsContainer {
    private final Long id;
    private final String email;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;
    private String password;
    public UserPrincipal(User user) {
        id=user.getId();email=user.getEmail();password=user.getPasswordHash();enabled=user.getStatus()==UserStatus.ACTIVE;
        authorities=List.of(new SimpleGrantedAuthority("ROLE_"+user.getPlatformRole().name()));
    }
    public Long getId() { return id; }
    public String getUsername() { return email; }
    @JsonIgnore public String getPassword() { return password; }
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    public boolean isEnabled() { return enabled; }
    public boolean isAccountNonExpired() { return true; }
    public boolean isAccountNonLocked() { return enabled; }
    public boolean isCredentialsNonExpired() { return true; }
    public void eraseCredentials() { password=null; }
}
