package org.example.expenseapi.application.service;

import org.example.expenseapi.model.User;
import org.example.expenseapi.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserApplicationServiceImpl implements UserApplicationService {

    private final UserService userService;

    public UserApplicationServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public User createUser(User user) {
        return userService.createUser(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userService.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userService.findAll();
    }

    @Override
    public User updateUser(Long id, User user) {
        return userService.updateUser(id, user);
    }

    @Override
    public void deleteUser(Long id) {
        userService.deleteUser(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userService.findByEmail(email);
    }
}

