package common;


/**
 * Single source of truth for the ANSI escape codes used to format terminal
 * output across both the server and client.
 *
 * Each constant wraps the raw escape sequence for one formatting effect;
 * call {@link #code()} to get the string to print. Codes like {@link #BOLD},
 * {@link #ITALICS}, and {@link #UNDERLINE} must be paired with a following
 * {@link #RESET} or the formatting will keep applying to everything printed
 * afterward.
 */
public enum ANSICodes {
    /** Turns on bold text. Must be followed by {@link #RESET} to turn it back off. */
    BOLD("\u001B[1m"),

    /** Clears all active formatting (bold, italics, underline) back to normal. */
    RESET("\u001B[0m"),

    /** Turns on italic text. Must be followed by {@link #RESET} to turn it back off. */
    ITALICS("\u001b[3m"),

    /** Turns on underlined text. Must be followed by {@link #RESET} to turn it back off. */
    UNDERLINE("\u001B[4m"),

    /** Moves the cursor to the start of the current line and clears it, used to overwrite
     * the last printed line rather than adding a new one. */
    CLEAR("\r\033[K");

    private final String code;

    ANSICodes(String code){ this.code = code; }

    /**
     * @return the raw ANSI escape sequence for this code, ready to be
     *         printed directly to the terminal
     */
    public String code(){ return code; }
}
