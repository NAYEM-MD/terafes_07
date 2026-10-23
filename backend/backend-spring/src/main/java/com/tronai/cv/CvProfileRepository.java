package com.tronai.cv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CvProfileRepository extends JpaRepository<CvProfileEntity, Long> {

    Optional<CvProfileEntity> findByDocumentId(Long documentId);
}
