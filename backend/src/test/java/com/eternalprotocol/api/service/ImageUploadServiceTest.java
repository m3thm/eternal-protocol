package com.eternalprotocol.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.eternalprotocol.api.exception.ImageUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    @InjectMocks
    private ImageUploadService imageUploadService;

    @Test
    void uploadProductImage_validPngUnder5MB_returnsSecureUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tee.png", "image/png", "fake-image-bytes".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(
                Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/tee.png"));

        String result = imageUploadService.uploadProductImage(file);

        assertThat(result).isEqualTo("https://res.cloudinary.com/demo/image/upload/tee.png");
    }

    @Test
    void uploadProductImage_nullFile_throwsIllegalArgument() {
        assertThatThrownBy(() -> imageUploadService.uploadProductImage(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No image file");
    }

    @Test
    void uploadProductImage_emptyFile_throwsIllegalArgument() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> imageUploadService.uploadProductImage(emptyFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadProductImage_overFiveMB_throwsIllegalArgumentWithoutCallingCloudinary() {
        byte[] tooLarge = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", tooLarge);

        assertThatThrownBy(() -> imageUploadService.uploadProductImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void uploadProductImage_disallowedContentType_throwsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "not-an-image".getBytes());

        assertThatThrownBy(() -> imageUploadService.uploadProductImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG");
    }

    @Test
    void uploadProductImage_cloudinaryThrowsIOException_wrapsInImageUploadException() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tee.png", "image/png", "fake-image-bytes".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("Cloudinary unreachable"));

        assertThatThrownBy(() -> imageUploadService.uploadProductImage(file))
                .isInstanceOf(ImageUploadException.class)
                .hasMessageContaining("Failed to upload");
    }

    @Test
    void uploadProductImage_acceptsAllFourAllowedContentTypes() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://example.com/img"));

        for (String contentType : new String[]{"image/jpeg", "image/png", "image/webp", "image/gif"}) {
            MockMultipartFile file = new MockMultipartFile("file", "img", contentType, "bytes".getBytes());
            assertThat(imageUploadService.uploadProductImage(file)).isEqualTo("https://example.com/img");
        }
    }
}
