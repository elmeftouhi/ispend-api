package org.example.expenseapi.service;

import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserStatus;
import org.example.expenseapi.repository.UserRepository;
import org.example.expenseapi.security.JwtBlacklistService;
import org.example.expenseapi.service.impl.UserServiceImpl;
import org.example.expenseapi.testutil.UserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @InjectMocks
    private UserServiceImpl userService;

    private User existing;

    @BeforeEach
    void setUp() {
        existing = UserBuilder.aUser()
                .withFirstName("John")
                .withLastName("Doe")
                .withEmail("john.doe@example.com")
                .withPassword("old-hash")
                .withStatus(UserStatus.ACTIVE)
                .withId(1L)
                .build();
    }

    @Test
    void createUser_encodesPasswordAndSaves() {
        User toCreate = UserBuilder.aUser()
                .withFirstName("Alice")
                .withLastName("Smith")
                .withEmail("alice@example.com")
                .withPassword("plainpass")
                .withStatus(UserStatus.ACTIVE)
                .build();

        when(passwordEncoder.encode("plainpass")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        User created = userService.createUser(toCreate);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("encoded-pass", created.getPassword());
        verify(passwordEncoder).encode("plainpass");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_updatesFieldsAndEncodesPassword() {
        // build an updates object and null-out fields we don't want to overwrite
        User updates = UserBuilder.aUser()
                .withFirstName("Johnny")
                .withPassword("newpass")
                .build();
        updates.setEmail(null);
        updates.setLastname(null);
        updates.setStatus(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpass")).thenReturn("new-encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(1L, updates);

        assertEquals("Johnny", updated.getFirstname());
        assertEquals("new-encoded", updated.getPassword());
        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode("newpass");
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_deactivate_shouldRevokeTokens() {
        User updates = new User();
        updates.setStatus(UserStatus.INACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(1L, updates);

        assertEquals(UserStatus.INACTIVE, updated.getStatus());
        verify(jwtBlacklistService, times(1)).revokeTokensForUser(existing.getEmail());
    }

    @Test
    void deleteUser_existing_deletes() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> userService.deleteUser(1L));
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_nonExisting_throws() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void findById_and_findAll_and_findByEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findAll()).thenReturn(List.of(existing));
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(existing));

        Optional<User> byId = userService.findById(1L);
        List<User> all = userService.findAll();
        Optional<User> byEmail = userService.findByEmail("john.doe@example.com");

        assertTrue(byId.isPresent());
        assertEquals(existing.getEmail(), byId.get().getEmail());

        assertEquals(1, all.size());
        assertEquals(existing.getEmail(), all.get(0).getEmail());

        assertTrue(byEmail.isPresent());
        assertEquals(existing.getEmail(), byEmail.get().getEmail());

        verify(userRepository).findById(1L);
        verify(userRepository).findAll();
        verify(userRepository).findByEmail("john.doe@example.com");
    }
}
