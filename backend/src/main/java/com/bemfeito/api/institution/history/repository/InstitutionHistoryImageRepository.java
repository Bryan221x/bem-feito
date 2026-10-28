package com.bemfeito.api.institution.history.repository;

import com.bemfeito.api.institution.history.entity.InstitutionHistoryImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionHistoryImageRepository extends JpaRepository<InstitutionHistoryImage,Long> {
    List<InstitutionHistoryImage> findByInstitutionIdOrderByDisplayOrderAscIdAsc(Long institutionId);
    Optional<InstitutionHistoryImage> findByIdAndInstitutionId(Long id,Long institutionId);
}
