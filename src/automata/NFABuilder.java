package automata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;


public class NFABuilder {

    // A counter to assign unique IDs to each state.
    public static int stateIdCounter = 0;
    
    // Constants for our special markers.
    private static final char WHITESPACE_MARKER = '\u0001';  // represents \s
    private static final char NEWLINE_MARKER    = '\u0002';  // represents \n
    public static final char WILDCARD_MARKER   = '\u0003';  // represents . as a wildcard
    // Use '#' as our explicit concatenation operator.
    private static final char CONCAT_OP         = '#';

    // Represents an NFA state.
    public static class State {
        int id;
        // Transitions for a literal symbol (non-epsilon)
        Map<Character, List<State>> transitions;
        // Epsilon transitions (represented by the special symbol 'ε' when printing)
        List<State> epsilonTransitions;
        public boolean isAccept;
        public String tokenType;  // Tag to indicate which token this accept state belongs to.

        public State() {
            id = stateIdCounter++;
            transitions = new HashMap<>();
            epsilonTransitions = new ArrayList<>();
            isAccept = false;
            tokenType = null;
        }

        public void addTransition(char symbol, State target) {
            transitions.computeIfAbsent(symbol, k -> new ArrayList<>()).add(target);
        }

        public void addEpsilon(State target) {
            epsilonTransitions.add(target);
        }
    }

    // Represents an NFA fragment with a start and an accept state.
    public static class NFA {
        public State start;
        public State accept;

        public NFA(State start, State accept) {
            this.start = start;
            this.accept = accept;
        }
    }

    // This inner class converts a simplified regular expression into an NFA.
    public static class RegexToNFA {

        // Check if a character is an operator or a parenthesis.
        private static boolean isOperator(char c) {
            return (c == '|' || c == '*' || c == '+' || c == '?' || c == CONCAT_OP || c == '(' || c == ')');
        }

        // A character is literal if it is not an operator.
        private static boolean isLiteral(char c) {
            // Note: We now treat '.' as a literal (wildcard) even though it is special.
            return (c != '|' && c != '*' && c != '+' && c != '?' && c != CONCAT_OP && c != '(' && c != ')');
        }

        // Operator precedence: '*' '+' '?' highest (3), then concatenation (CONCAT_OP) (2), then alternation ('|') (1)
        public static int precedence(char op) {
            if (op == '*' || op == '+' || op == '?')
                return 3;
            else if (op == CONCAT_OP)
                return 2;
            else if (op == '|')
                return 1;
            else
                return 0;
        }

        // Insert explicit concatenation operator (CONCAT_OP) between characters/operators when needed.
        // Also handles escape sequences: a backslash '\' causes the next character to be treated specially.
        public static String insertConcat(String regex) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < regex.length(); i++) {
                char c = regex.charAt(i);
                // Handle escape: if we see '\', check for special escapes.
                if (c == '\\' && i + 1 < regex.length()) {
                    char next = regex.charAt(i + 1);
                    // For known escapes, output the special marker.
                    if (next == 's') {
                        result.append(WHITESPACE_MARKER);
                    } else if (next == 'n') {
                        result.append(NEWLINE_MARKER);
                    } else if (next == '.') {
                        // If user wants a literal dot, output dot.
                        result.append('.');
                    } else {
                        // Otherwise, treat it as a literal.
                        result.append('\\').append(next);
                    }
                    i++; // skip next char since we processed it
                } else {
                    result.append(c);
                }
                // Lookahead: if current token (or escape sequence) should be concatenated with the next token.
                if (i + 1 < regex.length()) {
                    char curr = regex.charAt(i);
                    // If we just processed an escape that resulted in a marker, curr is already the marker.
                    char next = regex.charAt(i + 1);
                    // For next token, if it is a backslash then look at the following character.
                    if (next == '\\' && i + 2 < regex.length()) {
                        next = regex.charAt(i + 2);
                    }
                    // Insert concatenation if current is literal (or closure operator, or closing parenthesis)
                    // and next is literal, an escape, or an opening parenthesis.
                    if ((isLiteral(curr) || curr == '*' || curr == '+' || curr == '?' || curr == ')') &&
                        (isLiteral(next) || next == '(' || next == '\\')) {
                        result.append(CONCAT_OP);
                    }
                }
            }
            return result.toString();
        }

        // Convert infix regex (with explicit concatenation) to postfix notation.
        public static String infixToPostfix(String regex) {
            StringBuilder postfix = new StringBuilder();
            Stack<Character> stack = new Stack<>();
            String regexWithConcat = insertConcat(regex);
            for (int i = 0; i < regexWithConcat.length(); i++) {
                char c = regexWithConcat.charAt(i);
                // Handle escape sequences: if we see our special markers, they are already processed.
                if (c == '\\') {
                    // (Should not occur now, as our escapes are handled in insertConcat.)
                    i++;
                    if (i < regexWithConcat.length()) {
                        postfix.append('\\');
                        postfix.append(regexWithConcat.charAt(i));
                    }
                    continue;
                }
                if (isLiteral(c) || c == WHITESPACE_MARKER || c == NEWLINE_MARKER) {
                    // Note: if the literal is a dot '.', we want to treat it as wildcard (not as literal dot).
                    postfix.append(c);
                } else if (c == '(') {
                    stack.push(c);
                } else if (c == ')') {
                    while (!stack.isEmpty() && stack.peek() != '(') {
                        postfix.append(stack.pop());
                    }
                    if (!stack.isEmpty()) {
                        stack.pop(); // pop '('
                    }
                } else {
                    // c is an operator: pop until an operator with lower precedence is found.
                    while (!stack.isEmpty() && stack.peek() != '(' && precedence(stack.peek()) >= precedence(c)) {
                        postfix.append(stack.pop());
                    }
                    stack.push(c);
                }
            }
            while (!stack.isEmpty()) {
                postfix.append(stack.pop());
            }
            return postfix.toString();
        }

        // Convert the postfix expression to an NFA using Thompson's Construction.
        public static NFA postfixToNFA(String postfix) {
            Stack<NFA> stack = new Stack<>();
            for (int i = 0; i < postfix.length(); i++) {
                char c = postfix.charAt(i);
                // Handle special markers (they are treated as literals).
                if (c == WHITESPACE_MARKER || c == NEWLINE_MARKER) {
                    stack.push(createBasicNFA(c));
                }
                else if (isLiteral(c)) {
                    // If literal is '.', treat it as wildcard.
                    if (c == '.') {
                        stack.push(createWildcardNFA());
                    } else {
                        stack.push(createBasicNFA(c));
                    }
                } else if (c == '*') {
                    NFA nfa = stack.pop();
                    stack.push(applyKleeneStar(nfa));
                } else if (c == '+') {
                    NFA nfa = stack.pop();
                    stack.push(applyPlus(nfa));
                } else if (c == '?') {
                    NFA nfa = stack.pop();
                    stack.push(applyOptional(nfa));
                } else if (c == CONCAT_OP) {
                    NFA nfa2 = stack.pop();
                    NFA nfa1 = stack.pop();
                    stack.push(concatenateNFA(nfa1, nfa2));
                } else if (c == '|') {
                    NFA nfa2 = stack.pop();
                    NFA nfa1 = stack.pop();
                    stack.push(alternateNFA(nfa1, nfa2));
                }
            }
            return stack.pop();
        }

        // Basic NFA for a literal character.
        public static NFA createBasicNFA(char literal) {
            State start = new State();
            State accept = new State();
            start.addTransition(literal, accept);
            return new NFA(start, accept);
        }
        
        // Create an NFA for the wildcard operator (matches any character).
        public static NFA createWildcardNFA() {
            State start = new State();
            State accept = new State();
            // We mark the transition with our WILDCARD_MARKER.
            start.addTransition(WILDCARD_MARKER, accept);
            return new NFA(start, accept);
        }

        // Thompson's construction for Kleene star.
        public static NFA applyKleeneStar(NFA nfa) {
            State newStart = new State();
            State newAccept = new State();
            newStart.addEpsilon(nfa.start);
            newStart.addEpsilon(newAccept);
            nfa.accept.addEpsilon(nfa.start);
            nfa.accept.addEpsilon(newAccept);
            return new NFA(newStart, newAccept);
        }

        // Construction for plus operator (one or more occurrences).
        public static NFA applyPlus(NFA nfa) {
            State newAccept = new State();
            nfa.accept.addEpsilon(nfa.start);
            nfa.accept.addEpsilon(newAccept);
            return new NFA(nfa.start, newAccept);
        }

        // Construction for the optional operator '?' (zero or one occurrence).
        public static NFA applyOptional(NFA nfa) {
            State newStart = new State();
            State newAccept = new State();
            newStart.addEpsilon(nfa.start);
            newStart.addEpsilon(newAccept);
            nfa.accept.addEpsilon(newAccept);
            return new NFA(newStart, newAccept);
        }

        // Concatenation of two NFAs.
        public static NFA concatenateNFA(NFA nfa1, NFA nfa2) {
            nfa1.accept.addEpsilon(nfa2.start);
            return new NFA(nfa1.start, nfa2.accept);
        }

        // Alternation (union) of two NFAs.
        // The new start state is created with only ε-transitions, ensuring no literal transitions appear at the very start.
        public static NFA alternateNFA(NFA nfa1, NFA nfa2) {
            State newStart = new State();
            State newAccept = new State();
            newStart.addEpsilon(nfa1.start);
            newStart.addEpsilon(nfa2.start);
            nfa1.accept.addEpsilon(newAccept);
            nfa2.accept.addEpsilon(newAccept);
            return new NFA(newStart, newAccept);
        }

        // Main conversion function: from regex (in infix) to NFA.
        public static NFA convert(String regex) {
            String postfix = infixToPostfix(regex);
            return postfixToNFA(postfix);
        }
    }

    // Combine multiple NFAs into one master NFA using alternation pairwise.
    public static NFA combineNFAs(List<NFA> nfaList) {
        if (nfaList.isEmpty()) return null;
        NFA combined = nfaList.get(0);
        for (int i = 1; i < nfaList.size(); i++) {
            combined = RegexToNFA.alternateNFA(combined, nfaList.get(i));
        }
        return combined;
    }

    // Display the transition table starting from a given state.
    public static void displayTransitionTable(State start) {
        Set<Integer> visited = new HashSet<>();
        displayState(start, visited);
    }

    private static void displayState(State state, Set<Integer> visited) {
        if (visited.contains(state.id)) return;
        visited.add(state.id);
        // Print literal transitions.
        for (Map.Entry<Character, List<State>> entry : state.transitions.entrySet()) {
            char symbol = entry.getKey();
            // For clarity, if the symbol is one of our markers, display a readable name.
            String symStr;
            if (symbol == WHITESPACE_MARKER) {
                symStr = "\\s";
            } else if (symbol == NEWLINE_MARKER) {
                symStr = "\\n";
            } else if (symbol == WILDCARD_MARKER) {
                symStr = ".(wildcard)";
            } else {
                symStr = Character.toString(symbol);
            }
            for (State target : entry.getValue()) {
                System.out.println("State " + state.id + " --" + symStr + "--> State " + target.id);
            }
        }
        // Print ε-transitions.
        for (State target : state.epsilonTransitions) {
            System.out.println("State " + state.id + " --ε--> State " + target.id);
        }
        // Recursively display connected states.
        for (List<State> targets : state.transitions.values()) {
            for (State target : targets) {
                displayState(target, visited);
            }
        }
        for (State target : state.epsilonTransitions) {
            displayState(target, visited);
        }
    }

    // Utility method to count total unique states reachable from a given state.
    public static int countStates(State start) {
        Set<Integer> visited = new HashSet<>();
        collectStates(start, visited);
        return visited.size();
    }

    private static void collectStates(State state, Set<Integer> visited) {
        if (visited.contains(state.id)) return;
        visited.add(state.id);
        for (List<State> targets : state.transitions.values()) {
            for (State target : targets) {
                collectStates(target, visited);
            }
        }
        for (State target : state.epsilonTransitions) {
            collectStates(target, visited);
        }
    }
 // Add these methods and classes within your NFABuilder class, after your current code.

    // Represents a state in the DFA. Each DFA state is a set of NFA states.
    public static class DFAState {
        public Set<State> nfaStates;  // The subset of NFA states represented by this DFA state.
        public int id;
        public boolean isAccept;
        public Map<Character, DFAState> transitions;  // Transitions on input symbols.

        public DFAState(Set<State> nfaStates, int id) {
            this.nfaStates = nfaStates;
            this.id = id;
            this.transitions = new HashMap<>();
            // Mark as accepting if any NFA state in the set is accepting.
            for (State s : nfaStates) {
                if (s.isAccept) {
                    this.isAccept = true;
                    break;
                }
            }
        }
    }

    // Compute the ε-closure of a set of NFA states.
    public static Set<State> epsilonClosure(Set<State> states) {
        Set<State> closure = new HashSet<>(states);
        Stack<State> stack = new Stack<>();
        for (State s : states) {
            stack.push(s);
        }
        while (!stack.isEmpty()) {
            State s = stack.pop();
            for (State t : s.epsilonTransitions) {
                if (!closure.contains(t)) {
                    closure.add(t);
                    stack.push(t);
                }
            }
        }
        return closure;
    }

    // Given a set of NFA states and an input symbol, compute the set of states reachable 
    // by that symbol (without taking ε-transitions).
    public static Set<State> move(Set<State> states, char symbol) {
        Set<State> result = new HashSet<>();
        for (State s : states) {
            if (s.transitions.containsKey(symbol)) {
                result.addAll(s.transitions.get(symbol));
            }
            // Optionally, if you want your wildcard transitions to match any symbol,
            // check for your special WILDCARD_MARKER here.
            if (s.transitions.containsKey(WILDCARD_MARKER)) {
                result.addAll(s.transitions.get(WILDCARD_MARKER));
            }
        }
        return result;
    }

    // Helper method to create a unique key for a set of NFA states (used to detect duplicate DFA states).
    public static String getStatesKey(Set<State> states) {
        List<Integer> ids = new ArrayList<>();
        for (State s : states) {
            ids.add(s.id);
        }
        Collections.sort(ids);
        StringBuilder sb = new StringBuilder();
        for (Integer id : ids) {
            sb.append(id).append(",");
        }
        return sb.toString();
    }

    // Compute the alphabet (set of input symbols) from the NFA.
    public static Set<Character> getAlphabet(NFA nfa) {
        Set<Character> alphabet = new HashSet<>();
        Set<State> visited = new HashSet<>();
        Stack<State> stack = new Stack<>();
        stack.push(nfa.start);
        while (!stack.isEmpty()) {
            State s = stack.pop();
            if (visited.contains(s))
                continue;
            visited.add(s);
            for (Map.Entry<Character, List<State>> entry : s.transitions.entrySet()) {
                char sym = entry.getKey();
                // Exclude any special markers (like ε).
                alphabet.add(sym);
                for (State t : entry.getValue()) {
                    stack.push(t);
                }
            }
            for (State t : s.epsilonTransitions) {
                stack.push(t);
            }
        }
        return alphabet;
    }

    // Convert an NFA to a DFA using the subset construction algorithm.
    public static DFAState convertNFAtoDFA(NFA nfa) {
        int dfaStateIdCounter = 0;
        // Get the ε-closure of the NFA's start state.
        Set<State> startSet = epsilonClosure(new HashSet<>(Arrays.asList(nfa.start)));
        DFAState startDFA = new DFAState(startSet, dfaStateIdCounter++);
        
        // Map from the unique key (sorted state IDs) to DFAState.
        Map<String, DFAState> dfaStates = new HashMap<>();
        String startKey = getStatesKey(startSet);
        dfaStates.put(startKey, startDFA);
        
        // Queue for unmarked DFA states.
        Queue<DFAState> unmarked = new LinkedList<>();
        unmarked.add(startDFA);
        
        // Get the input alphabet.
        Set<Character> alphabet = getAlphabet(nfa);
        // Remove ε if present. (ε transitions are handled via epsilonClosure.)
        // (Assuming you use a separate mechanism for ε.)
        
        while (!unmarked.isEmpty()) {
            DFAState dstate = unmarked.poll();
            // For each symbol in the alphabet:
            for (Character symbol : alphabet) {
                // Compute the set of NFA states reachable on this symbol.
                Set<State> moveSet = move(dstate.nfaStates, symbol);
                if (moveSet.isEmpty())
                    continue;
                // Then take the ε-closure of the resulting set.
                Set<State> closure = epsilonClosure(moveSet);
                String key = getStatesKey(closure);
                DFAState dtarget = dfaStates.get(key);
                if (dtarget == null) {
                    dtarget = new DFAState(closure, dfaStateIdCounter++);
                    dfaStates.put(key, dtarget);
                    unmarked.add(dtarget);
                }
                dstate.transitions.put(symbol, dtarget);
            }
        }
        return startDFA;
    }

    // Display the DFA transition table.
    public static void displayDFATransitionTable(DFAState start) {
        Set<Integer> visited = new HashSet<>();
        displayDFAState(start, visited);
    }

    private static void displayDFAState(DFAState state, Set<Integer> visited) {
        if (visited.contains(state.id))
            return;
        visited.add(state.id);
        System.out.print("DFA State " + state.id + " [");
        List<Integer> ids = new ArrayList<>();
        for (State s : state.nfaStates)
            ids.add(s.id);
        Collections.sort(ids);
        for (Integer id : ids)
            System.out.print(id + " ");
        System.out.print("]");
        if (state.isAccept)
            System.out.print(" (Accept)");
        System.out.println();
        for (Map.Entry<Character, DFAState> entry : state.transitions.entrySet()) {
            char symbol = entry.getKey();
            String symStr;
            if (symbol == WHITESPACE_MARKER)
                symStr = "\\s";
            else if (symbol == NEWLINE_MARKER)
                symStr = "\\n";
            else if (symbol == WILDCARD_MARKER)
                symStr = ".(wildcard)";
            else
                symStr = Character.toString(symbol);
            System.out.println("  --" + symStr + "--> DFA State " + entry.getValue().id);
            displayDFAState(entry.getValue(), visited);
        }
    }

    public static int countDFAStates(DFAState start) {
        Set<Integer> visited = new HashSet<>();
        collectDFAStates(start, visited);
        return visited.size();
    }

    private static void collectDFAStates(DFAState state, Set<Integer> visited) {
        if (visited.contains(state.id))
            return;
        visited.add(state.id);
        for (DFAState target : state.transitions.values()) {
            collectDFAStates(target, visited);
        }
    }

   
}



