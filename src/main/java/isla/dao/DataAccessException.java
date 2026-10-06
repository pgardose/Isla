package isla.dao;

/** Unchecked wrapper for SQL failures so game code is not littered with try/catch. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
