package org.mickael.librarymsreservation.service.impl;

import org.mickael.librarymsreservation.exception.ReservationAlreadyExistException;
import org.mickael.librarymsreservation.exception.ReservationNotFoundException;
import org.mickael.librarymsreservation.model.Reservation;
import org.mickael.librarymsreservation.repository.ReservationRepository;
import org.mickael.librarymsreservation.service.contract.ReservationServiceContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReservationServiceImpl implements ReservationServiceContract {

    private final ReservationRepository reservationRepository;
    private JavaMailSender javaMailSender;
    private SimpleMailMessage preConfiguredMessage;

    @Autowired
    public ReservationServiceImpl(ReservationRepository reservationRepository, JavaMailSender javaMailSender, SimpleMailMessage preConfiguredMessage) {
        this.reservationRepository = reservationRepository;
        this.javaMailSender = javaMailSender;
        this.preConfiguredMessage = preConfiguredMessage;
    }


    @Override
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    @Override
    public Reservation findById(Integer reservationId) {
        Optional<Reservation> optionalReservation = reservationRepository.findById(reservationId);
        if (!optionalReservation.isPresent()){
            throw new ReservationNotFoundException("Reservation not Found");
        }
        return optionalReservation.get();
    }

    @Override
    public Reservation save(Reservation reservation, List<LocalDate> listReturnLoanDate, Integer numberOfCopies) {
        Reservation reservationToSave = new Reservation();

        //check if the customer already had a reservation
        Reservation reservationInBdd = reservationRepository.findByCustomerIdAndBookId(reservation.getCustomerId(), reservation.getBookId());
        if (reservationInBdd != null){
            throw new ReservationAlreadyExistException("Vous avez déjà une réservation pour ce livre.");
        }

        //get all the reservation for the book to know the position in the list
        List<Reservation> reservations = reservationRepository.findAllByBookId(reservation.getBookId());

        //set last position in the reservation list
        Integer lastPosition;
        if (reservations.isEmpty()){
            lastPosition = 0;
        } else {
            lastPosition = reservations.size();
        }
        if (listReturnLoanDate.isEmpty()){
            reservationToSave.setSoonDisponibilityDate(LocalDate.now());
        } else {
            reservationToSave.setSoonDisponibilityDate(listReturnLoanDate.get(lastPosition));
        }
        if(reservations.isEmpty() || reservations.size()<= numberOfCopies){
            if ((LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY)
                        || (LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY)){
                reservationToSave.setEndOfPriority(LocalDate.now().plusDays(4));
            } else {
                reservationToSave.setEndOfPriority(LocalDate.now().plusDays(2));
            }
        }
        reservationToSave.setCreationReservationDate(LocalDateTime.now());
        reservationToSave.setCustomerId(reservation.getCustomerId());
        reservationToSave.setCustomerEmail(reservation.getCustomerEmail());
        reservationToSave.setCustomerFirstname(reservation.getCustomerFirstname());
        reservationToSave.setCustomerLastname(reservation.getCustomerLastname());
        reservationToSave.setBookId(reservation.getBookId());
        reservationToSave.setBookTitle(reservation.getBookTitle());
        reservationToSave.setPosition(lastPosition + 1);

        //send mail
        //uncomment before release
/*        sendPreConfiguredMail(
                reservationToSave.getCustomerEmail(),
                reservationToSave.getCustomerFirstname(),
                reservationToSave.getCustomerLastname(),
                formatDateTimeToMail(reservationToSave.getCreationReservationDate()),
                reservationToSave.getBookTitle(),
                formatDateToMail(reservationToSave.getEndOfPriority()));*/

        return reservationRepository.save(reservationToSave);
    }



    @Override
    public void updateResaBookId(Integer bookId, Integer numberOfCopies) {
        //get list resa for this book
        List<Reservation> reservations = reservationRepository.findAllByBookId(bookId);
/*        if (reservations.isEmpty()){
            return;
        }*/
        reservations.sort(Comparator.comparing(Reservation::getPosition));
        //send mail to reservation customer
        for (int i = 0; i < reservations.size(); i++) {
            //set end resa date
            if ((LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY)
                        || (LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY)){
                reservations.get(i).setEndOfPriority(LocalDate.now().plusDays(4));
            } else {
                reservations.get(i).setEndOfPriority(LocalDate.now().plusDays(2));
            }

            //send mail
            sendPreConfiguredMail(
                    reservations.get(i).getCustomerEmail(),
                    reservations.get(i).getCustomerFirstname(),
                    reservations.get(i).getCustomerLastname(),
                    formatDateTimeToMail(reservations.get(i).getCreationReservationDate()),
                    reservations.get(i).getBookTitle(),
                    formatDateToMail(reservations.get(i).getEndOfPriority()));
        }

    }

    @Override
    public void updateDateResaBookId(Integer bookId, List<LocalDate> listReturnLoanDate) {
        List<Reservation> reservations = reservationRepository.findAllByBookId(bookId);
        if (reservations.isEmpty()){
            return;
        }
        reservations.sort(Comparator.comparing(Reservation::getPosition));
        //change soon to return date
        for (int i = 0; i < reservations.size(); i++) {
            reservations.get(i).setSoonDisponibilityDate(listReturnLoanDate.get(i));
        }
    }

    @Override
    public void delete(Integer reservationId, List<LocalDate> listReturnLoanDate) {
        Optional<Reservation> optionalReservation = reservationRepository.findById(reservationId);
        if (!optionalReservation.isPresent()){
            throw new ReservationNotFoundException("Reservation not Found");
        }
        //get resa
        Reservation reservationToDelete = optionalReservation.get();

        //delete
        reservationRepository.deleteById(reservationId);

        //modify list resa
        //get new list of all reservations for the book
        List<Reservation> reservations = reservationRepository.findAllByBookId(reservationToDelete.getBookId());
        if (!reservations.isEmpty() && !listReturnLoanDate.isEmpty()){
            //set last position in the reservation list
            //Integer lastPosition = reservations.size();
            Integer deleteReservationPosition = reservationToDelete.getPosition();

            //change all position
            for (Reservation reservation : reservations){
                if (reservation.getPosition() > deleteReservationPosition){
                    reservation.setPosition(reservation.getPosition() - 1);
                }
            }
            reservations.sort(Comparator.comparing(Reservation::getPosition));
            //change soon to return date
            for (int i = 0; i < reservations.size(); i++) {
                reservations.get(i).setSoonDisponibilityDate(listReturnLoanDate.get(i));
            }
            reservationRepository.saveAll(reservations);
        }
    }

    @Override
    public List<Reservation> findAllByCustomerId(Integer customerId) {
        return reservationRepository.findAllByCustomerId(customerId);
    }

    @Override
    public List<Reservation> findAllByBookId(Integer bookId) {
        return reservationRepository.findAllByBookId(bookId);
    }


    @Override
    public boolean checkIfReservationExistForCustomerIdAndBookId(Integer customerId, Integer bookId) {
        return reservationRepository.existByCustomerIdAndBookId(customerId, bookId);
    }

    @Override
    public Reservation findByCustomerIdAndBookId(Integer customerId, Integer bookId) {
        Reservation reservation = reservationRepository.findByCustomerIdAndBookId(customerId, bookId);
        if (reservation == null){
            throw new ReservationNotFoundException("No reservation for this customer and book");
        }
        return reservation;
    }

    /**
     * This method will send a pre-configured message
     * @param argTo the email of the recipient
     * @param argFirst the firstName of the recipient
     * @param argLast the lastName of the recipient
     * @param argTitle the title of the book
     * @param date the date of the expected return
     *
     * */
    private void sendPreConfiguredMail(String argTo, String argFirst, String argLast, String resaDate, String argTitle, String date){
        SimpleMailMessage mailMessage = new SimpleMailMessage(preConfiguredMessage);
        String text = String.format(Objects.requireNonNull(mailMessage.getText()),argFirst, argLast, resaDate, argTitle, date);
        mailMessage.setTo(argTo);
        mailMessage.setText(text);
        javaMailSender.send(mailMessage);
    }


    private void sendSimpleMessage(String to, String subject, String body){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }

    /**
     * This method format the expected return date
     *
     * @param date a date
     * @return a formatted date
     */
    private String formatDateToMail(LocalDate date){
        String pattern = "dd MMM yyyy";
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }


    /**
     * This method format the expected return date
     *
     * @param date a date
     * @return a formatted date
     */
    private String formatDateTimeToMail(LocalDateTime date){
        String pattern = "dd MMM yyyy";
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }
}
