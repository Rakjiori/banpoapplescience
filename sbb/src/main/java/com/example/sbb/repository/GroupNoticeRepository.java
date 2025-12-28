package com.example.sbb.repository;

import com.example.sbb.domain.group.GroupNotice;
import com.example.sbb.domain.group.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupNoticeRepository extends JpaRepository<GroupNotice, Long> {
    List<GroupNotice> findByGroupOrderByCreatedAtDesc(StudyGroup group);
}
