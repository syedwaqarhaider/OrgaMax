package com.example.controller;

import com.example.model.Invoice;
import com.example.service.ExcelReaderService;
import com.example.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class InvoiceController {

    @Autowired
    InvoiceService invoiceService;
    @Autowired
    ExcelReaderService excelReaderService;
    @CrossOrigin
    @PostMapping("/api/invoice")
    public int sendInvoice(@RequestBody Invoice invoice)
    {
      return invoiceService.sendInvoice(invoice.getApiKey(), invoice.getApiSecretKey(), invoice.getOwnerShipId(), invoice.getSubject(), invoice.getEmails());

    }
    @CrossOrigin
    @PostMapping("/api/v2/invoice")
    public String sendInvoice(
            @RequestParam("apiKey") String apiKey,
            @RequestParam("apiSecretKey") String apiSecretKey,
            @RequestParam("ownerShipId") String ownerShipId,
            @RequestParam ("subject") String subject,
            @RequestPart("file") MultipartFile file) {

        // 1️⃣ Read all emails from file
        List<String> emails = excelReaderService.readEmails(file);

        // 2️⃣ Define batch size
        int batchSize = 20;

        // 3️⃣ Process batches one by one
        for (int i = 0; i < emails.size(); i += batchSize) {
            int end = Math.min(i + batchSize, emails.size());
            List<String> batchEmail = emails.subList(i, end);

            // 4️⃣ Call invoiceService for each batch
            int status=invoiceService.sendInvoice(apiKey, apiSecretKey, ownerShipId, subject, batchEmail);
            if(status==0)
            {
                return "Failed: Email Not Sent";
            }
        }

        return "Success : Email Sent";
    }

    @CrossOrigin
    @GetMapping("/api/v3/invoice")
    public SseEmitter streamInvoice(
            @RequestParam String apiKey,
            @RequestParam String apiSecretKey,
            @RequestParam String ownerShipId,
            @RequestParam String subject,
            @RequestPart MultipartFile file
    ) {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                List<String> emails = excelReaderService.readEmails(file);
                int batchSize = 20;
                for (int i = 0; i < emails.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, emails.size());
                    List<String> batchEmail = emails.subList(i, end);

                    int status=invoiceService.sendInvoice(apiKey, apiSecretKey, ownerShipId, subject, batchEmail);

                    if(status!=0)
                    {
                        emitter.send("Batch " + (i/batchSize + 1) + " completed.");
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }
}
