package com.azuredoom.classescore.exceptions;

public class ClassesCoreException extends RuntimeException {

    public ClassesCoreException(String failedToCloseH2Connection, Exception e) {
        super(failedToCloseH2Connection, e);
    }

    public ClassesCoreException(String failedToCloseH2Connection) {
        super(failedToCloseH2Connection);
    }
}
