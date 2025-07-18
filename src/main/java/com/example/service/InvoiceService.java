package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class InvoiceService {

    private  String API_KEY;
    private String API_SECRET;
    private String OWNERSHIP_ID;
    private String TOKEN_URL = "https://api.orgamax.de/openapi/auth/token";

    public int sendInvoice(String apiKey, String apiSecretKey, String ownershipId, String subject, List<String> emails)
    {
        API_KEY=apiKey;
        API_SECRET=apiSecretKey;
        OWNERSHIP_ID=ownershipId;
        String token=getToken();
        if(token.equalsIgnoreCase("0"))
        {
            return 2;
        }
        else {
            int invoiceID=getInvoiceID(token);
            return sendingInvoice(token, subject, emails, invoiceID);
        }

    }

    private int getInvoiceID(String token) {
        String url = "https://api.orgamax.de/openapi/invoice?offset=0&limit=20&orderBy=number&desc=false&filter=all";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");

                if (!data.isEmpty()) {
                    Map<String, Object> firstInvoice = data.get(0);
                    int invoiceId = Integer.parseInt(firstInvoice.get("id").toString());
                    System.out.println("Invoice ID : "+ invoiceId);
                    return invoiceId;
                } else {
                    throw new RuntimeException("No invoices found!");
                }
            } else {
                throw new RuntimeException("No data key found in response!");
            }

        } else {
            throw new RuntimeException("Failed to fetch invoices! Status: " + response.getStatusCode());
        }
    }


    public String getToken() {
        RestTemplate restTemplate = new RestTemplate();
       try {
           String auth = this.API_KEY + ":" + this.API_SECRET;
           String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

           HttpHeaders headers = new HttpHeaders();
           headers.setContentType(MediaType.APPLICATION_JSON);
           headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
           headers.set("Authorization", "Basic " + encodedAuth);

           Map<String, String> body = Collections.singletonMap("ownershipId", OWNERSHIP_ID);

           HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

           ResponseEntity<Map> response = restTemplate.exchange(
                   TOKEN_URL,
                   HttpMethod.POST,
                   request,
                   Map.class
           );

           if (response.getStatusCode() == HttpStatus.OK) {
               Map<String, Object> responseBody = response.getBody();
               if (responseBody != null && responseBody.containsKey("token")) {
                   return responseBody.get("token").toString();
               } else {
                   throw new RuntimeException("Token not found in response");
               }
           } else {
               throw new RuntimeException("Failed to get token. Status: " + response.getStatusCode());
           }
       }
       catch (Exception e)
       {
           return "0";
       }
    }

    public int sendingInvoice(String token, String subject, List<String> emails, int invoiceID)
    {
        try{
        String url = "https://api.orgamax.de/openapi" + "/invoice/" + invoiceID + "/send";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("recipients", emails);
        body.put("subject", subject);
        body.put("attachmentName", "Rechnung");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        System.out.println("Send invoice response: " + response.getStatusCode());
        System.out.println("Response body: " + response.getBody());
        if (response.getStatusCode().is2xxSuccessful() )
        {
            return 1;
        } else if (response.getStatusCode().is4xxClientError()) {
            return 3;

        } else {
            System.out.println("Comming Here");
            return 0;
        }

        }catch (HttpClientErrorException.BadRequest ex) {
            System.out.println("400 Bad Request: " + ex.getResponseBodyAsString());
            return handleValidationError(ex.getResponseBodyAsString());

        }

    }

    public int handleValidationError(String responseBody) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode meta = root.path("meta");

            if (meta.has("emailCount")) {
                // Too many emails
                JsonNode emailCount = meta.get("emailCount");
                for (JsonNode item : emailCount) {
                    String code = item.path("code").asText();
                    if ("too much".equalsIgnoreCase(code)) {
                        return 0;  // your special code for too many
                    }
                }
            }

            if (meta.has("recipients")) {
                JsonNode recipients = meta.get("recipients");
                for (JsonNode recipient : recipients) {
                    String code = recipient.path("code").asText();
                    if ("INVALID".equalsIgnoreCase(code)) {
                        return 3;  // your special code for format issue
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error parsing validation error: " + e.getMessage());
        }
        return -1; // fallback if no match found
    }
}
