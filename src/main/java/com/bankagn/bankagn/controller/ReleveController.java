package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.service.impl.ReleveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/client/releves")
@RequiredArgsConstructor
public class ReleveController {

    private final ReleveService releveService;

    @GetMapping
    public String relevesPage(Model model) {
        return "client/releves";
    }

    @GetMapping("/telecharger")
    public ResponseEntity<byte[]> telecharger(
            Authentication authentication) {
        try {
            String email = authentication.getName();
            byte[] pdf = releveService.genererReleve(email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(
                    "attachment",
                    "releve-bankagn.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}