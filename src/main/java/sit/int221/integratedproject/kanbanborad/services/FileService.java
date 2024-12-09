package sit.int221.integratedproject.kanbanborad.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.properties.FileStorageProperties;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@Service
@Getter
public class FileService {
    private final Path fileStorageLocation;

    @Autowired
    public FileService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties
                .getUploadDir()).toAbsolutePath().normalize();
        try {
            if (!Files.exists(this.fileStorageLocation)) {
                Files.createDirectories(this.fileStorageLocation);
            }
        } catch (IOException ex) {
            throw new ItemNotFoundException(
                    "Could not create the directory where the uploaded files will be stored.");
        }
    }

    public String store(MultipartFile file, String boardId, Integer taskId) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (fileName.contains("..")) {
                throw new ItemNotFoundException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetDirectory = this.fileStorageLocation.resolve(boardId).resolve(taskId.toString());
            if (!Files.exists(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }

            Path targetLocation = targetDirectory.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetLocation.toString();
        } catch (IOException ex) {
            throw new ItemNotFoundException("Could not store file " + fileName + ". Please try again!");
        }
    }

    public void deleteFile(String boardId, Integer taskId, String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(boardId).resolve(taskId.toString()).resolve(fileName).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            } else {
                throw new ItemNotFoundException("File not found " + fileName);
            }
        } catch (IOException ex) {
            throw new ItemNotFoundException("File operation error: " + fileName);
        }
    }

    public Resource loadFileAsResource(String boardId, Integer taskId, String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(boardId).resolve(taskId.toString()).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ItemNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new ItemNotFoundException("File operation error: " + fileName);
        }
    }
}