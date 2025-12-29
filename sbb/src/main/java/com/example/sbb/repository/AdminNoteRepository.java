package com.example.sbb.repository;

import com.example.sbb.domain.AdminNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminNoteRepository extends JpaRepository<AdminNote, Long> {

    @Query("select n from AdminNote n left join fetch n.comments c left join fetch c.author where n.id = :id")
    java.util.Optional<AdminNote> findWithComments(Long id);

    List<AdminNote> findAllByOrderByCreatedAtDesc();
}
