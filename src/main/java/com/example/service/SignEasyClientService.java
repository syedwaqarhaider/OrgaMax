package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.util.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class SignEasyClientService {


    public int sendEnvelope(
            String apiToken,
            boolean isOrdered,
            boolean embeddedSigning,
            String message,
            List<Map<String, Object>> sources,
            List<Map<String, Object>> recipients,
            List<Map<String, Object>> fieldsPayload
    ) {
        String baseUrl="https://api.signeasy.com/v3/rs/envelope/";
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> body = new HashMap<>();
        body.put("is_ordered", isOrdered);
        body.put("embedded_signing", embeddedSigning);
        body.put("message", message);
        body.put("sources", sources);
        body.put("recipients", recipients);
        body.put("fields_payload", fieldsPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
       try{
           restTemplate.postForEntity(baseUrl, req, String.class);
           return 1;
       }
       catch (Exception e) {
           System.out.println(e.getMessage());
           return 0;
       }
    }


    public List<Map<String, Object>> getSources(int docID, String subject)
    {
         List<Map<String, Object>> sources = List.of(
            Map.of("source_id", 1, "type", "original", "id", docID, "name", subject)
    );
         return sources;
    }

    public List<Map<String, Object>> getRecipients(List<String> emailsChunk) {
        List<Map<String, Object>> recipients = new ArrayList<>();

        int id = 1;
        for (String email : emailsChunk) {
            Map<String, Object> recipient = new HashMap<>();
            recipient.put("recipient_id", id++);
            recipient.put("email", email);
            recipient.put("first_name", "There");
            recipient.put("type", "signer");
            recipients.add(recipient);
        }

        return recipients;
    }

    public List<Map<String, Object>> getPlayloadFields()
    {
        List<Map<String, Object>> fieldsPayload = List.of(
                Map.of(
                        "recipient_id", 1,
                        "source_id", 1,
                        "type", "signature",
                        "required", true,
                        "page_number", 1,
                        "position", Map.of("x",60,"y",520,"width",250,"height",50,"mode","fixed"),
                        "additional_info", Map.of()
                )
        );

        return fieldsPayload;
    }

    public int uploadOriginal(String apiToken, InputStream fileStream, String filename, long size) throws Exception {
        String url = "https://api.signeasy.com/v3" + "/original/";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiToken);

        InputStreamResource resource = new InputStreamResource(fileStream) {
            @Override public String getFilename() { return filename; }
            @Override public long contentLength() { return size; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("name", filename);
        body.add("rename_if_exists", "true");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, requestEntity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("SignEasy upload error: " +
                    resp.getStatusCode() + " – " + resp.getBody());
        }

        JsonNode root = new ObjectMapper().readTree(resp.getBody());
        JsonNode idNode = root.has("original_file_id") ? root.get("original_file_id") : root.get("id");
        if (idNode != null && idNode.isInt()) {
            return idNode.intValue();
        }
        throw new RuntimeException("Missing 'id' in SignEasy response: " + resp.getBody());
    }


}
