package org.example.expenseapi.service;

import org.example.expenseapi.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createUser(User user);

    User updateUser(Long id, User user);

    void deleteUser(Long id);

    Optional<User> findById(Long id);

    List<User> findAll();

    Optional<User> findByEmail(String email);
}

