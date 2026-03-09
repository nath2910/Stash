package backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class FileStorageService {

  private final Path root;
  private final long maxBytes;

  public FileStorageService(
      @Value("${app.storage.root:uploads}") String rootDir,
      @Value("${app.storage.max-size-bytes:10485760}") long maxBytes // 10 MB par défaut
  ) {
    this.root = Paths.get(rootDir).toAbsolutePath().normalize();
    this.maxBytes = maxBytes;
  }

  public StoredFile store(MultipartFile file, Long userId, Integer itemId) {
    Objects.requireNonNull(file, "file");
    if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide");
    if (file.getSize() > maxBytes) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier trop volumineux (max " + maxBytes + " octets)");
    }

    String originalName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename(), "filename"));
    if (originalName.contains("..")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide");

    String safeBase = originalName.replaceAll("[^a-zA-Z0-9_.-]", "_");
    String filename = System.currentTimeMillis() + "_" + safeBase;
    Path userDir = root.resolve(String.valueOf(userId)).resolve(String.valueOf(itemId));
    try {
      Files.createDirectories(userDir);
      Path target = userDir.resolve(filename);
      try (InputStream in = file.getInputStream()) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }

      String mime = detectMime(target, file.getContentType());
      return new StoredFile(target, mime, file.getSize(),
          root.relativize(target).toString().replace('\\', '/'), filename);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de stocker le fichier", e);
    }
  }

  public Resource loadAsResource(String storageKey) {
    try {
      Path file = root.resolve(storageKey).normalize();
      if (!file.startsWith(root)) throw new IllegalArgumentException("Chemin invalide");
      Resource resource = new UrlResource(file.toUri());
      if (resource.exists() && resource.isReadable()) {
        return resource;
      }
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur d'accès fichier", e);
    }
  }

  public boolean delete(String storageKey) {
    try {
      Path file = root.resolve(storageKey).normalize();
      if (!file.startsWith(root)) return false;
      return Files.deleteIfExists(file);
    } catch (IOException e) {
      return false;
    }
  }

  private String detectMime(Path path, String fallback) throws IOException {
    String probe = Files.probeContentType(path);
    if (probe != null) return probe;
    if (fallback != null) return fallback;
    try (InputStream in = Files.newInputStream(path)) {
      String guess = URLConnection.guessContentTypeFromStream(in);
      return guess != null ? guess : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
  }

  public record StoredFile(Path absolutePath, String mimeType, long sizeBytes, String storageKey, String filename) {}
}
