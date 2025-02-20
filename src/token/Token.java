package token;

public class Token {
    String type;
    public String value;
    int line;
    
    public Token(String type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }
    
    @Override
    public String toString() {
        return "Token{type='" + type + "', value='" + value + "', line=" + line + "}";
    }
}
