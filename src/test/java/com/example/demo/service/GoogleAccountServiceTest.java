package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleAccountServiceTest {

    private ClientRepository repository;
    private GoogleAccountService service;

    @BeforeEach
    void setUp() {
        repository = mock(ClientRepository.class);
        service = new GoogleAccountService(repository);
    }

    @Test
    void createsNewClientAccount() {
        when(repository.findByGoogleSubject("google-1")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase("nuevo@gmail.com")).thenReturn(Optional.empty());
        when(repository.findByUsernameIgnoreCase(any())).thenReturn(Optional.empty());
        when(repository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Client result = service.findOrCreate(new GoogleTokenValidator.GoogleUserInfo(
                "google-1", "nuevo@gmail.com", "Nuevo", "Usuario", "avatar"));

        assertEquals("CLIENT", result.getRole());
        assertEquals("GOOGLE", result.getAuthProvider());
        assertEquals("google-1", result.getGoogleSubject());
        assertEquals(null, result.getPassword());
    }

    @Test
    void returnsLinkedGoogleAccount() {
        Client linked = Client.builder()
                .id(5L).username("linked").email("linked@gmail.com")
                .role("CLIENT").authProvider("GOOGLE").googleSubject("google-5").build();
        when(repository.findByGoogleSubject("google-5")).thenReturn(Optional.of(linked));

        Client result = service.findOrCreate(new GoogleTokenValidator.GoogleUserInfo(
                "google-5", "linked@gmail.com", "Linked", "", null));

        assertEquals(5L, result.getId());
    }

    @Test
    void rejectsExistingLocalEmailWithoutAutomaticLink() {
        Client local = Client.builder().id(8L).email("local@gmail.com").role("CLIENT").authProvider("LOCAL").build();
        when(repository.findByGoogleSubject("google-8")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase("local@gmail.com")).thenReturn(Optional.of(local));

        assertThrows(IllegalArgumentException.class, () -> service.findOrCreate(
                new GoogleTokenValidator.GoogleUserInfo(
                        "google-8", "local@gmail.com", "Local", "Client", null)));
    }

    @Test
    void rejectsLinkedPrivilegedAccount() {
        Client admin = Client.builder()
                .id(1L).username("admin").role("ADMIN")
                .authProvider("GOOGLE").googleSubject("google-admin").build();
        when(repository.findByGoogleSubject("google-admin")).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () -> service.findOrCreate(
                new GoogleTokenValidator.GoogleUserInfo(
                        "google-admin", "admin@gmail.com", "Admin", "", null)));
    }
}
