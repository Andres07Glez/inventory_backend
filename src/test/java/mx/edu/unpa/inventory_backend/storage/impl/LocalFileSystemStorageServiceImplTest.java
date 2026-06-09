package mx.edu.unpa.inventory_backend.storage.impl;

import mx.edu.unpa.inventory_backend.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class LocalFileSystemStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private StorageProperties props;

    private LocalFileSystemStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        // Usamos lenient() para decirle a Mockito que no sea estricto si
        // alguna prueba específica no usa todas estas configuraciones.
        lenient().when(props.uploadDir()).thenReturn(tempDir.toString());
        lenient().when(props.allowedMimeTypes()).thenReturn(List.of("image/jpeg", "image/png", "application/pdf"));
        lenient().when(props.baseUrl()).thenReturn("http://localhost:8080");

        storageService = new LocalFileSystemStorageServiceImpl(props);
    }

    // =========================================================================
    // Constructor — inicialización del directorio
    // =========================================================================

    @Test
    void should_createUploadDirectory_when_instantiated() {
        // El directorio ya lo crea @TempDir; verificamos que el constructor
        // no lanza excepción aunque el directorio ya exista (idempotente).
        assertThat(tempDir).exists().isDirectory();
    }

    @Test
    void should_throwIllegalStateException_when_uploadDirCannotBeCreated() throws IOException {
        // Arrange — creamos un ARCHIVO donde el constructor esperará crear un DIRECTORIO.
        // Files.createDirectories() siempre falla en este caso, en cualquier SO y usuario.
        Path fileAsDir = tempDir.resolve("bloqueo");
        Files.createFile(fileAsDir); // es un archivo, no un directorio

        when(props.uploadDir()).thenReturn(fileAsDir.resolve("sub").toString());

        // Act & Assert
        assertThatThrownBy(() -> new LocalFileSystemStorageServiceImpl(props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo crear el directorio de almacenamiento");
    }

    // =========================================================================
    // store()
    // =========================================================================

    @Test
    void should_storeFileAndReturnRelativePath_when_validFileProvided() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "contenido".getBytes());

        // Act
        String relativePath = storageService.store(file, "assets");

        // Assert
        assertThat(relativePath).startsWith("assets/").endsWith(".jpg");
        assertThat(tempDir.resolve(relativePath)).exists();
    }

    @Test
    void should_generateUniqueFilename_when_twoFilesWithSameNameStored() {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "mismo.jpg", "image/jpeg", "contenido1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "mismo.jpg", "image/jpeg", "contenido2".getBytes());

        // Act
        String path1 = storageService.store(file1, "assets");
        String path2 = storageService.store(file2, "assets");

        // Assert — UUID garantiza unicidad
        assertThat(path1).isNotEqualTo(path2);
    }

    @Test
    void should_createSubdirectory_when_subDirDoesNotExist() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "bytes".getBytes());

        // Act
        String relativePath = storageService.store(file, "documentos/2026");

        // Assert
        assertThat(tempDir.resolve(relativePath)).exists();
    }

    @Test
    void should_throwBadRequest_when_fileIsEmpty() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        // Act & Assert
        assertThatThrownBy(() -> storageService.store(emptyFile, "assets"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void should_throwBadRequest_when_fileIsNull() {
        assertThatThrownBy(() -> storageService.store(null, "assets"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void should_throwUnsupportedMediaType_when_mimeTypeNotAllowed() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/x-msdownload", "bytes".getBytes());

        // Act & Assert
        assertThatThrownBy(() -> storageService.store(file, "assets"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void should_throwUnsupportedMediaType_when_contentTypeIsNull() {
        // Arrange — MockMultipartFile con contentType null
        MockMultipartFile file = new MockMultipartFile(
                "file", "archivo.jpg", null, "bytes".getBytes());

        // Act & Assert
        assertThatThrownBy(() -> storageService.store(file, "assets"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @ParameterizedTest(name = "Debe usar extensión .jpg cuando el filename es: {0}")
    @NullSource // Esto inyecta el valor 'null' en la variable 'filename'
    @ValueSource(strings = {"sin_extension", "FOTO.JPG"}) // Esto inyecta los otros dos valores
    void should_resolveToJpgExtension_when_edgeCaseFilenames(String filename) {
        // Arrange: Edge cases para la resolución de extensiones
        // (sin punto, nulos o en mayúsculas) deben terminar en .jpg
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "image/jpeg", "bytes".getBytes());

        // Act
        String relativePath = storageService.store(file, "assets");

        // Assert
        assertThat(relativePath).endsWith(".jpg");
    }

    // =========================================================================
    // delete()
    // =========================================================================

    @Test
    void should_deleteFile_when_fileExists() throws IOException {
        // Arrange — creamos el archivo manualmente en el tempDir
        Path subDir = tempDir.resolve("assets");
        Files.createDirectories(subDir);
        Path file = subDir.resolve("archivo.jpg");
        Files.writeString(file, "contenido");
        String relativePath = "assets/archivo.jpg";

        // Act
        storageService.delete(relativePath);

        // Assert
        assertThat(file).doesNotExist();
    }

    @Test
    void should_notThrow_when_deletingNonExistentFile() {
        // delete() usa deleteIfExists — debe ser silencioso si el archivo no existe
        assertThatNoException().isThrownBy(() -> storageService.delete("assets/no_existe.jpg"));
    }

    @Test
    void should_notThrow_when_relativePathIsNull() {
        assertThatNoException().isThrownBy(() -> storageService.delete(null));
    }

    @Test
    void should_notThrow_when_relativePathIsBlank() {
        assertThatNoException().isThrownBy(() -> storageService.delete("   "));
    }

    @Test
    void should_ignorePathTraversal_when_deletingWithDotDotSegments() {
        // Seguridad: rutas con ../ no deben salir del uploadRoot.
        assertThatNoException().isThrownBy(() -> storageService.delete("../../etc/passwd"));
    }

    // =========================================================================
    // buildPublicUrl()
    // =========================================================================

    @Test
    void should_buildCorrectPublicUrl_when_validRelativePathProvided() {
        // Act
        String url = storageService.buildPublicUrl("assets/foto.jpg");

        // Assert
        assertThat(url).isEqualTo("http://localhost:8080/uploads/assets/foto.jpg");
    }

    @Test
    void should_buildUrlWithSubdirectory_when_pathHasNestedDirs() {
        String url = storageService.buildPublicUrl("documentos/2026/contrato.pdf");

        assertThat(url).isEqualTo("http://localhost:8080/uploads/documentos/2026/contrato.pdf");
    }
}