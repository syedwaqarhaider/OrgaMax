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
    @PostMapping("api/v3/load")
    public String loadAccounts(@RequestPart("file") MultipartFile file) {
        if(excelReaderService.getAccoutCount()>0)
        {
            return "Accounts already remining, no need for new accounts!!!";
        }
        else {
            return excelReaderService.readAccounts(file);
        }

    }

    @CrossOrigin
    @PostMapping("/api/v3/invoice")
    public SseEmitter sendInvoice(
            @RequestPart("file") MultipartFile file
    )  {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                List<String> emails = excelReaderService.readEmails(file);
                int batchSize = 20;
                String apiKey=excelReaderService.getApiKey().get(0);
                String apiSecretKey= excelReaderService.getApiSecretLKey().get(0);
                String ownerShipId=excelReaderService.getOwnerShipID().get(0);
                String subject=excelReaderService.getSubject().get(0);

                System.out.println("Account Details : ");
                System.out.println(apiKey);
                System.out.println(apiSecretKey);
                System.out.println(ownerShipId);
                System.out.println(subject);
                System.out.println("===================================");

                for (int i = 0; i < emails.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, emails.size());
                    List<String> batchEmail = emails.subList(i, end);

                    int status=invoiceService.sendInvoice(apiKey, apiSecretKey, ownerShipId, subject, batchEmail);

                    if(status!=0)
                    {
                        emitter.send("Batch " + (i/batchSize + 1) + " completed.");
                    }
                    else
                    {
                        emitter.send("---> This account has been Completed");

                        excelReaderService.removeAccount(0);
                        if(excelReaderService.getAccoutCount()>0)
                        {
                        apiKey=excelReaderService.getApiKey().get(0);
                         apiSecretKey= excelReaderService.getApiSecretLKey().get(0);
                         ownerShipId=excelReaderService.getOwnerShipID().get(0);
                         subject=excelReaderService.getSubject().get(0);
                        emitter.send("---> Switched to New Account");
                        }
                        else {
                            emitter.send("---> Account List Empty");
                            emitter.send("Proccesed Emails : "+(i/batchSize + 1)*20);
                            break;
                        }
                    }
                }
                if(excelReaderService.getAccoutCount()>0)
                {
                    emitter.send("All the emails prcessed");
                    emitter.send("Remaining Accounts : "+ excelReaderService.getAccoutCount());
                    emitter.send("Just Upload new emails only!!!");
                }

                emitter.complete();
            } catch (Exception e) {
                System.out.println("Emitter : "+e.getMessage());
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }


    @CrossOrigin
    @PostMapping("/api/v4/invoice")
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
