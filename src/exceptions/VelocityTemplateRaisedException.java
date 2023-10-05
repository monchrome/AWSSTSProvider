package exceptions;
import java.lang.Exception;
public class VelocityTemplateRaisedException extends RuntimeException{
    public VelocityTemplateRaisedException() {
    }

    public VelocityTemplateRaisedException(String message) {
        super(message);
    }
    public void logCustomException(String message) {
        throw new VelocityTemplateRaisedException(message);
    }
}
