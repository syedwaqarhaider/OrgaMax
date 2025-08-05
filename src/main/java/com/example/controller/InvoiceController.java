package com.example.controller;

import com.example.model.Invoice;
import com.example.service.ExcelReaderService;
import com.example.service.InvoiceService;
import com.example.service.SignEasyClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.InputStream;
import java.util.List;

@RestController
public class InvoiceController {

    private InputStream pdfFile;
    private byte[] fileByteArray;
    private String pdfFileName;
    private long pdfFileSize;

    @Autowired
    InvoiceService invoiceService;
    @Autowired
    ExcelReaderService excelReaderService;

    @Autowired
    SignEasyClientService signEasyClientService;

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
            return excelReaderService.readAccounts(file); //This is for OrgaMAX
            //return  excelReaderService.readAccountsSigneasy(file);
        }

    }

    @CrossOrigin
    @PostMapping("api/signeasy/load")
    public String loadAccountsSignEasy(@RequestPart("file") MultipartFile file) {
        if(excelReaderService.getSignEasyAccoutCount()>0)
        {
            return "Accounts already remining, no need for new accounts!!!";
        }
        else {
            return  excelReaderService.readAccountsSigneasy(file);
        }

    }

    @CrossOrigin
    @PostMapping("/api/v3/invoice")
    public SseEmitter sendInvoice(
            @RequestPart("file") MultipartFile file
    )  {
        SseEmitter emitter = new SseEmitter(0L);
        new Thread(() -> {
            try {
                List<String> emails = excelReaderService.readEmails(file);
                int batchSize = 20;
                    if (excelReaderService.getAccoutCount()>0) {
                        String apiKey = excelReaderService.getApiKey().get(0);
                        String apiSecretKey = excelReaderService.getApiSecretLKey().get(0);
                        String ownerShipId = excelReaderService.getOwnerShipID().get(0);
                        String subject = excelReaderService.getSubject().get(0);

                        System.out.println("Account Details : ");
                        System.out.println(apiKey);
                        System.out.println(apiSecretKey);
                        System.out.println(ownerShipId);
                        System.out.println(subject);
                        System.out.println("===================================");
                        int i=0;
                        while (i < emails.size()) {
                            int end = Math.min(i + batchSize, emails.size());
                            List<String> batchEmail = emails.subList(i, end);

                            int status = invoiceService.sendInvoice(apiKey, apiSecretKey, ownerShipId, subject, batchEmail);

                            if (status == 1) {
                                emitter.send("Batch " + (i / batchSize + 1) + " completed.");
                                i += batchSize;
                            } else if(status<3){
                                if(status == 0) {
                                    emitter.send("---> This account has been Completed");
                                }
                                else {
                                    emitter.send("---> This account dont enabled \"Edit Access\"" +
                                            "\napiKey : "+excelReaderService.getApiKey().get(0)+"" +
                                            "\napiSecretKey : "+excelReaderService.getApiSecretLKey().get(0)+"" +
                                            "\napiOwnerShipId : "+excelReaderService.getOwnerShipID().get(0)+"" +
                                            "\nSkipping This Account for Now.");
                                }
                                excelReaderService.removeAccount(0);
                                if (excelReaderService.getAccoutCount() > 0) {
                                    apiKey = excelReaderService.getApiKey().get(0);
                                    apiSecretKey = excelReaderService.getApiSecretLKey().get(0);
                                    ownerShipId = excelReaderService.getOwnerShipID().get(0);
                                    subject = excelReaderService.getSubject().get(0);
                                    emitter.send("---> Switched to New Account");
                                } else {
                                    emitter.send("---> Account List Empty");
                                    emitter.send("Proccesed Emails : " + (i>0? (i / batchSize + 1) * 20 : 0));
                                    break;
                                }
                            }
                            else {
                                emitter.send("Email has wrong format in this Batch : \n" +
                                        batchEmail);
                            }

                        }
                        if (excelReaderService.getAccoutCount() > 0) {
                            emitter.send("All the emails prcessed");
                            emitter.send("Remaining Accounts : " + excelReaderService.getAccoutCount());
                            emitter.send("Just Upload new emails only!!!");
                        }

                        emitter.complete();
                    }
                    else {
                        emitter.send("Accounts Not loadded Yet...");
                        emitter.complete();
                    }

                } catch(Exception e){
                    System.out.println("Emitter : " + e.getMessage());
                    emitter.completeWithError(e);
                }


        }).start();
        return emitter;
    }

    //For Sign Easy
    @CrossOrigin
    @PostMapping("/api/v3/upload")
    public String forwardToSignEasy(@RequestPart("uploadPdfFile") MultipartFile uploadPdfFile) throws Exception {
        fileByteArray=uploadPdfFile.getBytes();
        pdfFile=uploadPdfFile.getInputStream();
        pdfFileName=uploadPdfFile.getOriginalFilename();
        pdfFileSize=uploadPdfFile.getSize();
        System.out.println("File : "+pdfFile);
        System.out.println("File Name : "+pdfFileName);
        System.out.println("File Size : "+pdfFileSize);
        return "File has been Uploaded!!!";
    }

    //This for SignEasy
    @CrossOrigin
    @PostMapping("/api/signeasy/invoice")
    public SseEmitter sendInvoice(
            @RequestPart("file") MultipartFile file,
            @RequestPart("message") String message
    )  {
        SseEmitter emitter = new SseEmitter(0L);
        new Thread(() -> {
            try {
                List<String> emails = excelReaderService.readEmails(file);
                int batchSize = 20;
                if (excelReaderService.getSignEasyAccoutCount()>0) {
                    String apiKey = excelReaderService.getSignEasyApiToken().get(0);
                    String subject = excelReaderService.getSignEasySubject().get(0);
                    int docId=signEasyClientService.uploadOriginal(apiKey, fileByteArray, pdfFileName);

                    System.out.println("Account Details : ");
                    System.out.println(apiKey);
                    System.out.println(subject);
                    System.out.println("Message : "+ message);
                    System.out.println("===================================");
                    int i=0;
                    while (i < emails.size()) {
                        int end = Math.min(i + batchSize, emails.size());
                        List<String> batchEmail = emails.subList(i, end);

                        int status = signEasyClientService.sendEnvelope(apiKey,
                                true,
                                false,
                                message,
                                signEasyClientService.getSources(docId, subject),
                                signEasyClientService.getRecipients(batchEmail),
                                signEasyClientService.getPlayloadFields());

                        if (status == 1) {
                            emitter.send("Batch " + (i / batchSize + 1) + " completed.");
                            i += batchSize;
                        } else if(status<3){
                            if(status == 0) {
                                emitter.send("---> This account has been Completed");
                            }

                            excelReaderService.removeAccountSignEasy(0);
                            if (excelReaderService.getSignEasyAccoutCount() > 0) {
                                System.out.println("New Account loading.....");
                                apiKey = excelReaderService.getSignEasyApiToken().get(0);
                                subject = excelReaderService.getSignEasySubject().get(0);
                                System.out.println("Account Details : ");
                                System.out.println(apiKey);
                                System.out.println(subject);

                                System.out.println("File : "+pdfFile);
                                System.out.println("File Name : "+pdfFileName);
                                System.out.println("File Size : "+pdfFileSize);
                                System.out.println("===================================");
                                docId=signEasyClientService.uploadOriginal(apiKey, fileByteArray, pdfFileName);

                                emitter.send("---> Switched to New Account");
                            } else {
                                emitter.send("---> Account List Empty");
                                emitter.send("Proccesed Emails : " + (i>0? (i / batchSize + 1) * 20 : 0));
                                break;
                            }
                        }
                        else {
                            emitter.send("Email has wrong format in this Batch : \n" +
                                    batchEmail);
                        }

                    }
                    if (excelReaderService.getSignEasyAccoutCount() > 0) {
                        emitter.send("All the emails prcessed");
                        emitter.send("Remaining Accounts : " + excelReaderService.getSignEasyAccoutCount());
                        emitter.send("Just Upload new emails only!!!");
                    }

                    emitter.complete();
                }
                else {
                    emitter.send("Accounts Not loadded Yet...");
                    emitter.complete();
                }

            } catch(Exception e){
                System.out.println("Emitter : " + e.getMessage());
                emitter.completeWithError(e);
            }


        }).start();
        return emitter;
    }
    /////////////////////////////////////////////


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
