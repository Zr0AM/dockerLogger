package vault.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultTokenException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultTokenException.class);

    public VaultTokenException(String message) {
        super(message);
        LOGGER.error(message);
    }

}
