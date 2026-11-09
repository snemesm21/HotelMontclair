package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAccountService {

    private final ClientRepository clientRepository;

    public GoogleAccountService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client findOrCreate(GoogleTokenValidator.GoogleUserInfo googleUser) {
        if (googleUser.subject() == null || googleUser.subject().isBlank()) {
            throw new IllegalArgumentException("Google no proporcionó un identificador válido.");
        }

        var linked = clientRepository.findByGoogleSubject(googleUser.subject());
        if (linked.isPresent()) {
            Client client = linked.get();
            if (!"CLIENT".equalsIgnoreCase(client.getRole())) {
                throw new IllegalArgumentException("Esta cuenta Google no tiene permisos de cliente.");
            }
            return client;
        }

        var emailAccount = clientRepository.findByEmailIgnoreCase(googleUser.email());
        if (emailAccount.isPresent()) {
            throw new IllegalArgumentException(
                    "El correo ya pertenece a una cuenta local. Vincula la cuenta desde un procedimiento autenticado.");
        }

        String usernameBase = googleUser.email().substring(0, googleUser.email().indexOf('@'))
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        if (usernameBase.isBlank()) {
            usernameBase = "google";
        }
        String username = usernameBase;
        int suffix = 1;
        while (clientRepository.findByUsernameIgnoreCase(username).isPresent()) {
            username = usernameBase + "_" + suffix++;
        }

        Client client = Client.builder()
                .username(username)
                .password(null)
                .email(googleUser.email())
                .firstName(googleUser.firstName())
                .lastName(googleUser.lastName())
                .avatarUrl(googleUser.avatarUrl())
                .role("CLIENT")
                .googleSubject(googleUser.subject())
                .authProvider("GOOGLE")
                .build();
        return clientRepository.save(client);
    }

    public Client findOrCreateFromId(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta autenticada ya no existe."));
    }
}
