package com.example.model;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Invoice {
    private String apiKey;
    private String apiSecretKey;
    private String ownerShipId;
    private String subject;
    private List<String> emails;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public void setOwnerShipId(String ownerShipId) {
        this.ownerShipId = ownerShipId;
    }

    public void setApiSecretKey(String apiSecretKey) {
        this.apiSecretKey = apiSecretKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecretKey() {
        return apiSecretKey;
    }

    public String getOwnerShipId() {
        return ownerShipId;
    }

    public List<String> getEmails() {
        return emails;
    }
}
