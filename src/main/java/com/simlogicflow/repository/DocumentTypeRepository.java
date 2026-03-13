package com.simlogicflow.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.simlogicflow.model.DocumentType;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
    Optional<DocumentType> findByName(String name);
}
