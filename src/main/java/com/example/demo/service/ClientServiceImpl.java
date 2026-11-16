package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.entities.UserEntity;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.security.CustomUserDetailsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.errors.NotFoundException;
import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository repository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired 
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    public List<Client> findAll() {
        return repository.findAll();
    }

    @Override
    public Client findById(Long id) {
        Client client = repository.findById(id).orElseThrow(() -> new NotFoundException(id));
        return client;
    }

    @Override
    @Transactional
    public Client save(Client client) {
        Client current = null;
        if (client.getId() == null) {
            if (client.getRole() == null || client.getRole().isBlank()) {
                client.setRole("CLIENT");
            }
        } else {
            current = findById(client.getId());
            
            if (client.getAvatarUrl() == null || client.getAvatarUrl().isBlank()) {
                client.setAvatarUrl(current.getAvatarUrl());
            }
            if (client.getRole() == null || client.getRole().isBlank()) {
                client.setRole(current.getRole());
            }
            if (client.getUsername() == null || client.getUsername().isBlank()) {
                client.setUsername(current.getUsername());
            }
            if (client.getEmail() == null || client.getEmail().isBlank()) {
                client.setEmail(current.getEmail());
            }
            if (client.getFirstName() == null || client.getFirstName().isBlank()) {
                client.setFirstName(current.getFirstName());
            }
            if (client.getLastName() == null || client.getLastName().isBlank()) {
                client.setLastName(current.getLastName());
            }
            if (client.getPhone() == null || client.getPhone().isBlank()) {
                client.setPhone(current.getPhone());
            }
        }
        // Validar campos obligatorios
        if (client.getUsername() == null || client.getUsername().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }
        if (client.getFirstName() == null || client.getFirstName().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (client.getId() == null && (client.getPassword() == null || client.getPassword().isBlank())) {
            throw new IllegalArgumentException("La contraseña es obligatoria para nuevos clientes.");
        }
        // Validar duplicados de username y email
        repository.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                client.getUsername().trim(), client.getEmail().trim())
            .ifPresent(existing -> {
                if (!existing.getId().equals(client.getId())) {
                    boolean usernameDup = existing.getUsername().equalsIgnoreCase(client.getUsername().trim());
                    boolean emailDup = existing.getEmail().equalsIgnoreCase(client.getEmail().trim());
                    if (usernameDup && emailDup) {
                        throw new IllegalArgumentException("El nombre de usuario y el correo electrónico ya están en uso por otro cliente.");
                    } else if (usernameDup) {
                        throw new IllegalArgumentException("Ya existe un cliente con el nombre de usuario '" + client.getUsername().trim() + "'.");
                    } else {
                        throw new IllegalArgumentException("Ya existe un cliente con el correo '" + client.getEmail().trim() + "'.");
                    }
                }
            });
        
            if(current == null) {
                client.setUser(userDetailsService.clientToUser(client));
            } else {
                UserEntity user = current.getUser();
                user.setUsername(client.getUsername().trim());
                if (client.getPassword() != null && !client.getPassword().isBlank()) {
                    user.setPassword(passwordEncoder.encode(client.getPassword().trim()));
                }
                user.setRoles(userDetailsService.rolesFor(client));
                client.setUser(user);
            }
            return repository.save(client);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Client client = repository.findById(id).orElseThrow(() -> new NotFoundException(id));

        if (reservationRepository.existsByClientId(id)) {
            throw new IllegalArgumentException(
                "No se puede eliminar el cliente porque tiene reservas asociadas. Cancela o elimina primero sus reservas.");
        }
        repository.deleteById(id);
    }

    @Override
    public Client login(String username, String email, String password) {
        String identifier = null;
        if (username != null && !username.trim().isEmpty()) {
            identifier = username.trim();
        } else if (email != null && !email.trim().isEmpty()) {
            identifier = email.trim();
        }

        if (identifier == null || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Por favor ingresa tu correo/usuario y tu contraseña.");
        }

        String passTrimmed = password.trim();
        return repository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .filter(c -> c.getUser() != null
                    && passwordEncoder.matches(passTrimmed, c.getUser().getPassword()))
                .orElse(null);
    }

    @Override
    public Client findByUsername(String username) {
        return repository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username).orElse(null);
    }
}
