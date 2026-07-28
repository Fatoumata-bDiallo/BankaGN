package com.bankagn.bankagn.service.impl;

import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    private static final String API_KEY =
            System.getenv("BREVO_API_KEY");

    public void envoyerEmail(String destinataire,
                             String sujet,
                             String message) {
        try {
            String body = "{"
                    + "\"sender\":{\"email\":\"fatmabinta47@gmail.com\","
                    + "\"name\":\"BankaGN\"},"
                    + "\"to\":[{\"email\":\"" + destinataire + "\"}],"
                    + "\"subject\":\"" + sujet + "\","
                    + "\"textContent\":\"" + message
                    .replace("\\", "")
                    .replace("\"", "'")
                    .replace("\n", " ")
                    .replace("\r", " ") + "\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", API_KEY)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient()
                            .send(request,
                                    HttpResponse.BodyHandlers.ofString());

            System.out.println("✅ Email envoyé ! Status: "
                    + response.statusCode());

        } catch (Exception e) {
            System.err.println("❌ Erreur : "
                    + e.getMessage());
        }
    }
}