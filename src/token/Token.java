package token;

public class Token {
    String type;    // e.g., IDENTIFIER, INTEGER, KEYWORD, etc.
    public String lexeme;
    int line;
    
    public Token(String type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }
    
    public String toString() {
        return "Token(" + type + ", \"" + lexeme + "\", line " + line + ")";
    }
}
