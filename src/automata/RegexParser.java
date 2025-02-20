package automata;

import java.util.HashSet;
import java.util.Set;

// --- Abstract Syntax Tree (AST) for regex ---


class LiteralNode extends RegexNode {
    public char c;
    public LiteralNode(char c) { this.c = c; }
}

class ConcatNode extends RegexNode {
    public RegexNode left, right;
    public ConcatNode(RegexNode left, RegexNode right) {
        this.left = left; this.right = right;
    }
}

class UnionNode extends RegexNode {
    public RegexNode left, right;
    public UnionNode(RegexNode left, RegexNode right) {
        this.left = left; this.right = right;
    }
}

class StarNode extends RegexNode {
    public RegexNode node;
    public StarNode(RegexNode node) { this.node = node; }
}

class PlusNode extends RegexNode {
    public RegexNode node;
    public PlusNode(RegexNode node) { this.node = node; }
}

class OptionalNode extends RegexNode {
    public RegexNode node;
    public OptionalNode(RegexNode node) { this.node = node; }
}

class CharClassNode extends RegexNode {
    public Set<Character> chars;
    public CharClassNode(Set<Character> chars) { this.chars = chars; }
}

// --- RegexParser using recursive descent ---
public class RegexParser {
    private String regex;
    private int pos;
    
    public RegexParser(String regex) {
        this.regex = regex;
        this.pos = 0;
    }
    
    public RegexNode parse() {
        RegexNode node = parseUnion();
        if (pos < regex.length()) {
            throw new RuntimeException("Unexpected character at position " + pos);
        }
        return node;
    }
    
    // union : concat ('|' concat)*
    private RegexNode parseUnion() {
        RegexNode left = parseConcat();
        while (pos < regex.length() && peek() == '|') {
            consume(); // skip '|'
            RegexNode right = parseConcat();
            left = new UnionNode(left, right);
        }
        return left;
    }
    
    // concat : factor+
    private RegexNode parseConcat() {
        RegexNode left = parseFactor();
        while (pos < regex.length() && peek() != '|' && peek() != ')') {
            RegexNode right = parseFactor();
            left = new ConcatNode(left, right);
        }
        return left;
    }
    
    // factor : primary ('*' | '+' | '?')*
    private RegexNode parseFactor() {
        RegexNode node = parsePrimary();
        while (pos < regex.length()) {
            char c = peek();
            if (c == '*') { consume(); node = new StarNode(node); }
            else if (c == '+') { consume(); node = new PlusNode(node); }
            else if (c == '?') { consume(); node = new OptionalNode(node); }
            else break;
        }
        return node;
    }
    
    // primary : literal | '(' union ')' | charclass
    private RegexNode parsePrimary() {
        if (pos >= regex.length())
            throw new RuntimeException("Unexpected end of regex");
        char c = peek();
        if (c == '(') {
            consume(); // '('
            RegexNode node = parseUnion();
            if (pos >= regex.length() || consume() != ')')
                throw new RuntimeException("Missing closing parenthesis at position " + pos);
            return node;
        } else if (c == '[') {
            return parseCharClass();
        } else if (c == '\\') {
            consume(); // skip '\'
            if (pos >= regex.length())
                throw new RuntimeException("Escape at end of regex");
            return new LiteralNode(translateEscape(consume()));
        } else {
            return new LiteralNode(consume());
        }
    }
    
    private RegexNode parseCharClass() {
        consume(); // '['
        Set<Character> set = new HashSet<>();
        // For simplicity, we do not implement negation.
        while (pos < regex.length() && peek() != ']') {
            char start = consume();
            if (peek() == '-' && pos + 1 < regex.length() && regex.charAt(pos+1) != ']') {
                consume(); // '-'
                char end = consume();
                for (char ch = start; ch <= end; ch++) {
                    set.add(ch);
                }
            } else {
                set.add(start);
            }
        }
        if (pos >= regex.length() || consume() != ']')
            throw new RuntimeException("Unterminated character class at position " + pos);
        return new CharClassNode(set);
    }
    
    private char peek() {
        return regex.charAt(pos);
    }
    
    private char consume() {
        return regex.charAt(pos++);
    }
    
    private char translateEscape(char esc) {
        if (esc == 'n') return '\n';
        if (esc == 't') return '\t';
        // Add more escapes as needed.
        return esc;
    }
}
