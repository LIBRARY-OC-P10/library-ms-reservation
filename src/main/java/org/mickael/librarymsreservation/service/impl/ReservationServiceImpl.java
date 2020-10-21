package org.mickael.librarymsreservation.service.impl;

import org.mickael.librarymsreservation.exception.ReservationAlreadyExistException;
import org.mickael.librarymsreservation.exception.ReservationNotAllowedException;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReservationServiceImpl implements ReservationServiceContract {

    private final ReservationRepository reservationRepository;
    private JavaMailSender javaMailSender;
    private SimpleMailMessage preConfiguredMessage;

    private static final String NOT_FOUND_MSG = "Reservation not Found in repository";
    private static final String RESERVATION_NOT_ALLOWED_MSG = "Reservation impossible. Contactez la bibliothèque. Merci.";
    private static final String ALREADY_RESERVED_MSG = "Vous avez déjà une réservation pour ce livre.";

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
            throw new ReservationNotFoundException(NOT_FOUND_MSG);
        }
        return optionalReservation.get();
    }

    @Override
    public Reservation save(Reservation reservation, List<LocalDate> listReturnLoanDate, Integer numberOfCopies, Integer copiesAvailable) {

        //check if the customer already had a reservation
        Reservation reservationInBdd = reservationRepository.findByCustomerIdAndBookId(reservation.getCustomerId(), reservation.getBookId());
        if (reservationInBdd != null){
            throw new ReservationAlreadyExistException(ALREADY_RESERVED_MSG);
        }
        //get all the reservation for the book to know the position in the list
        List<Reservation> reservations = reservationRepository.findAllByBookId(reservation.getBookId());
        //set last position in the reservation list
        Integer lastPosition;

        //reservation to save
        Reservation reservationToSave = new Reservation();
        reservationToSave.setCreationReservationDate(LocalDateTime.now());
        reservationToSave.setCustomerId(reservation.getCustomerId());
        reservationToSave.setCustomerEmail(reservation.getCustomerEmail());
        reservationToSave.setCustomerFirstname(reservation.getCustomerFirstname());
        reservationToSave.setCustomerLastname(reservation.getCustomerLastname());
        reservationToSave.setBookId(reservation.getBookId());
        reservationToSave.setBookTitle(reservation.getBookTitle());

        //no reservation
        if (reservations.isEmpty()) {
            lastPosition = 0;
        } else {
            lastPosition = reservations.size();
        }

        reservationToSave.setPosition(lastPosition + 1);

        //copies available
        if (copiesAvailable > 0) {
            if (reservations.isEmpty()){
                //soon disponibility
                reservationToSave.setSoonDisponibilityDate(LocalDate.now());
                //end priority
                if ((LocalDate.now().getDayOfWeek() == DayOfWeek.FRIDAY)
                            || (LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY)) {
                    reservationToSave.setEndOfPriority(LocalDate.now().plusDays(3));
                } else {
                    reservationToSave.setEndOfPriority(LocalDate.now().plusDays(2));
                }
            } else {
                reservationToSave.setSoonDisponibilityDate(reservations.get(reservations.size() - 1).getEndOfPriority());
                //end priority
                if ((LocalDate.now().getDayOfWeek() == DayOfWeek.FRIDAY)
                            || (LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY)) {
                    reservationToSave.setEndOfPriority(reservations.get(reservations.size() - 1).getEndOfPriority().plusDays(3));
                } else {
                    reservationToSave.setEndOfPriority(reservations.get(reservations.size() - 1).getEndOfPriority().plusDays(2));
                }
            }
            //send mail
            //uncomment before release
            sendPreConfiguredMail(
                    reservationToSave.getCustomerEmail(),
                    reservationToSave.getCustomerFirstname(),
                    reservationToSave.getCustomerLastname(),
                    formatDateTimeToMail(reservationToSave.getCreationReservationDate()),
                    reservationToSave.getBookTitle(),
                    formatDateToMail(reservationToSave.getEndOfPriority()));
        } else {
            if (!listReturnLoanDate.isEmpty()){
                //soon disponibility
                if (lastPosition == 0){
                    reservationToSave.setSoonDisponibilityDate(listReturnLoanDate.get(lastPosition));
                    reservationToSave.setEndOfPriority(listReturnLoanDate.get(lastPosition).plusDays(2));
                } else {
                    reservationToSave.setSoonDisponibilityDate(listReturnLoanDate.get(lastPosition - 1));
                    //end priority A VOIR
                    reservationToSave.setEndOfPriority(listReturnLoanDate.get(lastPosition - 1).plusDays(2));
                }
            } else {
                throw new ReservationNotAllowedException(RESERVATION_NOT_ALLOWED_MSG);
            }
        }
        return reservationRepository.save(reservationToSave);
    }



    @Override
    public void updateReservationsAndSendMail() {
        List<Reservation> reservations = reservationRepository.findAll();
        for (Reservation reservation : reservations){
            if(reservation.getSoonDisponibilityDate().compareTo(LocalDate.now()) == 0){
                sendPreConfiguredMail(
                        reservation.getCustomerEmail(),
                        reservation.getCustomerFirstname(),
                        reservation.getCustomerLastname(),
                        formatDateTimeToMail(reservation.getCreationReservationDate()),
                        reservation.getBookTitle(),
                        formatDateToMail(reservation.getEndOfPriority()));
            }
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
            throw new ReservationNotFoundException(NOT_FOUND_MSG);
        }
        //get resa
        Reservation reservationToDelete = optionalReservation.get();

        //delete
        reservationRepository.deleteById(reservationId);

        //modify list resa
        //get new list of all reservations for the book
        List<Reservation> reservations = reservationRepository.findAllByBookId(reservationToDelete.getBookId());

        //on regarde si la liste de réservation n'est pas vide sinon on ne fait rien.
        if (!reservations.isEmpty()){
            //set last position in the reservation list
            Integer deleteReservationPosition = reservationToDelete.getPosition();

            //change all position
            for (Reservation reservation : reservations){
                if (reservation.getPosition() > deleteReservationPosition){
                    reservation.setPosition(reservation.getPosition() - 1);
                }
            }
            reservations.sort(Comparator.comparing(Reservation::getPosition));

            //no loan
            if (listReturnLoanDate.isEmpty()){
                for (int i = 0; i < reservations.size(); i++) {
                    reservations.get(i).setSoonDisponibilityDate(LocalDate.now().plusDays(2*i));
                    reservations.get(i).setEndOfPriority(LocalDate.now().plusDays(2+(2*i)));
                }
            //with loan
            } else {
                //change soon to return date
                for (int i = 0; i < reservations.size(); i++) {
                    reservations.get(i).setSoonDisponibilityDate(listReturnLoanDate.get(i));
                }
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
            throw new ReservationNotFoundException(NOT_FOUND_MSG);
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
