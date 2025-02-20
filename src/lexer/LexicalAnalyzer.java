package lexer;

import java.util.*;
import automata.NFABuilder;
import automata.NFABuilder.DFAState;
import automata.NFABuilder.State;
import token.Token;

public class LexicalAnalyzer {
    DFAState dfaStart;
    ErrorHandler errorHandler;
    SymbolTable symbolTable;
    
    private static final Set<String> keywords = new HashSet<>(
        Arrays.asList("global", "local", "const", "int", "float", "bool", "char", "string", "if", "else", "for", "while", "return")
    );
    
    private static final Map<String, Integer> tokenPriorityMap = new HashMap<>();
    static {
        tokenPriorityMap.put("KEYWORD", 6);
        tokenPriorityMap.put("STRING_LITERAL", 5);
        tokenPriorityMap.put("CHARACTER_LITERAL", 5);
        tokenPriorityMap.put("INTEGER", 5);
        tokenPriorityMap.put("DECIMAL", 5);
        tokenPriorityMap.put("STDOUT", 5);
        tokenPriorityMap.put("STDIN", 5);
        tokenPriorityMap.put("OPERATOR", 5);
        tokenPriorityMap.put("DELIMITER", 5);
        tokenPriorityMap.put("IDENTIFIER", 4);
        tokenPriorityMap.put("SINGLE_LINE_COMMENT", 1);
        tokenPriorityMap.put("MULTI_LINE_COMMENT", 1);
    }
    
    private int getTokenPriority(String tokenType) {
        Integer prio = tokenPriorityMap.get(tokenType);
        return prio != null ? prio : 0;
    }
    
    public LexicalAnalyzer(DFAState dfaStart, ErrorHandler errorHandler, SymbolTable symbolTable) {
        this.dfaStart = dfaStart;
        this.errorHandler = errorHandler;
        this.symbolTable = symbolTable;
    }
    
    public List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0, line = 1;
        while (pos < source.length()) {
            char current = source.charAt(pos);
            if (current == ' ' || current == '\t' || current == '\r') { pos++; continue; }
            if (current == '\n') { line++; pos++; continue; }
            int startPos = pos;
            DFAState currentState = dfaStart;
            int lastAcceptPos = -1;
            DFAState lastAcceptState = null;
            int i = pos;
            while (i < source.length()) {
                char ch = source.charAt(i);
                DFAState nextState = currentState.transitions.get(ch);
                if (nextState == null)
                    nextState = currentState.transitions.get(NFABuilder.WILDCARD_MARKER);
                if (nextState == null) break;
                currentState = nextState;
                i++;
                if (currentState.isAccept) { lastAcceptPos = i; lastAcceptState = currentState; }
            }
            if (lastAcceptState == null) {
                errorHandler.addError("Unrecognized token starting with '" + source.charAt(pos) + "'", line);
                pos++;
            } else {
                String lexeme = source.substring(startPos, lastAcceptPos);
                String tokenType = getTokenTypeFromDFAState(lastAcceptState);
                tokenType = determineTokenType(tokenType, lexeme);
                // Discard comments.
                if (tokenType.equals("SINGLE_LINE_COMMENT") || tokenType.equals("MULTI_LINE_COMMENT")) {
                    pos = lastAcceptPos;
                    continue;
                }
                Token token = new Token(tokenType, lexeme, line);
                tokens.add(token);
                if (tokenType.equals("IDENTIFIER"))
                    symbolTable.addSymbol(token);
                pos = lastAcceptPos;
            }
        }
        return tokens;
    }
    
    private String getTokenTypeFromDFAState(DFAState dfaState) {
        String chosenType = "UNKNOWN";
        int bestPriority = -1;
        for (State s : dfaState.nfaStates) {
            if (s.isAccept && s.tokenType != null) {
                int prio = getTokenPriority(s.tokenType);
                if (prio > bestPriority) {
                    bestPriority = prio;
                    chosenType = s.tokenType;
                }
            }
        }
        return chosenType;
    }
    
    private String determineTokenType(String tokenType, String lexeme) {
        if (tokenType.equals("IDENTIFIER") && keywords.contains(lexeme))
            return "KEYWORD";
        if (tokenType.equals("INTEGER") || tokenType.equals("DECIMAL"))
            return "LITERAL";
        return tokenType;
    }
}
