package org.mickael.librarymsreservation.controller;

import org.mickael.librarymsreservation.model.Reservation;
import org.mickael.librarymsreservation.proxy.FeignBookProxy;
import org.mickael.librarymsreservation.proxy.FeignLoanProxy;
import org.mickael.librarymsreservation.service.contract.ReservationServiceContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
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

    @GetMapping("/customer/{customerId}")
    public List<Reservation> getCustomerReservations(@PathVariable Integer customerId){
        List<Reservation> reservations = reservationServiceContract.findAllByCustomerId(customerId);
        return reservations;
    }

    @PostMapping
    public Reservation createReservation(Reservation reservation){
        List<LocalDate> listReturnLoanDate = feignLoanProxy.getSoonReturned(reservation.getBookId());
        return reservationServiceContract.save(reservation, listReturnLoanDate);
    }


    @DeleteMapping("/customer/{customerId}/book/{bookId}/")
    public void deleteReservationAfterLoan(@PathVariable Integer customerId, @PathVariable Integer bookId){
        reservationServiceContract.delete(
                reservationServiceContract.findByCustomerIdAndBookId(customerId, bookId).getId(),
                feignLoanProxy.getSoonReturned(bookId));
    }

    @DeleteMapping("/{id}")
    public void deleteReservationAfterTwoDays(@PathVariable Integer id){
        reservationServiceContract.delete(id,
                feignLoanProxy.getSoonReturned(reservationServiceContract.findById(id).getBookId()));
    }

    @GetMapping("/customer/{customerId}/book/{bookId}/")
    public boolean checkIfReservationExist(@PathVariable Integer customerId, @PathVariable Integer bookId){
        return reservationServiceContract.checkIfReservationExistForCustomerIdAndBookId(customerId, bookId);
    }

    @PutMapping("/book/{bookId}")
    public void updateReservation(@PathVariable Integer bookId){
        reservationServiceContract.updateResaBookId(bookId,
                feignBookProxy.numberOfCopyAvailableForBook(bookId));
    }

    @PutMapping("/book/{bookId}/refresh")
    public void updateDateReservation(@PathVariable Integer bookId){
        reservationServiceContract.updateDateResaBookId(bookId, feignLoanProxy.getSoonReturned(bookId));
    }


}
