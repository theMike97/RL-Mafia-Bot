package exceptions;

public class LobbyTooBigException extends Exception {
    public LobbyTooBigException(String errorMessage) {
        super(errorMessage);
    }
}