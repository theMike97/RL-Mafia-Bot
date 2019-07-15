package exceptions;

public class LobbyTooSmallException extends Exception {
    public LobbyTooSmallException(String errorMessage) {
        super(errorMessage);
    }
}