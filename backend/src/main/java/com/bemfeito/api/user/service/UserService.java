package com.bemfeito.api.user.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.*;
import com.bemfeito.api.security.UserPrincipal;
import com.bemfeito.api.user.entity.User;
import com.bemfeito.api.user.repository.UserRepository;
@Service @Transactional(readOnly=true)
public class UserService implements UserDetailsService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users=users; }
    public UserDetails loadUserByUsername(String email) {
        User user=users.findByEmail(User.normalizeEmail(email)).orElseThrow(()->new UsernameNotFoundException("Credenciais inválidas"));
        if(!user.hasUsablePasswordHash())
            throw new UsernameNotFoundException("Credenciais inválidas");
        return new UserPrincipal(user);
    }
}
