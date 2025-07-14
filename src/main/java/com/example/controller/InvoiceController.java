package com.example.controller;

import com.example.model.Invoice;
import com.example.service.ExcelReaderService;
import com.example.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class InvoiceController {

    @Autowired
    InvoiceService invoiceService;
    @Autowired
    ExcelReaderService excelReaderService;
    @PostMapping("/api/invoice")
    public int sendInvoice(@RequestBody Invoice invoice)
    {
      return invoiceService.sendInvoice(invoice.getApiKey(), invoice.getApiSecretKey(), invoice.getOwnerShipId(), invoice.getEmails());

    }
    @PostMapping("/api/v2/invoice")
    public String sendInvoice(
            @RequestParam("apiKey") String apiKey,
            @RequestParam("apiSecretKey") String apiSecretKey,
            @RequestParam("ownerShipId") String ownerShipId,
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
            int status=invoiceService.sendInvoice(apiKey, apiSecretKey, ownerShipId, batchEmail);
            if(status==0)
            {
                return "Failed: Email Not Sent";
            }
        }

        return "Success : Email Sent";
    }
}
