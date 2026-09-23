package server;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

    @Test
    void startsFullAtTheGivenLimit() {
        TokenBucket bucket = new TokenBucket(5, Instant.now());

        assertEquals(5, bucket.tokens());
    }

    @Test
    void deductTokensRemovesOneToken() {
        TokenBucket bucket = new TokenBucket(3, Instant.now());

        bucket.deductTokens();

        assertEquals(2, bucket.tokens());
    }

    @Test
    void deductTokensCanDropBelowZero() {
        // TokenBucket itself does not refuse to go negative; callers are
        // expected to check tokens() < 1 before deducting.
        TokenBucket bucket = new TokenBucket(1, Instant.now());

        bucket.deductTokens();
        bucket.deductTokens();

        assertEquals(-1, bucket.tokens());
    }

    @Test
    void addTokensIncreasesTheCount() {
        TokenBucket bucket = new TokenBucket(5, Instant.now());
        bucket.deductTokens();
        bucket.deductTokens();

        bucket.addTokens(1);

        assertEquals(4, bucket.tokens());
    }

    @Test
    void addTokensNeverExceedsTheLimit() {
        TokenBucket bucket = new TokenBucket(5, Instant.now());

        bucket.addTokens(100);

        assertEquals(5, bucket.tokens());
    }

    @Test
    void addTokensCapsExactlyAtTheLimitBoundary() {
        TokenBucket bucket = new TokenBucket(3, Instant.now());
        bucket.deductTokens();

        bucket.addTokens(1);

        assertEquals(3, bucket.tokens());
    }

    @Test
    void lastRefillTimeReturnsTheConstructorValueInitially() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        TokenBucket bucket = new TokenBucket(5, createdAt);

        assertEquals(createdAt, bucket.lastRefillTime());
    }

    @Test
    void updateLastRefillTimeMovesItForward() {
        Instant createdAt = Instant.parse("2020-01-01T00:00:00Z");
        TokenBucket bucket = new TokenBucket(5, createdAt);

        bucket.updateLastRefillTime();

        assertTrue(bucket.lastRefillTime().isAfter(createdAt));
    }
}
