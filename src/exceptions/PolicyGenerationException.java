package exceptions;

public class PolicyGenerationException extends Exception{
    public PolicyGenerationException(String message) {
        super(message);
    }

    public PolicyGenerationException(String message, Exception ex) {
        super(message, ex);
    }
}
