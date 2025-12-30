package com.example.sbb.service;

import com.example.sbb.domain.AcademySchedule;
import com.example.sbb.domain.CourseReview;
import com.example.sbb.domain.ScheduleSlot;
import com.example.sbb.domain.Announcement;
import com.example.sbb.repository.AcademyScheduleRepository;
import com.example.sbb.repository.CourseReviewRepository;
import com.example.sbb.repository.ScheduleSlotRepository;
import com.example.sbb.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AcademyContentService {

    private final AcademyScheduleRepository scheduleRepository;
    private final CourseReviewRepository reviewRepository;
    private final ScheduleSlotRepository slotRepository;
    private final AnnouncementRepository announcementRepository;

    public List<AcademySchedule> listSchedules() {
        return scheduleRepository.findAllByOrderBySortOrderAscCreatedAtDesc();
    }

    public List<ScheduleSlot> listSlots() {
        return slotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc();
    }

    public List<CourseReview> listReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Announcement> listAnnouncements() {
        return announcementRepository.findAllByOrderByPublishedAtDescCreatedAtDesc();
    }

    @Transactional
    public AcademySchedule createSchedule(String subject,
                                          String courseType,
                                          String school,
                                          Integer sortOrder) {
        if (!StringUtils.hasText(subject)) subject = "기타";
        if (!StringUtils.hasText(courseType)) courseType = "기타";
        if (!StringUtils.hasText(school)) school = "기타";
        AcademySchedule s = new AcademySchedule();
        s.setTitle(subject + " " + courseType);
        s.setSummary(school);
        s.setGrade("전체");
        s.setSubject(subject.trim());
        s.setCourseType(courseType.trim());
        s.setSchool(school.trim());
        s.setDescription(null);
        s.setWeeklyPlan(null);
        s.setSortOrder(sortOrder != null ? sortOrder : 0);
        return scheduleRepository.save(s);
    }

    @Transactional
    public boolean deleteSchedule(Long id) {
        if (id == null) return false;
        if (!scheduleRepository.existsById(id)) return false;
        slotRepository.deleteAll(slotRepository.findBySchedule_Id(id));
        scheduleRepository.deleteById(id);
        return true;
    }

    @Transactional
    public CourseReview createReview(String author, String highlight, int rating, String content) {
        if (!StringUtils.hasText(author) || !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("작성자와 본문은 필수입니다.");
        }
        int safeRating = Math.min(5, Math.max(1, rating));
        CourseReview review = new CourseReview();
        review.setAuthor(author.trim());
        review.setHighlight(StringUtils.hasText(highlight) ? highlight.trim() : null);
        review.setRating(safeRating);
        review.setContent(content.trim());
        return reviewRepository.save(review);
    }

    @Transactional
    public boolean deleteReview(Long id) {
        if (id == null) return false;
        if (!reviewRepository.existsById(id)) return false;
        reviewRepository.deleteById(id);
        return true;
    }

    @Transactional
    public ScheduleSlot addSlot(Long scheduleId, DayOfWeek dayOfWeek, LocalTime start, LocalTime end, String note) {
        if (scheduleId == null || dayOfWeek == null || start == null || end == null) {
            throw new IllegalArgumentException("요일과 시간은 필수입니다.");
        }
        if (!end.isAfter(start)) throw new IllegalArgumentException("종료 시간이 시작 시간보다 늦어야 합니다.");
        var schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("시간표를 찾을 수 없습니다."));
        ScheduleSlot slot = new ScheduleSlot();
        slot.setSchedule(schedule);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setNote(StringUtils.hasText(note) ? note.trim() : null);
        return slotRepository.save(slot);
    }

    @Transactional
    public boolean deleteSlot(Long slotId) {
        if (slotId == null) return false;
        if (!slotRepository.existsById(slotId)) return false;
        slotRepository.deleteById(slotId);
        return true;
    }

    @Transactional
    public Announcement createAnnouncement(String title, String content, LocalDate publishedAt) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("제목과 본문은 필수입니다.");
        }
        Announcement a = new Announcement();
        a.setTitle(title.trim());
        a.setContent(content.trim());
        a.setPublishedAt(publishedAt);
        return announcementRepository.save(a);
    }

    @Transactional
    public boolean deleteAnnouncement(Long id) {
        if (id == null) return false;
        if (!announcementRepository.existsById(id)) return false;
        announcementRepository.deleteById(id);
        return true;
    }

    public Map<String, String> subjectColors() {
        Map<String, String> map = new HashMap<>();
        map.put("물리", "#3b82f6");
        map.put("화학", "#f97316");
        map.put("생명", "#22c55e");
        map.put("지구", "#8b5cf6");
        map.put("융합", "#ec4899");
        map.put("기타", "#475569");
        return map;
    }
}
