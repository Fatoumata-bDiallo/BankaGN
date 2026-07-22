package com.bankagn.bankagn.service.impl;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend;

    public EmailService() {
        this.resend = new Resend("re_MkYnzo4C_CcTPRM9NwoqBBqFZEhh65aSj");
    }

    public void envoyerEmail(String destinataire,
                             String sujet,
                             String message) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("BankaGN <onboarding@resend.dev>")
                    .to(destinataire)
                    .subject(sujet)
                    .text(message)
                    .build();

            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("✅ Email envoyé ! ID: " + data.getId());
        } catch (ResendException e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
        }
    }
}