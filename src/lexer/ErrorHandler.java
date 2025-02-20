package lexer;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private List<String> errors = new ArrayList<>();
    public void addError(String errorMessage, int line) {
        errors.add("Error at line " + line + ": " + errorMessage);
    }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public void printErrors() {
        for (String error : errors)
            System.err.println(error);
    }
}
