package lexer;

import java.util.ArrayList;
import java.util.List;
import automata.NFABuilder;
import automata.NFABuilder.DFAState;
import automata.NFABuilder.State; // Import the NFABuilder.State class
import token.Token; // Import your Token class

public class LexicalAnalyzer {
    DFAState dfaStart;
    ErrorHandler errorHandler;
    SymbolTable symbolTable;
    
    public LexicalAnalyzer(DFAState dfaStart, ErrorHandler errorHandler, SymbolTable symbolTable) {
        this.dfaStart = dfaStart;
        this.errorHandler = errorHandler;
        this.symbolTable = symbolTable;
    }
    
    // Tokenize the input source code and return a list of tokens.
    public List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int line = 1;
        
        while (pos < source.length()) {
            // Skip whitespace (you might want to do more pre-processing here)
            char current = source.charAt(pos);
            if (current == ' ' || current == '\t' || current == '\r') {
                pos++;
                continue;
            }
            // Track line number.
            if (current == '\n') {
                line++;
                pos++;
                continue;
            }
            
            int startPos = pos;
            DFAState currentState = dfaStart;
            int lastAcceptPos = -1;
            DFAState lastAcceptState = null;
            
            int i = pos;
            // Simulate the DFA on the input until no transition is possible.
            while (i < source.length()) {
                char ch = source.charAt(i);
                // If we have a DFA transition on the character, follow it.
                DFAState nextState = currentState.transitions.get(ch);
                // Also, check if the DFA has a wildcard transition.
                if (nextState == null)
                    nextState = currentState.transitions.get(NFABuilder.WILDCARD_MARKER);
                
                if (nextState == null)
                    break;
                
                currentState = nextState;
                i++;
                if (currentState.isAccept) {
                    lastAcceptPos = i;
                    lastAcceptState = currentState;
                }
            }
            
            if (lastAcceptState == null) {
                // No token recognized. Report an error.
                errorHandler.addError("Unrecognized token starting with '" + source.charAt(pos) + "'", line);
                // Skip one character and try again.
                pos++;
            } else {
                String lexeme = source.substring(startPos, lastAcceptPos);
                String tokenType = getTokenTypeFromDFAState(lastAcceptState);
                Token token = new Token(tokenType, lexeme, line);
                tokens.add(token);
                
                // If the token is an identifier, add it to the symbol table.
                if (tokenType.equals("IDENTIFIER"))
                    symbolTable.addSymbol(token);
                
                pos = lastAcceptPos;
            }
        }
        
        return tokens;
    }
    
    // This helper method extracts the token type from the DFA state.
    // (In our conversion, a DFA state's "accept" property is set if any NFA state was accepting.)
    private String getTokenTypeFromDFAState(DFAState dfaState) {
        // We assume that if a DFA state is accepting, then at least one NFA state had a tokenType.
        // Here we pick the first tokenType found.
        for (State s : dfaState.nfaStates) {
            if (s.isAccept && s.tokenType != null)
                return s.tokenType;
        }
        return "UNKNOWN";
    }
}
