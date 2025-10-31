// ...existing code...
package pl.matgwiazda.exception;

/**
 * Custom runtime exception for OpenRouter/AI integration errors.
 */
public class OpenRouterException extends RuntimeException {
    public OpenRouterException(String message) {
        super(message);
    }

    public OpenRouterException(String message, Throwable cause) {
        super(message, cause);
    }
}

