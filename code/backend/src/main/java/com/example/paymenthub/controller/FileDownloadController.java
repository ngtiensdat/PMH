package com.example.paymenthub.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller phục vụ tải file nội bộ khi dùng LocalStorage (không có MinIO).
 */
@RestController
@RequestMapping("/api/downloads/exports")
@Slf4j
public class FileDownloadController {

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadLocalFile(@PathVariable String fileName) {
        try {
            String safeFileName = Paths.get(fileName).getFileName().toString();
            Path exportsDir = Paths.get("./exports").toAbsolutePath().normalize();
            Path filePath = exportsDir.resolve(safeFileName).normalize();

            if (!filePath.startsWith(exportsDir)) {
                return ResponseEntity.badRequest().build();
            }

            if (!Files.exists(filePath)) {
                log.warn("[Download] File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());

            String contentType = "application/octet-stream";
            if (safeFileName.endsWith(".xlsx")) {
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else if (safeFileName.endsWith(".csv")) {
                contentType = "text/csv; charset=UTF-8";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("[Download] Lỗi khi stream file {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
