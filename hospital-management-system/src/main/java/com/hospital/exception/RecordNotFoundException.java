package com.hospital.exception;

/**
 * Thrown when a requested patient, doctor, or appointment record
 * does not exist in the database.
 */
public class RecordNotFoundException extends Exception {
    public RecordNotFoundException(String message) {
        super(message);
    }
}
