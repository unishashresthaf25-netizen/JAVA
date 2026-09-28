package com.hospital.exception;

/**
 * Thrown when attempting to schedule a doctor for an appointment
 * that overlaps with an existing scheduled appointment for that doctor.
 */
public class DoubleBookingException extends Exception {
    public DoubleBookingException(String message) {
        super(message);
    }
}
