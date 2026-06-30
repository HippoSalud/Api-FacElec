package com.factura.demo.infrastructure.adapter.out.storage;

import com.factura.demo.application.port.out.LocalDocumentStoragePort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalFileSystemStorageAdapter implements LocalDocumentStoragePort {

    private final String baseDir;

    public LocalFileSystemStorageAdapter(String baseDir) {
        this.baseDir = baseDir;
        // Ensure directory exists
        try {
            Files.createDirectories(Paths.get(baseDir));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio base para los comprobantes: " + baseDir, e);
        }
    }

    @Override
    public String saveXml(String documentId, String xmlContent, String documentType) {
        if (xmlContent == null || xmlContent.isBlank()) {
            return null;
        }

        try {
            String fileName = documentType + "-" + documentId + ".xml";
            Path filePath = Paths.get(baseDir, fileName);
            Files.writeString(filePath, xmlContent);
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el XML en disco: " + e.getMessage(), e);
        }
    }
}
