package org.mickael.librarymsreservation.controller;

import org.mickael.librarymsreservation.exception.NotFoundException;
import org.mickael.librarymsreservation.model.Reservation;
import org.mickael.librarymsreservation.proxy.FeignBookProxy;
import org.mickael.librarymsreservation.proxy.FeignLoanProxy;
import org.mickael.librarymsreservation.service.contract.ReservationServiceContract;
import org.mickael.librarymsreservation.utils.HandlerToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("isAuthenticated()")
public class ReservationRestController {

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
        return reservationServiceContract.findAll();
    }

    @GetMapping("/{id}")
    public Reservation getReservation(@PathVariable Integer id){
        return reservationServiceContract.findById(id);
    }

    @GetMapping("/book/{bookId}")
    public List<Reservation> getReservationsByBookId(@PathVariable Integer bookId){
        return reservationServiceContract.findAllByBookId(bookId);
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
    @ResponseStatus(HttpStatus.CREATED)
    public Reservation createReservation(@RequestBody Reservation reservation, @RequestHeader("Authorization") String accessToken){
        List<LocalDate> listReturnLoanDate = feignLoanProxy.getSoonReturned(reservation.getBookId(), HandlerToken.formatToken(accessToken));
        Integer numberOfCopies = feignBookProxy.numberOfCopyForBook(reservation.getBookId(), HandlerToken.formatToken(accessToken));
        Integer copiesAvailable = feignBookProxy.numberOfCopyAvailableForBook(reservation.getBookId(), HandlerToken.formatToken(accessToken));
        if (feignLoanProxy.checkIfLoanExistForCustomerIdAndBookId(reservation.getCustomerId(), reservation.getBookId(), HandlerToken.formatToken(accessToken))){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation not allowed, loan ongoing");
        } else {
            return reservationServiceContract.save(reservation, listReturnLoanDate, numberOfCopies, copiesAvailable);
        }
    }


    @DeleteMapping("/customer/{customerId}/book/{bookId}")
    public void deleteReservationAfterLoan(@PathVariable Integer customerId, @PathVariable Integer bookId, @RequestHeader("Authorization") String accessToken){
        reservationServiceContract.delete(
                reservationServiceContract.findByCustomerIdAndBookId(customerId, bookId).getId(),
                feignLoanProxy.getSoonReturned(bookId, HandlerToken.formatToken(accessToken)));
    }

    @DeleteMapping("/{reservationId}")
    public void deleteReservationAfterTwoDays(@PathVariable Integer reservationId, @RequestHeader("Authorization") String accessToken){
        Integer bookId = reservationServiceContract.findById(reservationId).getBookId();
        reservationServiceContract.delete(reservationId,
                feignLoanProxy.getSoonReturned(bookId, HandlerToken.formatToken(accessToken)));
    }

    @GetMapping("/customer/{customerId}/book/{bookId}")
    public boolean checkIfReservationExist(@PathVariable Integer customerId, @PathVariable Integer bookId){
        return reservationServiceContract.checkIfReservationExistForCustomerIdAndBookId(customerId, bookId);
    }

    @PutMapping
    public void updateReservation(){
        reservationServiceContract.updateReservationsAndSendMail();
    }

    @PutMapping("/book/{bookId}/refresh")
    public void updateDateReservation(@PathVariable Integer bookId, @RequestHeader("Authorization") String accessToken){
        reservationServiceContract.updateDateResaBookId(bookId, feignLoanProxy.getSoonReturned(bookId, HandlerToken.formatToken(accessToken)));
    }


}
