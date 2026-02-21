package com.howners.gestion.controller;

import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class TestPresignerController {

    private final StorageService storageService;

    @GetMapping("/presigned-url")
    public String testPresignedUrl(@RequestParam String fileKey) {
        return storageService.generatePresignedUrl(fileKey);
    }
}
