package server;

public enum ANSICodes {
    BOLD("\u001B[1m"),
    RESET("\u001B[0m"),
    ITALICS("\u001b[3m"),
    UNDERLINE("\u001B[4m");

    private final String code;

    ANSICodes(String code){ this.code = code; }

    public String code(){ return code; }
}
