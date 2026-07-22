package com.bankagn.bankagn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void envoyerEmail(String destinataire,
                             String sujet,
                             String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom("fatmabinta47@gmail.com");
            email.setTo(destinataire);
            email.setSubject(sujet);
            email.setText(message);
            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Erreur envoi email : "
                    + e.getMessage());
        }
    }
}