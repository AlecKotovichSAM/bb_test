package eu.bb.app.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;

@Service
public class StorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private final Path base = Paths.get("storage");

    public StorageService() throws IOException {
        Files.createDirectories(base);
        log.info("StorageService initialized. Base directory: {}", base.toAbsolutePath());
    }

    public String save(byte[] data, String filename) throws IOException {
        log.debug("Saving file: {} (size: {} bytes)", filename, data.length);
        Path p = base.resolve(filename);
        Files.write(p, data);
        String absolutePath = p.toAbsolutePath().toString();
        log.info("File saved successfully: {}", absolutePath);
        return absolutePath;
    }
}
