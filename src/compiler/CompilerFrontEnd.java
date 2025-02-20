package compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import automata.NFABuilder;
import automata.NFABuilder.DFAState;

public class CompilerFrontEnd {
    public static void main(String[] args) {
        // Reset state ID counter.
        NFABuilder.stateIdCounter = 0;

        // Define token regex patterns in our simplified syntax.
        Map<String, String> tokenRegexes = new LinkedHashMap<>();
        tokenRegexes.put("BOOLEAN", "true|false");
        tokenRegexes.put("INTEGER", "-?(0|1|2|3|4|5|6|7|8|9)+");
        tokenRegexes.put("CHARACTER", "[a-z]");  // Expand manually.
        tokenRegexes.put("ARITH_OP", "-|\\+|\\*|/|%");
        tokenRegexes.put("DECIMAL", 
            "-?((0|1|2|3|4|5|6|7|8|9)+)\\." +
            "(0|1|2|3|4|5|6|7|8|9)" +
            "((0|1|2|3|4|5|6|7|8|9)?" +
            "((0|1|2|3|4|5|6|7|8|9)?" +
            "((0|1|2|3|4|5|6|7|8|9)?" +
            "((0|1|2|3|4|5|6|7|8|9)?)?)?)");
        tokenRegexes.put("MULTI_LINE_COMMENT", "/\\*([\\s\\S])*?\\*/");
        tokenRegexes.put("STDOUT", "System\\.out\\.println|System\\.out\\.print");
        tokenRegexes.put("STDIN", "System\\.in");
        tokenRegexes.put("SINGLE_LINE_COMMENT", "//(.)*\\n");
        tokenRegexes.put("IDENTIFIER", 
            "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)" +
            "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)*");

        // Expand CHARACTER token.
        if (tokenRegexes.get("CHARACTER").equals("[a-z]")) {
            tokenRegexes.put("CHARACTER", "a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z");
        }

        List<NFABuilder.NFA> nfaList = new ArrayList<>();

        // Build and display each token's NFA.
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

        // Combine all token NFAs into one master NFA.
        NFABuilder.NFA masterNFA = NFABuilder.combineNFAs(nfaList);
        System.out.println("\nCombined Master NFA Transition Table:");
        NFABuilder.displayTransitionTable(masterNFA.start);
        System.out.println("Total states in Combined Master NFA: " + NFABuilder.countStates(masterNFA.start));
        
        // Convert the combined NFA to a DFA.
        DFAState dfaStart = NFABuilder.convertNFAtoDFA(masterNFA);
        System.out.println("\nDFA Transition Table:");
        NFABuilder.displayDFATransitionTable(dfaStart);
        System.out.println("Total DFA states: " + countDFAStates(dfaStart));

        // Now read the input from a file ("input.txt").
        String source = readSourceFromFile("input.txt");
        System.out.println("\nSource Code from file:");
        System.out.println(source);

        // Create an ErrorHandler and SymbolTable (assumed to be in your lexer package).
        lexer.ErrorHandler errorHandler = new lexer.ErrorHandler();
        lexer.SymbolTable symbolTable = new lexer.SymbolTable();

        // Create a LexicalAnalyzer with the DFA.
        lexer.LexicalAnalyzer lexer = new lexer.LexicalAnalyzer(dfaStart, errorHandler, symbolTable);

        // Tokenize the input source code.
        List<token.Token> tokens = lexer.tokenize(source);

        System.out.println("\nTokens:");
        for (token.Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("Total tokens: " + tokens.size());

        if (errorHandler.hasErrors()) {
            System.out.println("\nErrors:");
            errorHandler.printErrors();
        }

        System.out.println("\nSymbol Table:");
        symbolTable.display();
    }

    // Helper method to count DFA states, defined in CompilerFrontEnd.
    public static int countDFAStates(NFABuilder.DFAState start) {
        Set<Integer> visited = new HashSet<>();
        collectDFAStates(start, visited);
        return visited.size();
    }
    
    private static void collectDFAStates(NFABuilder.DFAState state, Set<Integer> visited) {
        if (visited.contains(state.id))
            return;
        visited.add(state.id);
        for (NFABuilder.DFAState target : state.transitions.values()) {
            collectDFAStates(target, visited);
        }
    }
    
    // Helper method to read source code from a file.
    public static String readSourceFromFile(String fileName) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return sb.toString();
    }
}
