package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelReaderService {

    public List<String> readEmails(MultipartFile file) {
        List<String> emails = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Some rows might have one email OR multiple emails separated by commas
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
        return emails;
    }
}
