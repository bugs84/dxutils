package cz.d1x.dxutils.io;

/**
 * Runtime exception that is related to exception states related to input/output operations.
 * Typically wraps checked {@link java.io.IOException} to allow "prettier" work with API without need to catch exception.
 *
 * @author Zdenek Obst, zdenek.obst-at-gmail.com
 */
public class IORuntimeException extends RuntimeException {

    public IORuntimeException(String message) {
        super(message);
    }

    public IORuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public IORuntimeException(Throwable cause) {
        super(cause);
    }
}
