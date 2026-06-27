package ca.umika.api.menu;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class MenuItemImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/avif"
    );

    private final Path storageRoot;

    public MenuItemImageStorageService(
            @Value("${app.upload.base-dir:uploads}") String uploadBaseDir
    ) {
        this.storageRoot = Paths.get(uploadBaseDir).toAbsolutePath().normalize().resolve("menu-item-images").normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(storageRoot);
            Path target = storageRoot.resolve(filename).normalize();
            file.transferTo(target);
            return filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image", e);
        }
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String filename = extractFilename(imageUrl);
        Path target = storageRoot.resolve(filename).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image path");
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete image", e);
        }
    }

    private String extractFilename(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            Path path = Paths.get(uri.getPath());
            Path filename = path.getFileName();
            if (filename == null) {
                throw new IllegalArgumentException("Missing filename");
            }
            return filename.toString();
        } catch (Exception ex) {
            int slash = imageUrl.lastIndexOf('/');
            if (slash >= 0 && slash < imageUrl.length() - 1) {
                return imageUrl.substring(slash + 1);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image URL", ex);
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return ".jpg";
            }
            if (lower.endsWith(".png")) {
                return ".png";
            }
            if (lower.endsWith(".webp")) {
                return ".webp";
            }
            if (lower.endsWith(".gif")) {
                return ".gif";
            }
            if (lower.endsWith(".avif")) {
                return ".avif";
            }
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/avif" -> ".avif";
            default -> "";
        };
    }
}
