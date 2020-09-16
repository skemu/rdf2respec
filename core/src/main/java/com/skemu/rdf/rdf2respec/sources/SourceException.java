package com.skemu.rdf.rdf2respec.sources;

public class SourceException extends RuntimeException {

    public SourceException(String message) {
        super(message);
    }

    public SourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
