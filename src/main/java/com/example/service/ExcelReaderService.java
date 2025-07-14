package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelReaderService {

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
        return emails;
    }

    private String removeBom(String line) {
        if (line.startsWith("\uFEFF")) {
            return line.substring(1);
        }
        return line;
    }
}
