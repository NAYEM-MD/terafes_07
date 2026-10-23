package com.tronai.cv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CvDocumentRepository extends JpaRepository<CvDocumentEntity, Long> {

    List<CvDocumentEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CvDocumentEntity> findByIdAndUserId(Long id, Long userId);
}
