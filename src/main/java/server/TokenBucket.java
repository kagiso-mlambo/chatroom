package server;

import java.time.Instant;
import static java.lang.Math.min;

public class TokenBucket {
    private double currentTokens;
    private final double tokenLimit;
    private Instant lastRefillTime;

    public TokenBucket(double tokenLimit, Instant lastRefillTime){
        this.tokenLimit = tokenLimit;
        currentTokens = this.tokenLimit;
        this.lastRefillTime = lastRefillTime;
    }

    public double tokens(){ return currentTokens; }

    public Instant lastRefillTime() { return lastRefillTime; }

    public void deductTokens(){ currentTokens--; }

    public void addTokens(double tokensEarned){ currentTokens =  min(currentTokens + tokensEarned, tokenLimit); }

    public void updateLastRefillTime(){ this.lastRefillTime = Instant.now(); }
}
