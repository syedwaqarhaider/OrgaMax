package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelReaderService {
    private List<String> apiKey;
    private List<String> apiSecretLKey;
    private List<String> ownerShipID;
    private List<String> subject;

    private List<String> signEasyApiToken;
    private List<String> signEasySubject;

    public List<String> getSignEasyApiToken() {
        return signEasyApiToken;
    }

    public List<String> getSignEasySubject() {
        return signEasySubject;
    }


    public List<String> getApiKey() {
        return apiKey;

    }

    public List<String> getApiSecretLKey() {
        return apiSecretLKey;
    }

    public List<String> getOwnerShipID() {
        return ownerShipID;
    }

    public List<String> getSubject() {
        return subject;
    }

    public boolean removeAccount(int index)
    {
        apiKey.remove(index);
        apiSecretLKey.remove(index);
        ownerShipID.remove(index);
        subject.remove(index);
        return true;
    }
    public boolean removeAccountSignEasy(int index)
    {
        signEasyApiToken.remove(index);
        signEasySubject.remove(index);
        return true;
    }
    public int getAccoutCount()
    {
        if (apiKey !=null) {
            return apiKey.size();
        }
        else {
            return 0;
        }
    }

    public int getSignEasyAccoutCount()
    {
        if (signEasyApiToken !=null) {
            return signEasyApiToken.size();
        }
        else {
            return 0;
        }
    }



    public List<String> readEmails(MultipartFile file) {
        List<String> emails = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    line = removeBom(line);
                    firstLine = false;
                }
                System.out.println("Line: " + line);
                String[] parts = line.split(",");
                for (String part : parts) {
                    String email = part.trim();
                    if (!email.isBlank()) {
                        emails.add(email);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV file", e);
        }
        return emails.stream()
                .distinct()
                .collect(Collectors.toList());
    }
    public String readAccounts(MultipartFile file) {
        int i=0;
        this.apiKey= new ArrayList<>();
        this.apiSecretLKey=new ArrayList<>();
        this.ownerShipID= new ArrayList<>();
        this.subject= new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    line = removeBom(line);
                    firstLine = false;
                }
                System.out.println("Line: " + line);
                String[] parts = line.split(",");
                apiKey.add(parts[0].trim());
                apiSecretLKey.add(parts[1].trim());
                ownerShipID.add(parts[2].trim());
                subject.add(parts[3].trim());
            }

        } catch (Exception e) {
            i=1;
            throw new RuntimeException("Failed to read CSV file", e);
        }
        if (i==0)
        {
            return "yes";
        }
        else {
            return "no";
        }

    }

    public String readAccountsSigneasy(MultipartFile file) {
        int i=0;
        this.signEasyApiToken= new ArrayList<>();
        this.signEasySubject= new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    line = removeBom(line);
                    firstLine = false;
                }
                System.out.println("Line: " + line);
                String[] parts = line.split(",");
                signEasyApiToken.add(parts[0].trim());
                signEasySubject.add(parts[1].trim());
            }

        } catch (Exception e) {
            i=1;
            throw new RuntimeException("Failed to read CSV file", e);
        }
        if (i==0)
        {
            return "yes";
        }
        else {
            return "no";
        }

    }

    private String removeBom(String line) {
        if (line.startsWith("\uFEFF")) {
            return line.substring(1);
        }
        return line;
    }
}
