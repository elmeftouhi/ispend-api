package org.example.expenseapi.application.service;

import org.example.expenseapi.model.UserSettings;

import java.util.Optional;

public interface UserSettingsApplicationService {
    Optional<UserSettings> findByUserId(Long userId);
    UserSettings createOrUpdate(Long userId, UserSettings settings);
}

