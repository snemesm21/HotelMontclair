package com.example.demo.security;

import com.example.demo.entities.Client;
import com.example.demo.entities.Role;
import com.example.demo.entities.UserEntity;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userDb = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return new User(userDb.getUsername(), userDb.getPassword(), mapToAuthorities(userDb.getRoles()));
    }

    private Collection<? extends GrantedAuthority> mapToAuthorities(List<Role> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role r : roles) {
            authorities.add(new SimpleGrantedAuthority(r.getName()));
            authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getName()));
        }
        return authorities;
    }

    public UserEntity clientToUser(Client client) {
        UserEntity user = new UserEntity();
        user.setUsername(client.getUsername().trim());
        String rawPassword = (client.getPassword() != null && !client.getPassword().isBlank())
                ? client.getPassword().trim()
                : UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(rolesFor(client));
        return user;
    }

    public List<Role> rolesFor(Client client) {
        String name = client.isAdmin() ? "ADMIN" : (client.isOperator() ? "OPERATOR" : "CLIENT");
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + name));
        return new ArrayList<>(List.of(role));
    }
}
