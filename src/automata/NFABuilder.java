package automata;

import java.util.*;

public class NFABuilder {

    public static int stateIdCounter = 0;
    
    // Define our special markers.
    public static final char WHITESPACE_MARKER = '\u0001';
    public static final char NEWLINE_MARKER = '\u0002';
    public static final char WILDCARD_MARKER = '\u0003';
    public static final char CONCAT_OP = '#'; // (Used internally in our engine)
    
    // --- NFA state and fragment classes ---
    public static class State {
        public int id;
        public Map<Character, List<State>> transitions = new HashMap<>();
        public List<State> epsilonTransitions = new ArrayList<>();
        public boolean isAccept = false;
        public String tokenType = null;
        
        public State() { id = stateIdCounter++; }
        
        public void addTransition(char symbol, State target) {
            transitions.computeIfAbsent(symbol, k -> new ArrayList<>()).add(target);
        }
        
        public void addEpsilon(State target) {
            epsilonTransitions.add(target);
        }
    }
    
    public static class NFA {
        public State start;
        public State accept; // May be null for a master NFA.
        public NFA(State start, State accept) {
            this.start = start;
            this.accept = accept;
        }
    }
    
    // --- Extended Regex-to-NFA conversion ---
    // This uses the RegexParser (above) to produce an AST and then builds an NFA via Thompson’s construction.
    public static class RegexToNFA {
        public static NFA convert(String regex) {
            RegexParser parser = new RegexParser(regex);
            RegexNode ast = parser.parse();
            return regexNodeToNFA(ast);
        }
        
        private static NFA regexNodeToNFA(RegexNode node) {
            if (node instanceof LiteralNode) {
                LiteralNode lit = (LiteralNode) node;
                State start = new State();
                State accept = new State();
                start.addTransition(lit.c, accept);
                return new NFA(start, accept);
            } else if (node instanceof CharClassNode) {
                CharClassNode cc = (CharClassNode) node;
                State start = new State();
                State accept = new State();
                for (char c : cc.chars) {
                    start.addTransition(c, accept);
                }
                return new NFA(start, accept);
            } else if (node instanceof ConcatNode) {
                ConcatNode con = (ConcatNode) node;
                NFA left = regexNodeToNFA(con.left);
                NFA right = regexNodeToNFA(con.right);
                left.accept.addEpsilon(right.start);
                return new NFA(left.start, right.accept);
            } else if (node instanceof UnionNode) {
                UnionNode un = (UnionNode) node;
                NFA left = regexNodeToNFA(un.left);
                NFA right = regexNodeToNFA(un.right);
                State start = new State();
                State accept = new State();
                start.addEpsilon(left.start);
                start.addEpsilon(right.start);
                left.accept.addEpsilon(accept);
                right.accept.addEpsilon(accept);
                return new NFA(start, accept);
            } else if (node instanceof StarNode) {
                StarNode star = (StarNode) node;
                NFA inner = regexNodeToNFA(star.node);
                State start = new State();
                State accept = new State();
                start.addEpsilon(inner.start);
                start.addEpsilon(accept);
                inner.accept.addEpsilon(inner.start);
                inner.accept.addEpsilon(accept);
                return new NFA(start, accept);
            } else if (node instanceof PlusNode) {
                PlusNode plus = (PlusNode) node;
                NFA inner = regexNodeToNFA(plus.node);
                State start = new State();
                State accept = new State();
                start.addEpsilon(inner.start);
                inner.accept.addEpsilon(inner.start);
                inner.accept.addEpsilon(accept);
                return new NFA(start, accept);
            } else if (node instanceof OptionalNode) {
                OptionalNode opt = (OptionalNode) node;
                NFA inner = regexNodeToNFA(opt.node);
                State start = new State();
                State accept = new State();
                start.addEpsilon(inner.start);
                start.addEpsilon(accept);
                inner.accept.addEpsilon(accept);
                return new NFA(start, accept);
            }
            throw new RuntimeException("Unsupported regex node");
        }
    }
    
    // Build a master NFA by creating a new start state with ε–transitions to every token’s NFA.
    public static NFA combineNFAs(List<NFA> nfaList) {
        State masterStart = new State();
        for (NFA nfa : nfaList) {
            masterStart.addEpsilon(nfa.start);
        }
        return new NFA(masterStart, null);
    }
    
    // --- DFA conversion via subset construction ---
    public static void displayTransitionTable(State start) {
        Set<Integer> visited = new HashSet<>();
        displayState(start, visited);
    }
    
    private static void displayState(State state, Set<Integer> visited) {
        if (visited.contains(state.id)) return;
        visited.add(state.id);
        for (Map.Entry<Character, List<State>> entry : state.transitions.entrySet()) {
            char symbol = entry.getKey();
            String symStr = (symbol == WHITESPACE_MARKER) ? "\\s" :
                            (symbol == NEWLINE_MARKER) ? "\\n" :
                            (symbol == WILDCARD_MARKER) ? ".(wildcard)" : 
                            Character.toString(symbol);
            for (State target : entry.getValue()) {
                System.out.println("State " + state.id + " --" + symStr + "--> State " + target.id);
            }
        }
        for (State target : state.epsilonTransitions) {
            System.out.println("State " + state.id + " --ε--> State " + target.id);
        }
        for (List<State> targets : state.transitions.values()) {
            for (State target : targets)
                displayState(target, visited);
        }
        for (State target : state.epsilonTransitions)
            displayState(target, visited);
    }
    
    public static int countStates(State start) {
        Set<Integer> visited = new HashSet<>();
        collectStates(start, visited);
        return visited.size();
    }
    
    private static void collectStates(State state, Set<Integer> visited) {
        if (visited.contains(state.id)) return;
        visited.add(state.id);
        for (List<State> targets : state.transitions.values()) {
            for (State target : targets)
                collectStates(target, visited);
        }
        for (State target : state.epsilonTransitions)
            collectStates(target, visited);
    }
    
    public static class DFAState {
        public Set<State> nfaStates;
        public int id;
        public boolean isAccept;
        public Map<Character, DFAState> transitions = new HashMap<>();
        public DFAState(Set<State> nfaStates, int id) {
            this.nfaStates = nfaStates;
            this.id = id;
            for (State s : nfaStates)
                if (s.isAccept) { isAccept = true; break; }
        }
    }
    
    public static Set<State> epsilonClosure(Set<State> states) {
        Set<State> closure = new HashSet<>(states);
        Stack<State> stack = new Stack<>();
        stack.addAll(states);
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
    
    public static Set<State> move(Set<State> states, char symbol) {
        Set<State> result = new HashSet<>();
        for (State s : states) {
            if (s.transitions.containsKey(symbol))
                result.addAll(s.transitions.get(symbol));
            if (s.transitions.containsKey(WILDCARD_MARKER))
                result.addAll(s.transitions.get(WILDCARD_MARKER));
        }
        return result;
    }
    
    public static String getStatesKey(Set<State> states) {
        List<Integer> ids = new ArrayList<>();
        for (State s : states) {
            ids.add(s.id);
        }
        Collections.sort(ids);
        StringBuilder sb = new StringBuilder();
        for (Integer id : ids)
            sb.append(id).append(",");
        return sb.toString();
    }
    
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
                alphabet.add(entry.getKey());
                stack.addAll(entry.getValue());
            }
            stack.addAll(s.epsilonTransitions);
        }
        return alphabet;
    }
    
    public static DFAState convertNFAtoDFA(NFA nfa) {
        int dfaStateIdCounter = 0;
        Set<State> startSet = epsilonClosure(new HashSet<>(Arrays.asList(nfa.start)));
        DFAState startDFA = new DFAState(startSet, dfaStateIdCounter++);
        Map<String, DFAState> dfaStates = new HashMap<>();
        String startKey = getStatesKey(startSet);
        dfaStates.put(startKey, startDFA);
        Queue<DFAState> unmarked = new LinkedList<>();
        unmarked.add(startDFA);
        Set<Character> alphabet = getAlphabet(nfa);
        while (!unmarked.isEmpty()) {
            DFAState dstate = unmarked.poll();
            for (Character symbol : alphabet) {
                Set<State> moveSet = move(dstate.nfaStates, symbol);
                if (moveSet.isEmpty()) continue;
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
    
    public static void displayDFATransitionTable(DFAState start) {
        Set<Integer> visited = new HashSet<>();
        displayDFAState(start, visited);
    }
    
    private static void displayDFAState(DFAState state, Set<Integer> visited) {
        if (visited.contains(state.id)) return;
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
            System.out.println("  --" + entry.getKey() + "--> DFA State " + entry.getValue().id);
            displayDFAState(entry.getValue(), visited);
        }
    }
    
    public static int countDFAStates(DFAState start) {
        Set<Integer> visited = new HashSet<>();
        collectDFAStates(start, visited);
        return visited.size();
    }
    
    private static void collectDFAStates(DFAState state, Set<Integer> visited) {
        if (visited.contains(state.id)) return;
        visited.add(state.id);
        for (DFAState target : state.transitions.values())
            collectDFAStates(target, visited);
    }
}
