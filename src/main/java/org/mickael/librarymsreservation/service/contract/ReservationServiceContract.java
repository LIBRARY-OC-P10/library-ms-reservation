package org.mickael.librarymsreservation.service.contract;

import org.mickael.librarymsreservation.model.Reservation;

import java.time.LocalDate;
import java.util.List;

public interface ReservationServiceContract {

    List<Reservation> findAll();
    Reservation findById(Integer id);
    Reservation save(Reservation reservation, List<LocalDate> localDateList);

    void updateResaBookId(Integer bookId, Integer numberOfCopies);

    void updateDateResaBookId(Integer bookId, List<LocalDate> listReturnLoanDate);

    void delete(Integer id, List<LocalDate> localDateList);

    List<Reservation> findAllByCustomerId(Integer customerId);

    boolean checkIfReservationExistForCustomerIdAndBookId(Integer customerId, Integer bookId);
    Reservation findByCustomerIdAndBookId(Integer customerId, Integer bookId);


}
