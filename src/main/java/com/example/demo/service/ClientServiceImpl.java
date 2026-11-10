package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.events.HotelNotificationListener.ClientRegisteredEvent;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ReservationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.errors.NotFoundException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository repository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

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
        boolean isNewClient = client.getId() == null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isNewClient) {
            client.setRole(admin && isValidRole(client.getRole()) ? client.getRole().trim().toUpperCase() : "CLIENT");
        } else {
            Client existing = findById(client.getId());
            if (client.getPassword() == null || client.getPassword().isBlank()) {
                client.setPassword(existing.getPassword());
            }
            if (client.getAvatarUrl() == null || client.getAvatarUrl().isBlank()) {
                client.setAvatarUrl(existing.getAvatarUrl());
            }
            if (client.getRole() == null || client.getRole().isBlank()) {
                client.setRole(existing.getRole());
            }
            if (!admin) {
                client.setRole(existing.getRole());
            } else if (!isValidRole(client.getRole())) {
                throw new IllegalArgumentException("Rol inválido. Use ADMIN, OPERATOR u OPERARIO.");
            } else {
                client.setRole(normalizeRole(client.getRole()));
            }
            if (client.getUsername() == null || client.getUsername().isBlank()) {
                client.setUsername(existing.getUsername());
            }
            if (client.getEmail() == null || client.getEmail().isBlank()) {
                client.setEmail(existing.getEmail());
            }
            if (client.getFirstName() == null || client.getFirstName().isBlank()) {
                client.setFirstName(existing.getFirstName());
            }
            if (client.getLastName() == null || client.getLastName().isBlank()) {
                client.setLastName(existing.getLastName());
            }
            if (client.getPhone() == null || client.getPhone().isBlank()) {
                client.setPhone(existing.getPhone());
            }
            if (client.getGoogleSubject() == null) {
                client.setGoogleSubject(existing.getGoogleSubject());
            }
            if (client.getAuthProvider() == null || client.getAuthProvider().isBlank()) {
                client.setAuthProvider(existing.getAuthProvider());
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
        boolean googleAccount = client.getGoogleSubject() != null && !client.getGoogleSubject().isBlank();
        if (client.getId() == null && googleAccount) {
            throw new IllegalArgumentException("Las cuentas Google deben crearse mediante el flujo OAuth autenticado.");
        }
        if (client.getId() == null && !googleAccount && (client.getPassword() == null || client.getPassword().isBlank())) {
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
        Client saved = repository.save(client);
        if (isNewClient && eventPublisher != null && "CLIENT".equalsIgnoreCase(saved.getRole())) {
            eventPublisher.publishEvent(new ClientRegisteredEvent(saved));
        }
        return saved;

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

    private boolean isValidRole(String role) {
        if (role == null) return false;
        String normalized = role.trim().toUpperCase();
        return normalized.equals("ADMIN") || normalized.equals("OPERATOR")
                || normalized.equals("OPERARIO") || normalized.equals("CLIENT");
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if ("OPERARIO".equals(normalized)) return "OPERATOR";
        return normalized;
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
                .filter(c -> c.getPassword() != null && passTrimmed.equals(c.getPassword().trim()))
                .orElse(null);
    }
}
