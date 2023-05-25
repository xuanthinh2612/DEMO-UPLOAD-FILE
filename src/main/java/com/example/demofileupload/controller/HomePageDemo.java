package com.example.demofileupload.controller;

import com.example.demofileupload.model.Contact;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/")
public class HomePageDemo {

    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";

    private final Path root = Paths.get("c:/demo/uploads/");
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    @GetMapping("homePage")
    public String showHomePage(Model model){
        return "index";
    }

    @PostMapping("uploadFile")
    public String uploadOnlyOneFile(@RequestParam("file") MultipartFile file, Model model){
        String fileName = timestamp.getTime() + file.getOriginalFilename();
        try {
            Files.copy(file.getInputStream(), this.root.resolve(Objects.requireNonNull(fileName)));
        } catch (Exception e) {
            e.printStackTrace();
                throw new RuntimeException("A file of that name already exists.");
        }

        model.addAttribute("fileName", fileName);
        return "uploadSuccess";
    }

    @PostMapping("uploadMultiFile")
    public String uploadOnlyOneFile(@RequestParam("multiFile") MultipartFile[] files, Model model){
        List<String> fileList = new ArrayList<>();


        try {

            for (MultipartFile file : files) {
                String fileName = timestamp.getTime() + file.getOriginalFilename();
                fileList.add(fileName);
                Files.copy(file.getInputStream(), this.root.resolve(Objects.requireNonNull(fileName)));
            }
        } catch (Exception e) {
            e.printStackTrace();
                throw new RuntimeException("A file of that name already exists.");
        }

        model.addAttribute("fileList", fileList);
        return "listFile";
    }

    @PostMapping("uploadAndMergePdf")
    public String uploadAndMergePdfFile(@RequestParam("pdfFiles") MultipartFile[] pdfFiles, Model model) throws IOException {
        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        List<File> fileList = new ArrayList<>();

        try {
            for (MultipartFile file : pdfFiles) {

                String fileName = timestamp.getTime() + file.getOriginalFilename();

                Files.copy(file.getInputStream(), this.root.resolve(fileName));

                File pdfFile = new File(this.root.resolve(Objects.requireNonNull(fileName)).toUri());
                file.transferTo(pdfFile);
                fileList.add(pdfFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("A file of that name already exists.");
        }

        String mergeFileName = timestamp.getTime() + "-merged.pdf";

        pdfMergerUtility.setDestinationFileName(String.valueOf(this.root.resolve( mergeFileName)));

        for(File file : fileList){
            pdfMergerUtility.addSource(file);
        }

        pdfMergerUtility.mergeDocuments();

        model.addAttribute("fileName", mergeFileName );
        return "uploadSuccess";
    }

    @GetMapping("downloadMergeFile/{fileName}")
    public ResponseEntity<Resource> downloadMergeFile(@PathVariable("fileName") String fileName) throws IOException {
        File file = new File(this.root.resolve(Objects.requireNonNull(fileName)).toUri());

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment");
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);

    }

    @GetMapping("deleteFile/{fileName}")
    public String deleteFile(@PathVariable("fileName") String fileName) {
        File file = new File(this.root.resolve(Objects.requireNonNull(fileName)).toUri());
        file.delete();
        return "redirect:/homePage";
    }

    @PostMapping("/createCSV")
    public ResponseEntity<Resource> createCSVFile(@ModelAttribute("contact") Contact contact) throws IOException {
        String fileName = timestamp.getTime() + "-contact.csv";
         File file = new File(root.resolve(fileName).toUri());

        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                String name = contact.getName();
                int phone = contact.getPhoneNumber();
                String group = contact.getGroup();
                String gender = contact.getGender();
                String address = contact.getAddress();
                String mail = contact.getEmail();
                String dateOfBirth = contact.getDateOfBirth();

                String line = name + COMMA_DELIMITER + phone + COMMA_DELIMITER + group + COMMA_DELIMITER
                        + gender + COMMA_DELIMITER + address + COMMA_DELIMITER + mail + COMMA_DELIMITER
                        + dateOfBirth + NEW_LINE_SEPARATOR;
                bufferedWriter.write(line);

            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;  filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1) );
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(resource);
    }

    @PostMapping("/readCSV")
    public String readCSVFile(@RequestParam("csvFile") MultipartFile file, Model model) throws IOException {

        String fileName = timestamp.getTime() + file.getOriginalFilename();

        Files.copy(file.getInputStream(), this.root.resolve(fileName));

        File csvFile = new File(this.root.resolve(Objects.requireNonNull(fileName)).toUri());

        Contact contact = new Contact();
        try {
            FileReader fileReader = new FileReader(csvFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                contact = splitString(line);
            }
            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        model.addAttribute("contact",  contact);
        return "contactInfo";
    }

    private static Contact splitString(String string) {
        String[] splitData = string.split(COMMA_DELIMITER);
        String fullName = splitData[0];
        int phoneNumber = Integer.parseInt(splitData[1]);
        String group = splitData[2];
        String gender = splitData[3];
        String address = splitData[4];
        String mail = splitData[5];
        String dateOfBirth = splitData[6];
        Contact contact = new Contact( phoneNumber,fullName, group, gender, address, mail, dateOfBirth);
        return contact;
    }


}
