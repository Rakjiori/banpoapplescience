package com.example.sbb.service;

import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.GroupNotice;
import com.example.sbb.domain.group.GroupTask;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.dto.GroupDto;
import com.example.sbb.dto.GroupNoticeDto;
import com.example.sbb.dto.GroupTaskDto;
import com.example.sbb.repository.GroupMemberRepository;
import com.example.sbb.repository.GroupNoticeRepository;
import com.example.sbb.repository.GroupTaskRepository;
import com.example.sbb.repository.StudyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupApiService {

    private final GroupMemberRepository groupMemberRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupNoticeRepository groupNoticeRepository;
    private final GroupTaskRepository groupTaskRepository;

    @Transactional(readOnly = true)
    public List<GroupDto> myGroups(SiteUser user) {
        if (user == null) return List.of();
        return groupMemberRepository.findByUser(user).stream()
                .map(GroupMember::getGroup)
                .filter(g -> g != null)
                .distinct()
                .map(this::toGroupDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupNoticeDto> notices(Long groupId) {
        StudyGroup group = studyGroupRepository.findById(groupId).orElse(null);
        if (group == null) return List.of();
        return groupNoticeRepository.findByGroupOrderByCreatedAtDesc(group).stream()
                .map(this::toNoticeDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupTaskDto> tasks(Long groupId) {
        StudyGroup group = studyGroupRepository.findById(groupId).orElse(null);
        if (group == null) return List.of();
        return groupTaskRepository.findByGroupOrderByDueDateAscCreatedAtDesc(group).stream()
                .map(this::toTaskDto)
                .toList();
    }

    private GroupDto toGroupDto(StudyGroup g) {
        int memberCount = groupMemberRepository.findByGroup(g).size();
        return new GroupDto(g.getId(), g.getName(), g.getJoinCode(), memberCount);
    }

    private GroupNoticeDto toNoticeDto(GroupNotice n) {
        String created = n.getCreatedAt() != null ? n.getCreatedAt().toLocalDate().toString() : null;
        String author = n.getAuthor() != null ? n.getAuthor().getUsername() : null;
        return new GroupNoticeDto(n.getId(), n.getTitle(), n.getContent(), created, author);
    }

    private GroupTaskDto toTaskDto(GroupTask t) {
        String due = t.getDueDate() != null ? t.getDueDate().toString() : null;
        String created = t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate().toString() : null;
        String author = t.getAuthor() != null ? t.getAuthor().getUsername() : null;
        return new GroupTaskDto(t.getId(), t.getTitle(), t.getDescription(), due, created, author);
    }
}
