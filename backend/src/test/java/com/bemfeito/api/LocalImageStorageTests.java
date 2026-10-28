package com.bemfeito.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import com.bemfeito.api.storage.LocalImageStorage;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class LocalImageStorageTests {
    @TempDir Path directory;
    private byte[] image(String format) throws Exception { var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(4,3,BufferedImage.TYPE_INT_RGB),format,out);return out.toByteArray(); }
    @Test void pngIsDecodedAndStoredWithGeneratedName() throws Exception {
        var storage=new LocalImageStorage(directory.toString(),1024);
        String url=storage.store(1L,new MockMultipartFile("file","../../malicious.exe","application/octet-stream",image("png")));
        assertThat(url).matches("/api/history-media/1/[a-f0-9-]{36}\\.png");
        assertThat(ImageIO.read(storage.read(1L,url.substring(url.lastIndexOf('/')+1)).getInputStream()).getWidth()).isEqualTo(4);
    }
    @Test void jpegIsReencodedAsPng() throws Exception {
        var storage=new LocalImageStorage(directory.toString(),2048);
        String url=storage.store(1L,new MockMultipartFile("file","photo.jpg","image/jpeg",image("jpg")));
        assertThat(Files.readAllBytes(directory.resolve(url.substring("/api/history-media/".length())))).startsWith((byte)137,(byte)80,(byte)78,(byte)71);
    }
    @Test void rejectsExecutableEvenWithPngNameAndMime() {
        var storage=new LocalImageStorage(directory.toString(),1024);
        assertThatThrownBy(()->storage.store(1L,new MockMultipartFile("file","photo.png","image/png","MZ executable".getBytes()))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsGifDespiteBeingDecodable() throws Exception {
        var storage=new LocalImageStorage(directory.toString(),1024);
        assertThatThrownBy(()->storage.store(1L,new MockMultipartFile("file","photo.png","image/png",image("gif")))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsOversizedAndEmptyFiles() {
        var storage=new LocalImageStorage(directory.toString(),8);
        assertThatThrownBy(()->storage.store(1L,new MockMultipartFile("file",new byte[9]))).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(413));
        assertThatThrownBy(()->storage.store(1L,new MockMultipartFile("file",new byte[0]))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void deletionIsOwnerScopedAndIgnoresExternalResources() throws Exception {
        var storage=new LocalImageStorage(directory.toString(),1024);
        String url=storage.store(1L,new MockMultipartFile("file",image("png")));
        Path file=directory.resolve(url.substring("/api/history-media/".length()));
        storage.delete(2L,url);storage.delete(1L,"https://example.invalid/image.png");assertThat(file).exists();
        storage.delete(1L,url);assertThat(file).doesNotExist();storage.delete(1L,url);
    }
    @Test void traversalCannotReadFiles() {
        var storage=new LocalImageStorage(directory.toString(),1024);
        assertThatThrownBy(()->storage.read(1L,"../../secret.png")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void truncatedImageIsRejected() throws Exception {
        var storage=new LocalImageStorage(directory.toString(),1024);
        assertThatThrownBy(()->storage.store(1L,new MockMultipartFile("file",java.util.Arrays.copyOf(image("png"),24)))).isInstanceOf(ResponseStatusException.class);
    }
}
