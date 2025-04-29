# MyLang Lexical Analyzer

This project implements the lexical analysis (tokenization) phase for a simple language, MyLang. It converts MyLang source code into a stream of tokens, handling comments, whitespace, and recognizing different language elements.

It uses a hybrid approach: ad-hoc logic for comments, whitespace, strings, and characters, and a DFA built from regex for other tokens.

## MyLang Language Lexical Rules

This outlines the structure of MyLang source code at the token level.

### Keywords

Reserved words with special meaning:
`global`, `local`, `const`, `boolean`, `integer`, `decimal`, `character`, `if`, `else`, `for`, `while`, `return`, `true`, `false`, `read`, `write`

### Identifiers

Names for variables, functions, etc.
* Must start with a lowercase letter (`a-z`).
* Must contain *only* lowercase letters (`a-z`).
* Cannot be a keyword.

**Examples:** `counter`, `calculate`, `x` (Valid). `PI`, `myVar1` (Invalid).

### Literals

Fixed values in code:

* **Integer:** Digits `0-9`, optional leading `-`. E.g., `10`, `-5`, `0`.
* **Decimal:** Optional `-`, digits, `.`, digits. Both sides of `.` require digits. E.g., `3.14`, `-1.0`.
* **Boolean:** The keywords `true` or `false`.
* **Character:** Single character or escape in single quotes (`'`). E.g., `'a'`, `'\n'`, `'\\'`. Supported escapes: `\\`, `\'`, `\n`, `\t`, `\r`, `\f`. Must contain exactly one element.
* **String:** Characters in double quotes (`"`). E.g., `"hello"`, `"with\\"quote"`. Supported escapes: `\\`, `\"`, `\n`, `\t`, `\r`, `\f`. Cannot span lines directly.

### Operators

`+`, `-`, `*`, `/`, `%`, `^`, `=`, `==`, `<=`, `>=`, `<`, `>`

### Delimiters

`(`, `)`, `{`, `}`, `;`, `,`

### Comments

Ignored by the lexer.
* Single-line: Start with `//`, goes to end of line.
* Multi-line: Start with `/*`, end with `*/`.

### Whitespace

Spaces, tabs, newlines, carriage returns, form feeds. Ignored unless in literals. Separates tokens.

## Project Structure

Basic structure: `src/automata/`, `src/compiler/`, `src/lexer/`, `src/token/`. Main class is `src.compiler.CompilerFrontEnd`.

## Prerequisites

* Java Development Kit (JDK) 8+.

## Building

```bash
javac src/automata/*.java src/compiler/*.java src/lexer/*.java src/token/*.java
