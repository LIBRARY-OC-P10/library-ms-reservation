package org.mickael.librarymsreservation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.reservation_reservation_id_seq")
    @SequenceGenerator(name = "public.reservation_reservation_id_seq", sequenceName = "public.reservation_reservation_id_seq", allocationSize = 1)
    @Column(name = "reservation_id")
    private Integer id;

    @Column(name = "create_reservation_date")
    private LocalDateTime creationReservationDate;

    @Column(name = "soon_disponibility_date")
    private LocalDate soonDisponibilityDate;

    @Column(name = "end_of_priority")
    private LocalDate endOfPriority;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_firstname")
    private String customerFirstname;

    @Column(name = "customer_lastname")
    private String customerLastname;

    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "book_title")
    private String bookTitle;

    @Column(name = "position")
    private Integer position;


}
