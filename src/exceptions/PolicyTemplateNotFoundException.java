package exceptions;

public class PolicyTemplateNotFoundException extends Exception {
    public PolicyTemplateNotFoundException(String message) {
        super(message);
    }

    public PolicyTemplateNotFoundException(String message, Exception ex) {
        super(message, ex);
    }
}
