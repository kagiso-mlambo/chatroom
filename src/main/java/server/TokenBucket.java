package server;

import java.time.Instant;
import static java.lang.Math.min;


/**
 * A simple token-bucket rate limiter.
 *
 * A bucket starts with a fixed number of tokens (its limit). Each
 * action that should be rate-limited deducts one token via
 * {@link #deductTokens()}; once the bucket is empty, further actions
 * should be rejected by the caller. Tokens are replenished over time
 * by calling {@link #addTokens(double)}, which never lets the total
 * exceed the configured limit.
 *
 * This class does not refill itself automatically or track elapsed
 * time internally beyond {@link #lastRefillTime}; callers are
 * responsible for computing how many tokens have been earned since
 * the last refill and passing that amount to {@link #addTokens(double)}.
 */
public class TokenBucket {
    private double currentTokens;
    private final double tokenLimit;
    private Instant lastRefillTime;


    /**
     * Creates a new token bucket, starting full at its token limit.
     *
     * @param tokenLimit     the maximum number of tokens the bucket can hold
     * @param lastRefillTime the initial "last refill" timestamp, typically
     *                       the time the bucket is created
     */
    public TokenBucket(double tokenLimit, Instant lastRefillTime){
        this.tokenLimit = tokenLimit;
        currentTokens = this.tokenLimit;
        this.lastRefillTime = lastRefillTime;
    }


    /**
     * Returns the number of tokens currently available.
     *
     * @return the current token count
     */
    public double tokens(){ return currentTokens; }


    /**
     * Returns the timestamp of the most recent refill.
     *
     * @return the last refill time
     */
    public Instant lastRefillTime() { return lastRefillTime; }


    /**
     * Deducts one token from the bucket, typically called when the
     * rate-limited action is performed.
     */
    public void deductTokens(){ currentTokens--; }


    /**
     * Adds earned tokens to the bucket, capping the result at the
     * bucket's token limit so it never overflows.
     *
     * @param tokensEarned the number of tokens to add
     */
    public void addTokens(double tokensEarned){ currentTokens =  min(currentTokens + tokensEarned, tokenLimit); }


    /**
     * Records the current time as the last refill time.
     */
    public void updateLastRefillTime(){ this.lastRefillTime = Instant.now(); }
}
