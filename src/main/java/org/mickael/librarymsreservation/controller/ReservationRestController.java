package org.mickael.librarymsreservation.controller;

import org.mickael.librarymsreservation.exception.NotFoundException;
import org.mickael.librarymsreservation.model.Reservation;
import org.mickael.librarymsreservation.proxy.FeignBookProxy;
import org.mickael.librarymsreservation.proxy.FeignLoanProxy;
import org.mickael.librarymsreservation.service.contract.ReservationServiceContract;
import org.mickael.librarymsreservation.utils.HandlerToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("isAuthenticated()")
public class ReservationRestController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationRestController.class);
    private final ReservationServiceContract reservationServiceContract;
    private final FeignLoanProxy feignLoanProxy;
    private final FeignBookProxy feignBookProxy;

    @Autowired
    public ReservationRestController(ReservationServiceContract reservationServiceContract, FeignLoanProxy feignLoanProxy, FeignBookProxy feignBookProxy) {
        this.reservationServiceContract = reservationServiceContract;
        this.feignLoanProxy = feignLoanProxy;
        this.feignBookProxy = feignBookProxy;
    }

    @GetMapping
    public List<Reservation> getReservations(){
        List<Reservation> reservations = reservationServiceContract.findAll();
        return reservations;
    }

    @GetMapping("/book/{bookId}")
    public List<Reservation> getReservationsByBookId(@PathVariable Integer bookId){
        List<Reservation> reservations = reservationServiceContract.findAllByBookId(bookId);
        return reservations;
    }

    @GetMapping("/customer/{customerId}")
    public List<Reservation> getCustomerReservations(@PathVariable Integer customerId){
        try {
            return reservationServiceContract.findAllByCustomerId(customerId);
        } catch (NotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No reservation found", ex);
        }
    }

    @PostMapping
    public Reservation createReservation(@RequestBody Reservation reservation, @RequestHeader("Authorization") String accessToken){
        List<LocalDate> listReturnLoanDate = feignLoanProxy.getSoonReturned(reservation.getBookId(), HandlerToken.formatToken(accessToken));
        Integer numberOfCopies = feignBookProxy.numberOfCopyForBook(reservation.getBookId(), HandlerToken.formatToken(accessToken));
        Reservation newResa = reservationServiceContract.save(reservation, listReturnLoanDate, numberOfCopies);
        return newResa;
    }


    @DeleteMapping("/customer/{customerId}/book/{bookId}/")
    public void deleteReservationAfterLoan(@PathVariable Integer customerId, @PathVariable Integer bookId, @RequestHeader("Authorization") String accessToken){
        reservationServiceContract.delete(
                reservationServiceContract.findByCustomerIdAndBookId(customerId, bookId).getId(),
                feignLoanProxy.getSoonReturned(bookId, HandlerToken.formatToken(accessToken)));
    }

    @DeleteMapping("/{id}")
    public void deleteReservationAfterTwoDays(@PathVariable Integer id, @RequestHeader("Authorization") String accessToken){
        reservationServiceContract.delete(id,
                feignLoanProxy.getSoonReturned(reservationServiceContract.findById(id).getBookId(), HandlerToken.formatToken(accessToken)));
    }

    @GetMapping("/customer/{customerId}/book/{bookId}/")
    public boolean checkIfReservationExist(@PathVariable Integer customerId, @PathVariable Integer bookId){
        return reservationServiceContract.checkIfReservationExistForCustomerIdAndBookId(customerId, bookId);
    }

    @PutMapping("/book/{bookId}")
    public void updateReservation(@PathVariable Integer bookId, @RequestHeader("Authorization") String accessToken){
        reservationServiceContract.updateResaBookId(bookId,feignBookProxy.numberOfCopyAvailableForBook(bookId, HandlerToken.formatToken(accessToken)));
    }

    @PutMapping("/book/{bookId}/refresh")
    public void updateDateReservation(@PathVariable Integer bookId, @RequestHeader("Authorization") String accessToken){
        reservationServiceContract.updateDateResaBookId(bookId, feignLoanProxy.getSoonReturned(bookId, HandlerToken.formatToken(accessToken)));
    }


}
