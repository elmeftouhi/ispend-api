package org.example.expenseapi.security;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory blacklist for JWTs. Stores token -> expiryMillis.
 * Cleans up expired entries periodically.
 *
 * Also tracks active issued tokens per username so we can revoke all tokens for a user
 * when their account is deactivated.
 */
@Component
public class JwtBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();
    // Track tokens that were issued (token -> expiry)
    private final Map<String, Long> activeTokens = new ConcurrentHashMap<>();
    // Map token -> username for quick removal
    private final Map<String, String> tokenToUser = new ConcurrentHashMap<>();
    // Map username -> set of tokens
    private final Map<String, Set<String>> userTokens = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // Run cleanup every minute
        cleaner.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        cleaner.shutdownNow();
    }

    /**
     * Register an issued token for a given username so it can be revoked later.
     * Does not blacklist the token.
     */
    public void registerTokenForUser(String username, String token, long expiryMillis) {
        if (token == null || username == null) return;
        if (expiryMillis <= System.currentTimeMillis()) return;
        activeTokens.put(token, expiryMillis);
        tokenToUser.put(token, username);
        userTokens.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(token);
    }

    /**
     * Blacklist a single token until its expiryMillis. Also remove it from the active tracking maps.
     */
    public void blacklistToken(String token, long expiryMillis) {
        if (token == null) return;
        // Only store if expiry is in the future
        if (expiryMillis <= System.currentTimeMillis()) {
            // remove any tracking since it's already expired
            removeTrackingForToken(token);
            return;
        }
        blacklist.put(token, expiryMillis);
        // Remove from active tracking
        removeTrackingForToken(token);
    }

    /**
     * Revoke (blacklist) all currently tracked tokens for the given username.
     */
    public void revokeTokensForUser(String username) {
        if (username == null) return;
        Set<String> tokens = userTokens.get(username);
        if (tokens == null || tokens.isEmpty()) return;
        // We operate on a copy to avoid concurrent modification
        for (String token : tokens.toArray(new String[0])) {
            Long exp = activeTokens.get(token);
            long expiry = exp != null ? exp : System.currentTimeMillis() + 1L;
            blacklistToken(token, expiry);
        }
        // remove the user's entry
        userTokens.remove(username);
    }

    /**
     * Returns true if token is blacklisted.
     */
    public boolean isBlacklisted(String token) {
        if (token == null) return false;
        Long exp = blacklist.get(token);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            // expired entry - remove and treat as not blacklisted
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        // Cleanup blacklist
        Iterator<Map.Entry<String, Long>> it = blacklist.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() < now) {
                it.remove();
            }
        }
        // Cleanup activeTokens and userTokens/tokenToUser
        Iterator<Map.Entry<String, Long>> ait = activeTokens.entrySet().iterator();
        while (ait.hasNext()) {
            Map.Entry<String, Long> e = ait.next();
            if (e.getValue() < now) {
                String token = e.getKey();
                ait.remove();
                String username = tokenToUser.remove(token);
                if (username != null) {
                    Set<String> set = userTokens.get(username);
                    if (set != null) {
                        set.remove(token);
                        if (set.isEmpty()) {
                            userTokens.remove(username);
                        }
                    }
                }
            }
        }
    }

    private void removeTrackingForToken(String token) {
        activeTokens.remove(token);
        String username = tokenToUser.remove(token);
        if (username != null) {
            Set<String> set = userTokens.get(username);
            if (set != null) {
                set.remove(token);
                if (set.isEmpty()) {
                    userTokens.remove(username);
                }
            }
        }
    }
}
