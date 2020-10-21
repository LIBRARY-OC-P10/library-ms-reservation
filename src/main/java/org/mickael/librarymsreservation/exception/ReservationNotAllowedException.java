package org.mickael.librarymsreservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Reservation impossible. Contactez la bibliothèque. Merci.")
public class ReservationNotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    public ReservationNotAllowedException(String errorMessage) {
        super(errorMessage);
    }
}
