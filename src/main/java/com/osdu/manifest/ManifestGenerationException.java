package com.osdu.manifest;

public class ManifestGenerationException extends RuntimeException {

    public ManifestGenerationException(String message) {
        super(message);
    }

    public ManifestGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
