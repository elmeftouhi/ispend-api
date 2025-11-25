package org.example.expenseapi.application.service;

import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserSettings;
import org.example.expenseapi.repository.UserRepository;
import org.example.expenseapi.repository.UserSettingsRepository;
import org.example.expenseapi.service.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserSettingsApplicationServiceImpl implements UserSettingsApplicationService {

    private final UserSettingsRepository repository;
    private final UserRepository userRepository;

    public UserSettingsApplicationServiceImpl(UserSettingsRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSettings> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public UserSettings createOrUpdate(Long userId, UserSettings settings) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

        Optional<UserSettings> existing = repository.findByUserId(userId);
        if (existing.isPresent()) {
            UserSettings s = existing.get();
            if (settings.getCurrency() != null) s.setCurrency(settings.getCurrency());
            if (settings.getDecimalDigits() != null) s.setDecimalDigits(settings.getDecimalDigits());
            if (settings.getWeekStart() != null) s.setWeekStart(settings.getWeekStart());
            if (settings.getCurrencySymbolPlacement() != null) s.setCurrencySymbolPlacement(settings.getCurrencySymbolPlacement());
            return repository.save(s);
        } else {
            settings.setUser(user);
            return repository.save(settings);
        }
    }
}
