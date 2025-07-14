package com.example.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class AuthCallbackController {

    @Value("${orgamax.client_id}")
    private String clientId;

    @Value("${orgamax.client_secret}")
    private String clientSecret;

    @Value("${orgamax.redirect_uri}")
    private String redirectUri;

    @Value("${orgamax.token_url}")
    private String tokenUrl;

    @CrossOrigin
    @GetMapping("/auth/callback")
    public String handleCallback() {
      /*  System.out.println("Received code: " + code);

        String token = exchangeCodeForToken(code);
        System.out.println("Access Token: " + token);*/

        return "Authorization complete! Token received: Check console.";
    }

    @CrossOrigin
    @GetMapping("/test")
    public String doTest() {
        return "My App";
    }

    private String exchangeCodeForToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + redirectUri +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                tokenUrl,
                request,
                String.class
        );

        return response.getBody();
    }
}