package com.bemfeito.api.storage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class LocalImageStorage implements ImageStorage {
    private final Path root;
    private final long maxBytes;
    public LocalImageStorage(@Value("${app.upload.dir:${user.home}/.bem-feito/uploads}") String directory,@Value("${app.upload.max-bytes:5242880}") long maxBytes) {
        root=Path.of(directory).toAbsolutePath().normalize(); this.maxBytes=maxBytes;
        if(maxBytes<1 || maxBytes>Integer.MAX_VALUE-1) throw new IllegalArgumentException("Limite de upload inválido");
    }
    private Path path(Long owner,String key) throws IOException {
        if(owner==null || owner<=0 || !key.matches("[a-f0-9-]{36}\\.png")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Path directory=root.resolve(owner.toString());
        Files.createDirectories(directory);
        if(Files.isSymbolicLink(root) || Files.isSymbolicLink(directory) || !directory.toRealPath().startsWith(root.toRealPath())) throw new IOException("Diretório de upload inválido");
        Path target=directory.resolve(key).normalize();
        if(!target.startsWith(directory) || Files.isSymbolicLink(target)) throw new IOException("Caminho de upload inválido");
        return target;
    }
    @Override public String store(Long owner,MultipartFile file) {
        if(file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione uma imagem");
        if(file.getSize()>maxBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Imagem excede o tamanho permitido");
        Path target=null;
        try(var input=file.getInputStream()) {
            byte[] bytes=input.readNBytes((int)maxBytes+1);
            if(bytes.length>maxBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Imagem excede o tamanho permitido");
            try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(stream);
                if(!readers.hasNext()) throw invalid();
                var reader=readers.next();
                try {
                    if(!Set.of("JPEG","JPG","PNG","WEBP").contains(reader.getFormatName().toUpperCase(Locale.ROOT))) throw invalid();
                    reader.setInput(stream,true,true);
                    int width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<=0 || height<=0 || (long)width*height>16000000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Imagem deve ter até 16 milhões de pixels");
                    var decoded=reader.read(0);
                    if(decoded==null) throw invalid();
                    String key=UUID.randomUUID()+".png"; target=path(owner,key);
                    // Reencode pixels: do not retain executable trailers, metadata or original filenames.
                    try(var output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
                        if(!ImageIO.write(decoded,"png",output)) throw new IOException("Codificador indisponível");
                    }
                    return "/api/history-media/"+owner+"/"+key;
                } finally { reader.dispose(); }
            }
        } catch(ResponseStatusException e) { throw e; }
        catch(IOException | RuntimeException e) {
            if(target!=null) try { Files.deleteIfExists(target); } catch(IOException ignored) { }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Não foi possível validar ou armazenar a imagem");
        }
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Envie uma imagem JPEG, PNG ou WebP válida"); }
    @Override public Resource read(Long owner,String key) {
        try { Path target=path(owner,key); if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS)) throw new ResponseStatusException(HttpStatus.NOT_FOUND); return new FileSystemResource(target); }
        catch(IOException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
    }
    @Override public void delete(Long owner,String url) {
        String prefix="/api/history-media/"+owner+"/";
        if(url==null || !url.startsWith(prefix)) return;
        try { Files.deleteIfExists(path(owner,url.substring(prefix.length()))); }
        catch(IOException | RuntimeException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Falha ao remover arquivo de imagem da instituição {}",owner); }
    }
}
