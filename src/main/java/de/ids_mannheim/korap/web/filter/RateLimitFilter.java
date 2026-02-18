package de.ids_mannheim.korap.web.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.utils.TimeUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import lombok.Getter;

/**
 * Simple in-memory rate limitation for authenticated users.
 * <p>
 * Keyed by bearer token (preferred) or username (fallback).
 * <p>
 * Note: In-memory means per-JVM only. For clustered deployments, use Redis/etc.
 * 
 * Implemented with AI assistance
 */
@Component
@Priority(Priorities.AUTHORIZATION)
public class RateLimitFilter implements ContainerRequestFilter {

    private static final Logger jlog = LogManager
            .getLogger(RateLimitFilter.class);

    @Autowired
    private KustvaktConfiguration config;

    // Rate limiting configuration (loaded from kustvakt.conf)
    private long refillTokens;
    private Duration refillPeriod;
    @Getter
    private long burstCapacity;
    private int maxBuckets;
    private Duration bucketTTL;

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    @PostConstruct
    private void initializeConfiguration() {
        try {
            Properties props = config.getProperties();
            
            // Handle case where properties might not be initialized yet
            if (props == null) {
                jlog.warn("KustvaktConfiguration properties not available, using default rate limiting values");
                setDefaultValues();
                return;
            }
            
            // Load rate limiting settings from kustvakt.conf with sensible defaults
            String refillTokensStr = props.getProperty("ratelimit.refill.tokens", "60");
            this.refillTokens = Long.parseLong(refillTokensStr);

            String refillPeriodStr = props.getProperty("ratelimit.refill.period", "1M");
            this.refillPeriod = Duration.ofSeconds(TimeUtils.convertTimeToSeconds(refillPeriodStr));

            String burstCapacityStr = props.getProperty("ratelimit.burst.capacity", "60");
            this.burstCapacity = Long.parseLong(burstCapacityStr);

            String maxBucketsStr = props.getProperty("ratelimit.max.buckets", "10000");
            this.maxBuckets = Integer.parseInt(maxBucketsStr);

            String bucketTTLStr = props.getProperty("ratelimit.bucket.ttl", "6H");
            this.bucketTTL = Duration.ofSeconds(TimeUtils.convertTimeToSeconds(bucketTTLStr));

            jlog.info("Rate limiting initialized: refillTokens={}, refillPeriod={}, burstCapacity={}, maxBuckets={}, bucketTTL={}",
                    refillTokens, refillPeriod, burstCapacity, maxBuckets, bucketTTL);
        } catch (Exception e) {
            jlog.error("Failed to initialize rate limiting configuration, using defaults", e);
            setDefaultValues();
        }
    }
    
    private void setDefaultValues() {
        this.refillTokens = 60;
        this.refillPeriod = Duration.ofMinutes(1);
        this.burstCapacity = 60;
        this.maxBuckets = 10000;
        this.bucketTTL = Duration.ofHours(6);
        jlog.info("Rate limiting initialized with defaults: refillTokens={}, refillPeriod={}, burstCapacity={}, maxBuckets={}, bucketTTL={}",
                refillTokens, refillPeriod, burstCapacity, maxBuckets, bucketTTL);
    }

    @Override
    public void filter (ContainerRequestContext request) {
        // Only apply to authenticated requests
        if (request.getSecurityContext() == null
                || request.getSecurityContext().getUserPrincipal() == null) {
            jlog.debug("Skipping rate limiting - no SecurityContext or UserPrincipal");
            return;
        }

        String key = resolveKey(request);
        if (key == null) {
            jlog.debug("Skipping rate limiting - could not resolve key");
            return;
        }

        jlog.debug("Applying rate limiting for key: {}", key);

        long now = System.currentTimeMillis();

        // Opportunistic cleanup to avoid memory growth.
        if (buckets.size() > maxBuckets) {
            cleanupOldEntries(now);
        }

        BucketEntry entry = buckets.compute(key, (k, existing) -> {
            if (existing == null) {
                if (buckets.size() > maxBuckets) {
                    cleanupOldEntries(now);
                }
                return new BucketEntry(new TokenBucket(burstCapacity,
                        refillTokens, refillPeriod.toMillis()), now);
            }
            existing.lastSeenAtMillis = now;
            return existing;
        });

        if (!entry.bucket.tryConsume(1)) {
            long retryAfterSeconds = Math
                    .max(1, entry.bucket.millisUntilNextToken() / 1000);

            jlog.info("Rate limit exceeded for key: {}, retry after {} seconds", key, retryAfterSeconds);
            throw new WebApplicationException(Response.status(429)
                    .header("Retry-After", String.valueOf(retryAfterSeconds))
                    .entity("Rate limit exceeded")
                    .build());
        }
    }

    /**
     * Clear all rate limit buckets. For testing purposes only.
     */
    public void clearBuckets() {
        buckets.clear();
        jlog.info("Rate limit buckets cleared");
    }

    private void cleanupOldEntries (long nowMillis) {
        final long cutoff = nowMillis - bucketTTL.toMillis();
        buckets.entrySet().removeIf(e -> e.getValue().lastSeenAtMillis < cutoff);

        // Still too big? Remove arbitrary entries (best-effort bound).
        if (buckets.size() > maxBuckets) {
            int toRemove = buckets.size() - maxBuckets;
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

        // Skip unauthenticated requests. DemoUserFilter sets username guest 
        // for such requests.
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