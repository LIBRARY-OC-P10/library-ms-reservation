package org.mickael.librarymsreservation.service.impl;

import javafx.beans.binding.When;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mickael.librarymsreservation.configuration.EmailConfig;
import org.mickael.librarymsreservation.exception.ReservationNotFoundException;
import org.mickael.librarymsreservation.model.Reservation;
import org.mickael.librarymsreservation.repository.ReservationRepository;
import org.mockito.*;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

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
    void itShoudReturnAListOfReservation(){
        //Given
        Reservation reservation = new Reservation();
        given(reservationRepository.findAll()).willReturn(Collections.singletonList(reservation));

        //When


        //Then
        assertThat(reservationServiceUnderTest.findAll()).isNotEmpty();
    }

    @Test
    @DisplayName("True if return one reservation")
    void itShoudReturnOneReservation(){
        //Given
        Integer reservationId = 1;
        Reservation reservation = new Reservation();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        //When


        //Then
        assertThat(reservationServiceUnderTest.findById(reservationId)).isInstanceOf(Reservation.class);
    }

    @Test
    void itShoudReturnExceptionIfReservationNotExist(){
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
        assertThat(reservationArgumentCaptorValue).isEqualTo(reservation);
    }
}
