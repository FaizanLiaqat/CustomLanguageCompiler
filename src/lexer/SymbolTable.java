package lexer;

import java.util.HashMap;
import java.util.Map;

import token.Token;


public  class SymbolTable {
    // For simplicity, we use a HashMap. In a real compiler you might maintain scope via a stack.
    private Map<String, Token> table = new HashMap<>();
    
    public void addSymbol(Token token) {
        table.put(token.lexeme, token);
    }
    
    public Token lookup(String lexeme) {
        return table.get(lexeme);
    }
    
    public void display() {
        System.out.println("Symbol Table:");
        for (Token token : table.values()) {
            System.out.println(token);
        }
    }
}