package lexer;

import token.Token;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    private Stack<Map<String, Token>> scopes = new Stack<>();
    public SymbolTable() { enterScope(); }
    public void enterScope() { scopes.push(new HashMap<>()); }
    public void exitScope() { if (scopes.size() > 1) scopes.pop(); }
    public void addSymbol(Token token) { scopes.peek().put(token.value, token); }
    public Token lookup(String lexeme) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Token t = scopes.get(i).get(lexeme);
            if (t != null) return t;
        }
        return null;
    }
    public void display() {
        System.out.println("Symbol Table (Scopes):");
        for (int i = 0; i < scopes.size(); i++) {
            System.out.println("Scope " + i + ":");
            for (Token token : scopes.get(i).values())
                System.out.println("  " + token);
        }
    }
}
