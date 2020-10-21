package org.mickael.librarymsreservation.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mickael.librarymsreservation.exception.ReservationAlreadyExistException;
import org.mickael.librarymsreservation.exception.ReservationNotAllowedException;
import org.mickael.librarymsreservation.exception.ReservationNotFoundException;
import org.mickael.librarymsreservation.model.Reservation;
import org.mickael.librarymsreservation.repository.ReservationRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SimpleMailMessage preConfiguredMessage;

    private ReservationServiceImpl reservationServiceUnderTest;

    private static final String NOT_FOUND_MSG = "Reservation not Found in repository";
    private static final String RESERVATION_NOT_ALLOWED_MSG = "Reservation impossible. Contactez la bibliothèque. Merci.";
    private static final String ALREADY_RESERVED_MSG = "Vous avez déjà une réservation pour ce livre.";

    @Captor
    private ArgumentCaptor<Reservation> reservationArgumentCaptor;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.initMocks(this);
        preConfiguredMessage = new SimpleMailMessage();
        preConfiguredMessage.setTo("%s");
        preConfiguredMessage.setFrom("mc.ocform@gmail.com");
        preConfiguredMessage.setSubject("Réservation - Bibliothèque d'OCland");
        preConfiguredMessage.setText("Bonjour, %s %s" +
                                          "\n\nSuite à votre réservation, du : %s" +
                                          "\n\nNous vous informons que le livre \"%s\" est disponible en bibliothèque." +
                                          "\nVous avez jusqu'au %s pour venir emprunter l'ouvrage. Passé ce jour, vous devrez effectuer une nouvelle réservation." +
                                          "\nN'oubliez pas de ramener vos autres emprunts." +
                                          "\n\n\nBibliothèque d'OCland" +
                                          "\n\n\n\n\nCeci est un envoi automatique, merci de ne pas y répondre.");
        reservationServiceUnderTest = new ReservationServiceImpl(reservationRepository,javaMailSender,preConfiguredMessage);
    }

    @Test
    void itShouldReturnAListOfReservation(){
        //Given
        Reservation reservation = new Reservation();
        given(reservationRepository.findAll()).willReturn(Collections.singletonList(reservation));

        //When


        //Then
        assertThat(reservationServiceUnderTest.findAll()).isNotEmpty();
    }

    @Test
    @DisplayName("True if return one reservation")
    void itShouldReturnOneReservation(){
        //Given
        Integer reservationId = 1;
        Reservation reservation = new Reservation();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        //When


        //Then
        assertThat(reservationServiceUnderTest.findById(reservationId)).isInstanceOf(Reservation.class);
    }

    @Test
    void itShouldReturnExceptionIfReservationNotExist(){
        //Given
        Integer reservationId = 1;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

        //When


        //Then
        assertThatThrownBy(() ->reservationServiceUnderTest.findById(reservationId))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasMessageContaining(NOT_FOUND_MSG);
    }

    @Test
    void itShouldSaveReservation(){
        //Given
        List<LocalDate> listReturnLoanDate = new ArrayList<>();
        Integer numberOfCopies = 2;
        Integer copiesAvailable = 2;
        Integer customerId = 2;
        Integer bookId = 18;
        String bookTitle = "the witcher";
        String customerFirstName = "mickael";
        String customerLastName = "coz" ;
        String customerEmail = "coz.mickael@gmail.com";

        Reservation reservation = new Reservation();
        reservation.setCreationReservationDate(LocalDateTime.now());
        reservation.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation.setSoonDisponibilityDate(LocalDate.now());
        reservation.setPosition(1);
        reservation.setCustomerId(customerId);
        reservation.setCustomerLastname(customerLastName);
        reservation.setCustomerFirstname(customerFirstName);
        reservation.setCustomerEmail(customerEmail);
        reservation.setBookId(bookId);
        reservation.setBookTitle(bookTitle);

        given(reservationRepository.findByCustomerIdAndBookId(customerId,bookId)).willReturn(null);
        given(reservationRepository.findAllByBookId(bookId)).willReturn(Collections.emptyList());

        //When
        reservationServiceUnderTest.save(reservation,listReturnLoanDate,numberOfCopies,copiesAvailable);

        //Then
        then(reservationRepository).should().save(reservationArgumentCaptor.capture());
        Reservation reservationArgumentCaptorValue = reservationArgumentCaptor.getValue();
        assertThat(reservationArgumentCaptorValue.getBookId()).isEqualTo(reservation.getBookId());
        assertThat(reservationArgumentCaptorValue.getBookTitle()).isEqualTo(reservation.getBookTitle());
        assertThat(reservationArgumentCaptorValue.getCustomerEmail()).isEqualTo(reservation.getCustomerEmail());
        assertThat(reservationArgumentCaptorValue.getCustomerFirstname()).isEqualTo(reservation.getCustomerFirstname());
        assertThat(reservationArgumentCaptorValue.getCustomerLastname()).isEqualTo(reservation.getCustomerLastname());
        assertThat(reservationArgumentCaptorValue.getCustomerId()).isEqualTo(reservation.getCustomerId());
        assertThat(reservationArgumentCaptorValue.getPosition()).isEqualTo(reservation.getPosition());
        assertThat(reservationArgumentCaptorValue.getEndOfPriority()).isEqualTo(reservation.getEndOfPriority());
        assertThat(reservationArgumentCaptorValue.getSoonDisponibilityDate()).isEqualTo(reservation.getSoonDisponibilityDate());
    }

    @Test
    void itShouldSaveReservationWhenNoBookDisponibilityAndOneReservation(){
        //Given
        List<LocalDate> listReturnLoanDate = new ArrayList<>();
        listReturnLoanDate.add(LocalDate.of(2020, 10, 01));

        Integer numberOfCopies = 2;
        Integer copiesAvailable = 0;
        Integer customerId = 2;
        Integer bookId = 18;
        String bookTitle = "the witcher";
        String customerFirstName = "mickael";
        String customerLastName = "coz" ;
        String customerEmail = "coz.mickael@gmail.com";

        Reservation reservation = new Reservation();
        reservation.setCreationReservationDate(LocalDateTime.now());
        reservation.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation.setSoonDisponibilityDate(LocalDate.now());
        reservation.setPosition(2);
        reservation.setCustomerId(customerId);
        reservation.setCustomerLastname(customerLastName);
        reservation.setCustomerFirstname(customerFirstName);
        reservation.setCustomerEmail(customerEmail);
        reservation.setBookId(bookId);
        reservation.setBookTitle(bookTitle);


        given(reservationRepository.findByCustomerIdAndBookId(customerId,bookId)).willReturn(null);
        given(reservationRepository.findAllByBookId(bookId)).willReturn(Collections.singletonList(reservation));


        //When
        reservationServiceUnderTest.save(reservation,listReturnLoanDate,numberOfCopies,copiesAvailable);

        //Then
        then(reservationRepository).should().save(reservationArgumentCaptor.capture());
        Reservation reservationArgumentCaptorValue = reservationArgumentCaptor.getValue();
        assertThat(reservationArgumentCaptorValue.getBookId()).isEqualTo(reservation.getBookId());
        assertThat(reservationArgumentCaptorValue.getBookTitle()).isEqualTo(reservation.getBookTitle());
        assertThat(reservationArgumentCaptorValue.getCustomerEmail()).isEqualTo(reservation.getCustomerEmail());
        assertThat(reservationArgumentCaptorValue.getCustomerFirstname()).isEqualTo(reservation.getCustomerFirstname());
        assertThat(reservationArgumentCaptorValue.getCustomerLastname()).isEqualTo(reservation.getCustomerLastname());
        assertThat(reservationArgumentCaptorValue.getCustomerId()).isEqualTo(reservation.getCustomerId());
        assertThat(reservationArgumentCaptorValue.getPosition()).isEqualTo(reservation.getPosition());
    }

    @Test
    void itShouldSaveReservationWhenNoBookDisponibilityAndNoReservation(){
        //Given
        List<LocalDate> listReturnLoanDate = new ArrayList<>();
        listReturnLoanDate.add(LocalDate.of(2020, 10, 01));

        Integer numberOfCopies = 2;
        Integer copiesAvailable = 0;
        Integer customerId = 2;
        Integer bookId = 18;
        String bookTitle = "the witcher";
        String customerFirstName = "mickael";
        String customerLastName = "coz" ;
        String customerEmail = "coz.mickael@gmail.com";

        Reservation reservation = new Reservation();
        reservation.setCreationReservationDate(LocalDateTime.now());
        reservation.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation.setSoonDisponibilityDate(LocalDate.now());
        reservation.setPosition(1);
        reservation.setCustomerId(customerId);
        reservation.setCustomerLastname(customerLastName);
        reservation.setCustomerFirstname(customerFirstName);
        reservation.setCustomerEmail(customerEmail);
        reservation.setBookId(bookId);
        reservation.setBookTitle(bookTitle);


        given(reservationRepository.findByCustomerIdAndBookId(customerId,bookId)).willReturn(null);
        given(reservationRepository.findAllByBookId(bookId)).willReturn(Collections.emptyList());


        //When
        reservationServiceUnderTest.save(reservation,listReturnLoanDate,numberOfCopies,copiesAvailable);

        //Then
        then(reservationRepository).should().save(reservationArgumentCaptor.capture());
        Reservation reservationArgumentCaptorValue = reservationArgumentCaptor.getValue();
        assertThat(reservationArgumentCaptorValue.getBookId()).isEqualTo(reservation.getBookId());
        assertThat(reservationArgumentCaptorValue.getBookTitle()).isEqualTo(reservation.getBookTitle());
        assertThat(reservationArgumentCaptorValue.getCustomerEmail()).isEqualTo(reservation.getCustomerEmail());
        assertThat(reservationArgumentCaptorValue.getCustomerFirstname()).isEqualTo(reservation.getCustomerFirstname());
        assertThat(reservationArgumentCaptorValue.getCustomerLastname()).isEqualTo(reservation.getCustomerLastname());
        assertThat(reservationArgumentCaptorValue.getCustomerId()).isEqualTo(reservation.getCustomerId());
        assertThat(reservationArgumentCaptorValue.getPosition()).isEqualTo(reservation.getPosition());
    }

    @Test
    void itShouldThrowReservationAlreadyExistException(){
        //Given
        List<LocalDate> listReturnLoanDate = new ArrayList<>();
        listReturnLoanDate.add(LocalDate.of(2020, 10, 01));

        Integer numberOfCopies = 2;
        Integer copiesAvailable = 0;
        Integer customerId = 2;
        Integer bookId = 18;
        String bookTitle = "the witcher";
        String customerFirstName = "mickael";
        String customerLastName = "coz" ;
        String customerEmail = "coz.mickael@gmail.com";

        Reservation reservation = new Reservation();
        reservation.setCreationReservationDate(LocalDateTime.now());
        reservation.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation.setSoonDisponibilityDate(LocalDate.now());
        reservation.setPosition(1);
        reservation.setCustomerId(customerId);
        reservation.setCustomerLastname(customerLastName);
        reservation.setCustomerFirstname(customerFirstName);
        reservation.setCustomerEmail(customerEmail);
        reservation.setBookId(bookId);
        reservation.setBookTitle(bookTitle);


        given(reservationRepository.findByCustomerIdAndBookId(customerId,bookId)).willReturn(reservation);

        //When


        //Then
        assertThatThrownBy(()->reservationServiceUnderTest.save(reservation,listReturnLoanDate,numberOfCopies,copiesAvailable))
                .isInstanceOf(ReservationAlreadyExistException.class)
                .hasMessageContaining(ALREADY_RESERVED_MSG);
    }

    @Test
    void itShouldThrowReservationNotAllowedException(){
        //Given
        List<LocalDate> listReturnLoanDate = new ArrayList<>();

        Integer numberOfCopies = 2;
        Integer copiesAvailable = 0;
        Integer customerId = 2;
        Integer bookId = 18;
        String bookTitle = "the witcher";
        String customerFirstName = "mickael";
        String customerLastName = "coz" ;
        String customerEmail = "coz.mickael@gmail.com";

        Reservation reservation = new Reservation();
        reservation.setCreationReservationDate(LocalDateTime.now());
        reservation.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation.setSoonDisponibilityDate(LocalDate.now());
        reservation.setPosition(1);
        reservation.setCustomerId(customerId);
        reservation.setCustomerLastname(customerLastName);
        reservation.setCustomerFirstname(customerFirstName);
        reservation.setCustomerEmail(customerEmail);
        reservation.setBookId(bookId);
        reservation.setBookTitle(bookTitle);


        given(reservationRepository.findByCustomerIdAndBookId(customerId,bookId)).willReturn(null);

        //When
        //Then
        assertThatThrownBy(()->reservationServiceUnderTest.save(reservation,listReturnLoanDate,numberOfCopies,copiesAvailable))
                .isInstanceOf(ReservationNotAllowedException.class)
                .hasMessageContaining(RESERVATION_NOT_ALLOWED_MSG);
    }

    @Test
    void itShouldReturnReservationByCustomerIdAndBookId(){
        //Given
        Integer customerId = 2;
        Integer bookId = 18;
        String bookTitle = "the witcher";
        String customerFirstName = "mickael";
        String customerLastName = "coz" ;
        String customerEmail = "coz.mickael@gmail.com";
        Reservation reservation = new Reservation();
        reservation.setCreationReservationDate(LocalDateTime.now());
        reservation.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation.setSoonDisponibilityDate(LocalDate.now());
        reservation.setPosition(1);
        reservation.setCustomerId(customerId);
        reservation.setCustomerLastname(customerLastName);
        reservation.setCustomerFirstname(customerFirstName);
        reservation.setCustomerEmail(customerEmail);
        reservation.setBookId(bookId);
        reservation.setBookTitle(bookTitle);
        given(reservationRepository.findByCustomerIdAndBookId(customerId,bookId)).willReturn(reservation);
        //when
        Reservation reservationToFind = reservationServiceUnderTest.findByCustomerIdAndBookId(customerId,bookId);
        //then
        assertThat(reservationToFind).isInstanceOf(Reservation.class);
        assertThat(reservationToFind.getId()).isEqualTo(reservation.getId());
    }

    @Test
    void itShouldThrowReservationNotFoundExceptionWhenSearchingForReservationByCustomerIdAndBookId(){
        //Given
        Integer customerId = 2;
        Integer bookId = 18;
        given(reservationRepository.findByCustomerIdAndBookId(customerId,bookId)).willReturn(null);
        //when
        //Then
        assertThatThrownBy(()->reservationServiceUnderTest.findByCustomerIdAndBookId(customerId,bookId))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasMessageContaining(NOT_FOUND_MSG);
    }

    @Test
    void itShouldReturnAListOfReservationByCustomerId(){
        //Given
        Integer customerId = 2;
        Reservation reservation = new Reservation();
        given(reservationRepository.findAllByCustomerId(customerId)).willReturn(Collections.singletonList(reservation));

        //When
        //Then
        assertThat(reservationServiceUnderTest.findAllByCustomerId(customerId)).isNotEmpty();
    }

    @Test
    void itShouldReturnAListOfReservationByBookId(){
        //Given
        Integer bookId = 18;
        Reservation reservation = new Reservation();
        given(reservationRepository.findAllByBookId(bookId)).willReturn(Collections.singletonList(reservation));

        //When
        //Then
        assertThat(reservationServiceUnderTest.findAllByBookId(bookId)).isNotEmpty();
    }

    @Test
    void itShouldCheckIfReservationExistByCustomerIdAndByBookId(){
        //Given
        Integer customerId = 2;
        Integer bookId = 18;
        given(reservationRepository.existByCustomerIdAndBookId(customerId,bookId)).willReturn(true);

        //When
        //Then
        assertThat(reservationServiceUnderTest.checkIfReservationExistForCustomerIdAndBookId(customerId,bookId)).isTrue();
    }

    @Test
    void itShouldUpdateReservationBook(){
        //Given
        List<LocalDate> listReturnLoanDate = new ArrayList<>();
        LocalDate now = LocalDate.now();
        listReturnLoanDate.add(now);
        Integer bookId = 18;
        Reservation reservation = new Reservation();
        given(reservationRepository.findAllByBookId(bookId)).willReturn(Collections.singletonList(reservation));

        //when
        reservationServiceUnderTest.updateDateResaBookId(bookId,listReturnLoanDate);

        //Then
        assertThat(reservation.getSoonDisponibilityDate()).isEqualTo(now);
    }

    @Test
    void itShouldThrowReservationNotFoundExceptionWhenDelete(){
        //Given
        Integer reservationId = 1;
        List<LocalDate> listReturnLoanDate = new ArrayList<>();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

        //When
        //Then
        assertThatThrownBy(()->reservationServiceUnderTest.delete(reservationId,listReturnLoanDate))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasMessageContaining(NOT_FOUND_MSG);
    }

    @Test
    void itShouldDeleteReservation(){
        //Given
        Integer reservationId = 1;
        List<LocalDate> listReturnLoanDate = new ArrayList<>();
        Integer customerId = 2;
        Integer bookId = 18;
        String bookTitle = "the witcher";
        String customerFirstName = "mickael";
        String customerLastName = "coz" ;
        String customerEmail = "coz.mickael@gmail.com";
        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setCreationReservationDate(LocalDateTime.now());
        reservation.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation.setSoonDisponibilityDate(LocalDate.now());
        reservation.setPosition(1);
        reservation.setCustomerId(customerId);
        reservation.setCustomerLastname(customerLastName);
        reservation.setCustomerFirstname(customerFirstName);
        reservation.setCustomerEmail(customerEmail);
        reservation.setBookId(bookId);
        reservation.setBookTitle(bookTitle);

        Integer customerId2 = 3;
        String customerFirstName2 = "mickael";
        String customerLastName2 = "coz" ;
        String customerEmail2 = "coz.mickael@gmail.com";
        Reservation reservation2 = new Reservation();
        reservation2.setId(reservationId);
        reservation2.setCreationReservationDate(LocalDateTime.now());
        reservation2.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation2.setSoonDisponibilityDate(LocalDate.now());
        reservation2.setPosition(2);
        reservation2.setCustomerId(customerId);
        reservation2.setCustomerLastname(customerLastName);
        reservation2.setCustomerFirstname(customerFirstName);
        reservation2.setCustomerEmail(customerEmail);
        reservation2.setBookId(bookId);
        reservation2.setBookTitle(bookTitle);

        Integer customerId3 = 4;
        String customerFirstName3 = "mickael";
        String customerLastName3 = "coz" ;
        String customerEmail3 = "coz.mickael@gmail.com";
        Reservation reservation3 = new Reservation();
        reservation3.setId(reservationId);
        reservation3.setCreationReservationDate(LocalDateTime.now());
        reservation3.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation3.setSoonDisponibilityDate(LocalDate.now());
        reservation3.setPosition(3);
        reservation3.setCustomerId(customerId);
        reservation3.setCustomerLastname(customerLastName);
        reservation3.setCustomerFirstname(customerFirstName);
        reservation3.setCustomerEmail(customerEmail);
        reservation3.setBookId(bookId);
        reservation3.setBookTitle(bookTitle);

        Integer customerId4 = 5;
        String customerFirstName4 = "mickael";
        String customerLastName4 = "coz" ;
        String customerEmail4 = "coz.mickael@gmail.com";
        Reservation reservation4 = new Reservation();
        reservation4.setId(reservationId);
        reservation4.setCreationReservationDate(LocalDateTime.now());
        reservation4.setEndOfPriority(LocalDate.now().plusDays(2));
        reservation4.setSoonDisponibilityDate(LocalDate.now());
        reservation4.setPosition(1);
        reservation4.setCustomerId(customerId);
        reservation4.setCustomerLastname(customerLastName);
        reservation4.setCustomerFirstname(customerFirstName);
        reservation4.setCustomerEmail(customerEmail);
        reservation4.setBookId(bookId);
        reservation4.setBookTitle(bookTitle);

        List<Reservation> reservations = new ArrayList<>();
        reservations.add(reservation2);
        reservations.add(reservation3);
        reservations.add(reservation4);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(reservationRepository.findAllByBookId(bookId)).willReturn(reservations);

        //When
        reservationServiceUnderTest.delete(reservationId, listReturnLoanDate);

        //Then
        assertThat(reservation2.getPosition()).isEqualTo(1);
    }
}
