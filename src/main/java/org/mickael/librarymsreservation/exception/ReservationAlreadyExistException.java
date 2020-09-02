package org.mickael.librarymsreservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Reservation Already exist")
public class ReservationAlreadyExistException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    public ReservationAlreadyExistException(String errorMessage) {
        super(errorMessage);
    }
}
