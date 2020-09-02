package org.mickael.librarymsreservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Reservation Not Found")
public class ReservationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    public ReservationNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
