package com.azuredoom.classescore.exceptions;

/**
 * A custom exception class that represents errors encountered within the ClassesCore module. This exception extends
 * {@link RuntimeException} and is primarily used to encapsulate issues arising from operations such as closing
 * resources or unexpected failures in the system.
 * <p>
 * This class provides constructors to include a custom error message and an optional underlying {@link Exception}
 * cause.
 */
public class ClassesCoreException extends RuntimeException {

    public ClassesCoreException(String failedToCloseH2Connection, Exception e) {
        super(failedToCloseH2Connection, e);
    }

    public ClassesCoreException(String failedToCloseH2Connection) {
        super(failedToCloseH2Connection);
    }
}
