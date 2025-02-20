package compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import automata.NFABuilder;
import automata.NFABuilder.DFAState;
import lexer.ErrorHandler;
import lexer.LexicalAnalyzer;
import lexer.SymbolTable;
import token.Token;

public class CompilerFrontEnd {
    public static void main(String[] args) {
        // Reset state counter.
        NFABuilder.stateIdCounter = 0;
        
        // Define token regex patterns.
        Map<String, String> tokenRegexes = new LinkedHashMap<>();
        tokenRegexes.put("KEYWORD", "global|local|const|int|float|bool|char|string|if|else|for|while|return");
        tokenRegexes.put("BOOLEAN", "true|false");
        tokenRegexes.put("INTEGER", "-?(0|1|2|3|4|5|6|7|8|9)+");
        tokenRegexes.put("DECIMAL", "-?(0|1|2|3|4|5|6|7|8|9)+\\.(0|1|2|3|4|5|6|7|8|9)+(e(\\+|-)?(0|1|2|3|4|5|6|7|8|9)+)?");
        tokenRegexes.put("STDOUT", "System\\.out\\.println|System\\.out\\.print");
        tokenRegexes.put("STDIN", "System\\.in");
        tokenRegexes.put("SINGLE_LINE_COMMENT", "//(.)*\\n");
        tokenRegexes.put("MULTI_LINE_COMMENT", "/\\*((.|\\n)*)\\*/");
        tokenRegexes.put("OPERATOR", "(==|=|\\+|-|\\*|/|%|>|<)");
        tokenRegexes.put("DELIMITER", "(;|,|\\(|\\)|\\{|\\})");
        tokenRegexes.put("STRING_LITERAL", "\"([^\"\\\\]|\\\\.)*\"");
        tokenRegexes.put("CHARACTER_LITERAL", "'([^'\\\\]|\\\\.)'");
        tokenRegexes.put("IDENTIFIER", "([a-z])([a-z])*");
        
        List<NFABuilder.NFA> nfaList = new ArrayList<>();
        for (Map.Entry<String, String> entry : tokenRegexes.entrySet()) {
            String tokenType = entry.getKey();
            String regex = entry.getValue();
            System.out.println("Building NFA for token " + tokenType + " using regex: " + regex);
            NFABuilder.NFA nfa = NFABuilder.RegexToNFA.convert(regex);
            nfa.accept.isAccept = true;
            nfa.accept.tokenType = tokenType;
            nfaList.add(nfa);
            System.out.println("Transition table for " + tokenType + " NFA:");
            NFABuilder.displayTransitionTable(nfa.start);
            System.out.println("Total states for " + tokenType + " NFA: " + NFABuilder.countStates(nfa.start));
            System.out.println("----------------------------------");
        }
        
        NFABuilder.NFA masterNFA = NFABuilder.combineNFAs(nfaList);
        System.out.println("\nCombined Master NFA Transition Table:");
        NFABuilder.displayTransitionTable(masterNFA.start);
        System.out.println("Total states in Combined Master NFA: " + NFABuilder.countStates(masterNFA.start));
        
        DFAState dfaStart = NFABuilder.convertNFAtoDFA(masterNFA);
        System.out.println("\nDFA Transition Table:");
        NFABuilder.displayDFATransitionTable(dfaStart);
        System.out.println("Total DFA states: " + countDFAStates(dfaStart));
        
        String source = readSourceFromFile("input.txt");
        System.out.println("\nSource Code from file:");
        System.out.println(source);
        
        ErrorHandler errorHandler = new ErrorHandler();
        SymbolTable symbolTable = new SymbolTable();
        
        LexicalAnalyzer lexer = new LexicalAnalyzer(dfaStart, errorHandler, symbolTable);
        List<Token> tokens = lexer.tokenize(source);
        System.out.println("\nTokens:");
        for (Token token : tokens)
            System.out.println(token);
        System.out.println("Total tokens: " + tokens.size());
        
        if (errorHandler.hasErrors()) {
            System.out.println("\nErrors:");
            errorHandler.printErrors();
        }
        
        System.out.println("\nSymbol Table:");
        symbolTable.display();
    }
    
    public static int countDFAStates(NFABuilder.DFAState start) {
        Set<Integer> visited = new HashSet<>();
        collectDFAStates(start, visited);
        return visited.size();
    }
    
    private static void collectDFAStates(NFABuilder.DFAState state, Set<Integer> visited) {
        if (visited.contains(state.id)) return;
        visited.add(state.id);
        for (NFABuilder.DFAState target : state.transitions.values())
            collectDFAStates(target, visited);
    }
    
    public static String readSourceFromFile(String fileName) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return sb.toString();
    }
}
