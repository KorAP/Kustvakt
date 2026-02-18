package de.ids_mannheim.korap.web.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/** Implemented with AI assistance
 * 
 * Simple in-memory rate limitation for authenticated users.
 * <p>
 * Keyed by bearer token (preferred) or username (fallback).
 * <p>
 * Note: In-memory means per-JVM only. For clustered deployments, use Redis/etc.
 */
@Component
@Priority(Priorities.AUTHORIZATION)
public class RateLimitFilter implements ContainerRequestFilter {

    private static final Logger jlog = LogManager
            .getLogger(RateLimitFilter.class);

    // Defaults: 60 requests per minute per key
    // Keep these conservative and easy to change later via config injection.
    private static final long REFILL_TOKENS = 60;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    public static final long BURST_CAPACITY = 60;

    /**
     * Prevent unbounded growth: keep at most this many distinct keys in-memory.
     */
    private static final int MAX_BUCKETS = 10_000;

    /**
     * Evict buckets that haven't been seen for this long.
     */
    private static final Duration BUCKET_TTL = Duration.ofHours(6);

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    @Override
    public void filter (ContainerRequestContext request) {
        // Only apply to authenticated requests
        if (request.getSecurityContext() == null
                || request.getSecurityContext().getUserPrincipal() == null) {
            return;
        }

        String key = resolveKey(request);
        if (key == null) {
            return;
        }

        long now = System.currentTimeMillis();

        // Opportunistic cleanup to avoid memory growth.
        // Do it only on inserts or if we grow too large.
        if (buckets.size() > MAX_BUCKETS) {
            cleanupOldEntries(now);
        }

        BucketEntry entry = buckets.compute(key, (k, existing) -> {
            if (existing == null) {
                // If we're still too large, try another cleanup pass before adding.
                if (buckets.size() > MAX_BUCKETS) {
                    cleanupOldEntries(now);
                }
                return new BucketEntry(new TokenBucket(BURST_CAPACITY,
                        REFILL_TOKENS, REFILL_PERIOD.toMillis()), now);
            }
            existing.lastSeenAtMillis = now;
            return existing;
        });

        if (!entry.bucket.tryConsume(1)) {
            long retryAfterSeconds = Math
                    .max(1, entry.bucket.millisUntilNextToken() / 1000);

            throw new WebApplicationException(Response.status(429)
                    .header("Retry-After", String.valueOf(retryAfterSeconds))
                    .entity("Rate limit exceeded")
                    .build());
        }
    }

    private void cleanupOldEntries (long nowMillis) {
        final long cutoff = nowMillis - BUCKET_TTL.toMillis();
        buckets.entrySet().removeIf(e -> e.getValue().lastSeenAtMillis < cutoff);

        // Still too big? Remove arbitrary entries (best-effort bound).
        if (buckets.size() > MAX_BUCKETS) {
            int toRemove = buckets.size() - MAX_BUCKETS;
            for (String k : buckets.keySet()) {
                buckets.remove(k);
                if (--toRemove <= 0)
                    break;
            }
        }
    }

    private String resolveKey (ContainerRequestContext request) {
        // Prefer bearer token if present
        String authorization = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorization != null
                && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authorization.substring(7).trim();
            if (!token.isEmpty()) {
                return "bearer:" + shortHash(token);
            }
        }

        // Fallback to username/principal name
//        String name = request.getSecurityContext().getUserPrincipal().getName();
//        if (name != null && !name.isBlank()) {
//            return "user:" + name;
//        }

        return null;
    }

    private String shortHash (String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            // short stable identifier; never store raw token
            String b64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest);
            return b64.substring(0, Math.min(16, b64.length()));
        }
        catch (Exception e) {
            // extremely unlikely; fallback to deterministic-ish hash
            String fallback = Integer.toHexString(Objects.hashCode(token));
            jlog.warn("Could not hash token securely, using fallback hash");
            return fallback;
        }
    }

    /**
     * Minimal token bucket with lazy refill.
     */
    static final class TokenBucket {
        private final long capacity;
        private final long refillTokens;
        private final long refillPeriodMillis;

        private long tokens;
        private long lastRefillAtMillis;

        TokenBucket (long capacity, long refillTokens, long refillPeriodMillis) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodMillis = refillPeriodMillis;
            this.tokens = capacity;
            this.lastRefillAtMillis = System.currentTimeMillis();
        }

        synchronized boolean tryConsume (long n) {
            refill();
            if (tokens >= n) {
                tokens -= n;
                return true;
            }
            return false;
        }

        synchronized long millisUntilNextToken () {
            refill();
            if (tokens > 0)
                return 0;
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillAtMillis;
            return Math.max(0, refillPeriodMillis - elapsed);
        }

        private void refill () {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillAtMillis;
            if (elapsed < refillPeriodMillis)
                return;

            long periods = elapsed / refillPeriodMillis;
            long add = periods * refillTokens;
            tokens = Math.min(capacity, tokens + add);
            lastRefillAtMillis += periods * refillPeriodMillis;
        }
    }

    private static final class BucketEntry {
        final TokenBucket bucket;
        volatile long lastSeenAtMillis;

        BucketEntry (TokenBucket bucket, long lastSeenAtMillis) {
            this.bucket = bucket;
            this.lastSeenAtMillis = lastSeenAtMillis;
        }
    }
}
