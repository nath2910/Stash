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

    String originalName = file.getOriginalFilename();
    if (originalName == null || originalName.length() > 180 || originalName.contains("..")
        || originalName.contains("/") || originalName.contains("\\")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide");
    }
    String mime = validateContent(file);

    String safeBase = originalName.replaceAll("[^a-zA-Z0-9_.-]", "_");
    String filename = java.util.UUID.randomUUID() + "_" + safeBase;
    Path userDir = root.resolve(String.valueOf(userId)).resolve(String.valueOf(itemId));
    try {
      Files.createDirectories(userDir);
      Path target = userDir.resolve(filename);
      try (InputStream in = file.getInputStream()) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }

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

  public void deleteUserFiles(Long userId) {
    if (userId == null || userId <= 0) throw new IllegalArgumentException("Invalid user");
    Path directory = root.resolve(userId.toString()).normalize();
    if (!directory.startsWith(root) || directory.equals(root)) throw new IllegalArgumentException("Invalid path");
    if (!Files.exists(directory)) return;
    try (var paths = Files.walk(directory)) {
      for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
    } catch (IOException ex) {
      org.slf4j.LoggerFactory.getLogger(FileStorageService.class).error("Account file cleanup requires retry for user {}", userId);
    }
  }

  static String validateContent(MultipartFile file) {
    String name = String.valueOf(file.getOriginalFilename()).toLowerCase(java.util.Locale.ROOT);
    try (InputStream input = file.getInputStream()) {
      byte[] bytes = input.readNBytes(16);
      String ascii = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
      String mime = null;
      if (name.endsWith(".pdf") && ascii.startsWith("%PDF-")) mime = "application/pdf";
      else if ((name.endsWith(".jpg") || name.endsWith(".jpeg")) && bytes.length >= 3
          && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) mime = "image/jpeg";
      else if (name.endsWith(".png") && bytes.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(bytes, 8),
          new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10})) mime = "image/png";
      else if (name.endsWith(".webp") && ascii.startsWith("RIFF") && ascii.substring(8).startsWith("WEBP")) mime = "image/webp";
      if (mime == null || (file.getContentType() != null && !mime.equalsIgnoreCase(file.getContentType())
          && !"application/octet-stream".equals(file.getContentType()))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier invalide : PDF, JPEG, PNG ou WebP requis");
      }
      return mime;
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier illisible");
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
