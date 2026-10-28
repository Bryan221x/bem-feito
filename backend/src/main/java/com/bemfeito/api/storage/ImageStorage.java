package com.bemfeito.api.storage;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
public interface ImageStorage {
    String store(Long institutionId,MultipartFile file);
    Resource read(Long institutionId,String key);
    void delete(Long institutionId,String url);
}
