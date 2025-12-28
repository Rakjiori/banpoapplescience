package com.example.sbb.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 간단하게 여기서 바로 생성해서 사용 (빈으로 주입해도 됨)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Map<String, Integer> AVATAR_PRICES = Map.of(
            "🧑", 0,
            "🐱", 10,
            "🐳", 10,
            "🦊", 10,
            "🐯", 12,
            "🐼", 12,
            "👾", 14,
            "🤖", 14
    );

    private static final Map<String, Integer> BANNER_PRICES = Map.of(
            "sunrise", 10,
            "ocean", 15,
            "forest", 15,
            "midnight", 18,
            "aurora", 20
    );

    private static final Map<String, Integer> BOOST_PRICES = Map.of(
            "shield", 20,
            "extra10", 15,
            "extra20", 25
    );

    /**
     * 회원 가입용 유저 생성 (계정 유형별 기본 정보 포함)
     */
    @org.springframework.transaction.annotation.Transactional
    public SiteUser createUser(String username,
                               String password,
                               AccountType accountType,
                               String fullName,
                               String schoolName,
                               String grade) {
        String trimmedUsername = trimToNull(username);
        if (!StringUtils.hasText(trimmedUsername) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("아이디와 비밀번호를 모두 입력해주세요.");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("계정 유형을 선택해주세요.");
        }
        userRepository.findByUsername(trimmedUsername)
                .ifPresent(u -> { throw new IllegalArgumentException("이미 사용 중인 아이디입니다."); });

        SiteUser user = new SiteUser();
        user.setUsername(trimmedUsername);
        user.setPassword(passwordEncoder.encode(password)); // 비밀번호 암호화
        user.setRole("ROLE_USER"); // 기본 권한
        user.setAccountType(accountType);
        user.setFullName(trimToNull(fullName));

        switch (accountType) {
            case STUDENT -> {
                if (!StringUtils.hasText(fullName)) {
                    throw new IllegalArgumentException("학생 이름을 입력해주세요.");
                }
                if (!StringUtils.hasText(schoolName)) {
                    throw new IllegalArgumentException("학교를 입력해주세요.");
                }
                if (!StringUtils.hasText(grade)) {
                    throw new IllegalArgumentException("학년을 입력해주세요.");
                }
                user.setSchoolName(trimToNull(schoolName));
                user.setGrade(trimToNull(grade));
            }
            case ASSISTANT -> {
                if (!StringUtils.hasText(fullName)) {
                    throw new IllegalArgumentException("조교 이름을 입력해주세요.");
                }
            }
            case PARENT -> {
                if (!StringUtils.hasText(fullName)) {
                    throw new IllegalArgumentException("학부모 이름을 입력해주세요.");
                }
            }
            default -> throw new IllegalArgumentException("지원하지 않는 계정 유형입니다.");
        }

        return userRepository.save(user);
    }

    /**
     * username으로 유저 한 명 조회 (로그인 유저 찾을 때 사용)
     */
    public SiteUser getUser(String username) {
        Optional<SiteUser> optionalUser = this.userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            return optionalUser.get();
        } else {
            // 없을 때 예외 발생 (나중에 커스텀 예외로 바꿔도 됨)
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + username);
        }
    }

    /**
     * username 중복 체크용 (회원가입 시 사용 가능)
     */
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    /**
     * 문제 풀이 후 포인트/연속 풀이 갱신
     */
    public void recordSolve(SiteUser user, boolean correct) {
        if (user == null) return;
        var today = java.time.LocalDate.now();
        var last = user.getLastSolvedDate();
        if (last == null) {
            user.setStreak(1);
        } else if (last.isEqual(today)) {
            // same day: streak 유지
        } else if (last.plusDays(1).isEqual(today)) {
            user.setStreak(user.getStreak() + 1);
        } else {
            if (user.getShieldItems() > 0) {
                user.setShieldItems(user.getShieldItems() - 1); // 보호 아이템 사용
            } else {
                user.setStreak(1);
            }
        }
        user.setLastSolvedDate(today);

        int base = correct ? 10 : 5;
        int bonus = Math.max(0, user.getStreak());
        user.setPoints(user.getPoints() + base + bonus);
        try {
            userRepository.save(user);
        } catch (Exception ignore) {
            // 포인트 저장 실패 시 로직을 막지 않음
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean updateAvatar(SiteUser user, String avatar) {
        if (user == null || avatar == null) return false;
        if (!AVATAR_PRICES.containsKey(avatar)) return false;
        int price = AVATAR_PRICES.getOrDefault(avatar, 0);
        Set<String> owned = parseOwned(user.getPurchasedAvatars());
        if (user.getAvatar() != null) owned.add(user.getAvatar()); // 현재 장착 중인 아바타는 보유 처리
        boolean alreadyOwned = owned.contains(avatar) || price == 0;
        if (!alreadyOwned && user.getPoints() < price) return false;
        if (!alreadyOwned && price > 0) {
            user.setPoints(user.getPoints() - price);
            owned.add(avatar);
            user.setPurchasedAvatars(String.join(",", owned));
        }
        user.setAvatar(avatar);
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean updateBanner(SiteUser user, String banner) {
        if (user == null || banner == null) return false;
        if (!BANNER_PRICES.containsKey(banner)) return false;
        int price = BANNER_PRICES.getOrDefault(banner, 0);
        Set<String> owned = parseOwned(user.getPurchasedBanners());
        if (user.getBanner() != null) owned.add(user.getBanner()); // 현재 장착 중인 배너는 보유 처리
        boolean alreadyOwned = owned.contains(banner) || price == 0;
        if (!alreadyOwned && user.getPoints() < price) return false;
        if (!alreadyOwned && price > 0) {
            user.setPoints(user.getPoints() - price);
            owned.add(banner);
            user.setPurchasedBanners(String.join(",", owned));
        }
        user.setBanner(banner);
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean equipBadge(SiteUser user, String badgeId, Set<String> unlocked) {
        if (user == null || badgeId == null) return false;
        if (unlocked == null || !unlocked.contains(badgeId)) return false;
        user.setActiveBadge(badgeId);
        Set<String> owned = parseOwned(user.getPurchasedBadges());
        owned.add(badgeId);
        user.setPurchasedBadges(String.join(",", owned));
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean grantBadge(SiteUser user, String badgeId) {
        if (user == null || badgeId == null) return false;
        Set<String> owned = parseOwned(user.getPurchasedBadges());
        if (owned.contains(badgeId)) return false;
        owned.add(badgeId);
        user.setPurchasedBadges(String.join(",", owned));
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean purchaseBoost(SiteUser user, String boostId) {
        if (user == null || boostId == null) return false;
        Integer price = BOOST_PRICES.get(boostId);
        if (price == null) return false;
        if (user.getPoints() < price) return false;
        user.setPoints(user.getPoints() - price);
        switch (boostId) {
            case "shield" -> user.setShieldItems(user.getShieldItems() + 1);
            case "extra10" -> user.setExtraProblemTokens(user.getExtraProblemTokens() + 10);
            case "extra20" -> user.setExtraProblemTokens(user.getExtraProblemTokens() + 20);
            default -> { return false; }
        }
        userRepository.save(user);
        return true;
    }

    public Set<String> parseOwned(String raw) {
        if (raw == null || raw.isBlank()) return new HashSet<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Map<String, Integer> getAvatarPrices() {
        return AVATAR_PRICES;
    }

    public Map<String, Integer> getBannerPrices() {
        return BANNER_PRICES;
    }

    public Map<String, Integer> getBoostPrices() {
        return BOOST_PRICES;
    }

    @org.springframework.transaction.annotation.Transactional
    public SiteUser updateAccount(SiteUser user,
                                  String username,
                                  String fullName,
                                  String schoolName,
                                  String grade,
                                  String newPassword,
                                  String currentPassword) {
        if (user == null) throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다.");
        if (!StringUtils.hasText(currentPassword) || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        String newUsername = trimToNull(username);
        if (!StringUtils.hasText(newUsername)) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (!user.getUsername().equals(newUsername)) {
            userRepository.findByUsername(newUsername)
                    .filter(u -> !u.getId().equals(user.getId()))
                    .ifPresent(u -> { throw new IllegalArgumentException("이미 사용 중인 아이디입니다."); });
            user.setUsername(newUsername);
        }

        user.setFullName(trimToNull(fullName));
        user.setSchoolName(trimToNull(schoolName));
        user.setGrade(trimToNull(grade));

        if (StringUtils.hasText(newPassword)) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        return userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public SiteUser save(SiteUser user) {
        return userRepository.save(user);
    }

    public List<SiteUser> findAll() {
        return userRepository.findAll();
    }

    public Optional<SiteUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean isRoot(SiteUser user) {
        return user != null && "ROLE_ROOT".equals(user.getRole());
    }

    public boolean isAdminOrRoot(SiteUser user) {
        if (user == null) return false;
        String role = user.getRole();
        return "ROLE_ADMIN".equals(role) || "ROLE_ROOT".equals(role);
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean deleteUser(SiteUser actor, Long targetUserId,
                              com.example.sbb.repository.GroupMemberRepository groupMemberRepository,
                              com.example.sbb.repository.FriendRepository friendRepository,
                              com.example.sbb.repository.FriendRequestRepository friendRequestRepository,
                              com.example.sbb.repository.FriendShareRequestRepository friendShareRequestRepository,
                              com.example.sbb.repository.GroupInviteRepository groupInviteRepository,
                              com.example.sbb.repository.DocumentFileRepository documentFileRepository,
                              com.example.sbb.repository.QuizQuestionRepository quizQuestionRepository,
                              com.example.sbb.repository.ProblemRepository problemRepository) {
        if (actor == null || targetUserId == null) return false;
        if (!isAdminOrRoot(actor)) return false;
        SiteUser target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return false;
        if ("ROLE_ROOT".equals(target.getRole())) return false;
        // 그룹 멤버 제거
        groupMemberRepository.deleteAll(
                groupMemberRepository.findAll().stream()
                        .filter(m -> m.getUser() != null && targetUserId.equals(m.getUser().getId()))
                        .toList());
        // 친구/요청/공유 제거
        friendRepository.deleteAll(
                friendRepository.findAll().stream()
                        .filter(f -> (f.getFrom() != null && targetUserId.equals(f.getFrom().getId()))
                                || (f.getTo() != null && targetUserId.equals(f.getTo().getId())))
                        .toList());
        friendRequestRepository.deleteAll(
                friendRequestRepository.findAll().stream()
                        .filter(f -> (f.getFromUser() != null && targetUserId.equals(f.getFromUser().getId()))
                                || (f.getToUser() != null && targetUserId.equals(f.getToUser().getId())))
                        .toList());
        friendShareRequestRepository.deleteAll(
                friendShareRequestRepository.findAll().stream()
                        .filter(f -> (f.getFromUser() != null && targetUserId.equals(f.getFromUser().getId()))
                                || (f.getToUser() != null && targetUserId.equals(f.getToUser().getId())))
                        .toList());
        // 그룹 초대 제거
        groupInviteRepository.deleteAll(
                groupInviteRepository.findAll().stream()
                        .filter(inv -> (inv.getFromUser() != null && targetUserId.equals(inv.getFromUser().getId()))
                                || (inv.getToUser() != null && targetUserId.equals(inv.getToUser().getId())))
                        .toList());
        // 업로드 파일/문제 제거
        documentFileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && targetUserId.equals(doc.getUser().getId()))
                .forEach(doc -> {
                    quizQuestionRepository.deleteAllByDocument(doc);
                    problemRepository.deleteAllByDocumentFile(doc);
                    documentFileRepository.delete(doc);
                });
        try {
            userRepository.delete(target);
        } catch (Exception e) {
            // FK 제약 등으로 삭제가 막히면 계정을 비활성/익명화
            target.setRole("ROLE_DELETED");
            if (target.getUsername() != null && !target.getUsername().contains("_deleted_")) {
                target.setUsername(target.getUsername() + "_deleted_" + target.getId());
            }
            target.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            userRepository.save(target);
        }
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean promoteToAdmin(SiteUser actor, Long targetUserId) {
        if (!isRoot(actor) || targetUserId == null) return false;
        SiteUser target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new java.util.NoSuchElementException("유저를 찾을 수 없습니다."));
        if (isRoot(target)) return true;
        target.setRole("ROLE_ADMIN");
        userRepository.save(target);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean revokeAdmin(SiteUser actor, Long targetUserId) {
        if (!isRoot(actor) || targetUserId == null) return false;
        SiteUser target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new java.util.NoSuchElementException("유저를 찾을 수 없습니다."));
        if (isRoot(target)) return false;
        target.setRole("ROLE_USER");
        userRepository.save(target);
        return true;
    }
}
