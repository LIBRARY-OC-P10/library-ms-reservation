package org.mickael.librarymsreservation.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;

@Configuration
public class EmailConfig {

    @Bean
    public SimpleMailMessage emailNotificationTemplate(){
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo("%s");
        simpleMailMessage.setFrom("mc.ocform@gmail.com");
        simpleMailMessage.setSubject("Réservation - Bibliothèque d'OCland");
        simpleMailMessage.setText("Bonjour, %s %s" +
                                          "\n\nSuite à votre réservation, du : %s" +
                                          "\n\nNous vous informons que le livre \"%s\" est disponible en bibliothèque." +
                                          "\nVous avez jusqu'au %s pour venir emprunter l'ouvrage. Passé ce jour, vous devrez effectuer une nouvelle réservation." +
                                          "\nN'oubliez pas de ramener vos autres emprunts." +
                                          "\n\n\nBibliothèque d'OCland" +
                                          "\n\n\n\n\nCeci est un envoi automatique, merci de ne pas y répondre.");
        return simpleMailMessage;
    }
}
