package sit.int221.integratedproject.kanbanborad.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sit.int221.integratedproject.kanbanborad.properties.FileStorageProperties;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            throw new RuntimeException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String store(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (IOException ex) {
            throw new RuntimeException("File operation error: " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File operation error: " + fileName, ex);
        }
    }

    public List<String> getAllFiles() {
        try (Stream<Path> stream = Files.list(fileStorageLocation)) {
            return stream.filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new RuntimeException("File Listing Error", ex);
        }
    }

    public void updateFileMetadata(String oldFileName, String newFileName) {
        try {
            Path oldFilePath = this.fileStorageLocation.resolve(oldFileName).normalize();
            Path newFilePath = this.fileStorageLocation.resolve(newFileName).normalize();
            if (Files.exists(oldFilePath)) {
                Files.move(oldFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new RuntimeException("File not found " + oldFileName);
            }
        } catch (IOException ex) {
            throw new RuntimeException("File operation error: " + oldFileName, ex);
        }
    }

    public void copyFile(String sourceFileName, String targetFileName) {
        try {
            Path sourceFilePath = this.fileStorageLocation.resolve(sourceFileName).normalize();
            Path targetFilePath = this.fileStorageLocation.resolve(targetFileName).normalize();
            Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not copy file " + sourceFileName + " to " + targetFileName, ex);
        }
    }

//    public void moveFile(String sourceFileName, String targetFileName) {
//        try {
//            Path sourceFilePath = this.fileStorageLocation.resolve(sourceFileName).normalize();
//            Path targetFilePath = this.fileStorageLocation.resolve(targetFileName).normalize();
//            Files.move(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
//        } catch (IOException ex) {
//            throw new RuntimeException("Could not move file " + sourceFileName + " to " + targetFileName, ex);
//        }
//    }

    public String readFileAsString(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.readString(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not read file " + fileName, ex);
        }
    }

    public void writeStringToFile(String fileName, String content) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not write to file " + fileName, ex);
        }
    }

    public long getFileSize(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.size(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not get size of file " + fileName, ex);
        }
    }

    public FileTime getLastModifiedTime(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.getLastModifiedTime(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not get last modified time of file " + fileName, ex);
        }
    }


}
