package org.example.expenseapi.application.service;

import org.example.expenseapi.model.User;

import java.util.List;
import java.util.Optional;

public interface UserApplicationService {
    User createUser(User user);
    Optional<User> findById(Long id);
    List<User> findAll();
    User updateUser(Long id, User user);
    void deleteUser(Long id);
    Optional<User> findByEmail(String email);
}

