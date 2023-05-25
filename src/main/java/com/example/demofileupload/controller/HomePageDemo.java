package com.example.demofileupload.controller;

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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    private final Path root = Paths.get("c:/demo/uploads/");

    @GetMapping("homePage")
    public String showHomePage(Model model){
        return "index";
    }

    @PostMapping("uploadFile")
    public String uploadOnlyOneFile(@RequestParam("file") MultipartFile file, Model model){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
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
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

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
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
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

}
