package org.example.expenseapi.service.impl;

import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserStatus;
import org.example.expenseapi.repository.UserRepository;
import org.example.expenseapi.repository.TenantRepository;
import org.example.expenseapi.model.Tenant;
import org.example.expenseapi.security.JwtBlacklistService;
import org.example.expenseapi.service.UserNotFoundException;
import org.example.expenseapi.service.UserService;
import org.example.expenseapi.tenant.TenantContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtBlacklistService jwtBlacklistService;

    // Configurable default tenant id (optional). Set in application properties: app.default-tenant-id
    @Value("${app.default-tenant-id:}")
    private String defaultTenantId;

    public UserServiceImpl(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder, JwtBlacklistService jwtBlacklistService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    public User createUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Ensure tenantId is set: prefer explicitly provided value, otherwise use TenantContextHolder.
        if (user.getTenantId() == null || user.getTenantId().isBlank()) {
            String tenant = TenantContextHolder.getTenant();
            if (tenant != null && !tenant.isBlank()) {
                user.setTenantId(tenant);
            } else {
                // If no tenant context is available (e.g. initial signup or empty DB), automatically create a tenant
                // if none exist and assign the generated tenant id to the user.
                if (tenantRepository.count() == 0) {
                    String newTenantId = UUID.randomUUID().toString();
                    Tenant t = new Tenant(newTenantId, user.getFirstname() + "'s tenant");
                    tenantRepository.save(t);
                    user.setTenantId(newTenantId);
                } else {
                    // Previously we assigned the first tenant found; now prefer a configured default tenant id
                    if (defaultTenantId != null && !defaultTenantId.isBlank()) {
                        // Ensure default tenant exists (create if missing)
                        Optional<Tenant> def = tenantRepository.findById(defaultTenantId);
                        if (def.isPresent()) {
                            user.setTenantId(defaultTenantId);
                        } else {
                            Tenant t = new Tenant(defaultTenantId, "Default Tenant");
                            tenantRepository.save(t);
                            user.setTenantId(defaultTenantId);
                        }
                    } else {
                        // Fallback: use the first tenant if no default configured
                        Optional<Tenant> any = tenantRepository.findAll().stream().findFirst();
                        if (any.isPresent()) {
                            user.setTenantId(any.get().getId());
                        } else {
                            // Last fallback: generate a tenant id (shouldn't normally reach here)
                            user.setTenantId(UUID.randomUUID().toString());
                        }
                    }
                }
            }
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

        // Preserve existing tenant unless an explicit change is requested
        if (user.getTenantId() != null && !user.getTenantId().isBlank() && !user.getTenantId().equals(existing.getTenantId())) {
            existing.setTenantId(user.getTenantId());
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
