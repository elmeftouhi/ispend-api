package org.example.expenseapi.service.impl;

import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserStatus;
import org.example.expenseapi.repository.UserRepository;
import org.example.expenseapi.security.JwtBlacklistService;
import org.example.expenseapi.service.UserNotFoundException;
import org.example.expenseapi.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtBlacklistService jwtBlacklistService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtBlacklistService jwtBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    public User createUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User user) {
        User existing = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        UserStatus previousStatus = existing.getStatus();
        if (user.getFirstname() != null) existing.setFirstname(user.getFirstname());
        if (user.getLastname() != null) existing.setLastname(user.getLastname());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        User saved = userRepository.save(existing);

        // If user was active and now set to inactive, revoke issued tokens for that user
        if (previousStatus == UserStatus.ACTIVE && saved.getStatus() == UserStatus.INACTIVE) {
            if (jwtBlacklistService != null && saved.getEmail() != null) {
                jwtBlacklistService.revokeTokensForUser(saved.getEmail());
            }
        }

        return saved;
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
