package com.example.coupon.event.service;

import com.example.coupon.event.model.Coupon;
import com.example.coupon.event.model.IssuedCoupon;
import com.example.coupon.event.repository.CouponRepository;
import com.example.coupon.event.repository.IssuedCouponRepository;
import com.example.coupon.redis.CouponRedisService;
import com.example.coupon.user.User;
import com.example.coupon.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponRedisService redisService;

    public CouponService(final CouponRepository couponRepository, final UserRepository userRepository, final IssuedCouponRepository issuedCouponRepository, final CouponRedisService redisService) {
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.redisService = redisService;
    }

    // Lock 미적용
    @Transactional
    public IssuedCoupon issueCouponWithoutLock(Long couponId, Long userId){
        // 쿠폰 찾기
        Coupon coupon = couponRepository.findById(couponId) // 일반 findById 사용
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 쿠폰 재고 확인
        if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }

        // 유저 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 유저가 이미 쿠폰을 발급받았는지 확인
        if (issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new IllegalStateException("이미 쿠폰을 발급받았습니다.");
        }

        // 쿠폰 발급
        coupon.issue();

        IssuedCoupon issuedCoupon = new IssuedCoupon(user, coupon);
        return issuedCouponRepository.save(issuedCoupon);
    }


    // Lock 적용
    @Transactional
    public IssuedCoupon issueCouponWithLock(Long couponId, Long userId) {
        // 비관적 락을 걸어 동시에 여러 스레드가 접근하지 못하도록 합니다.
        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 쿠폰 재고 확인
        if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }

        // 유저 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 유저가 이미 쿠폰을 발급받았는지 확인
        if (issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new IllegalStateException("이미 쿠폰을 발급받았습니다.");
        }

        // 쿠폰 수량 증가
        coupon.issue();

        // 발급된 쿠폰 정보 저장
        IssuedCoupon issuedCoupon = new IssuedCoupon(user, coupon);
        return issuedCouponRepository.save(issuedCoupon);
    }

    // RDBMS 트랜잭션 관리
    @Transactional
    public IssuedCoupon issueCoupon(Long couponId, Long userId) {

        // 1. Redis에서 선착순/중복 검사 및 대기열 등록 (핵심 로직)
        // - 이 메서드 내부에서 Redis ZCARD (수량 확인) 및 ZADD (선착순 등록) 로직이 처리됩니다.
        // - 중복 발급자 및 정원 초과자는 여기서 빠르게 차단됩니다.
        boolean isApplicantRegistered = redisService.registerApplicant(couponId, userId);

        if (!isApplicantRegistered) {
            // Redis에서 중복(이미 발급받았거나) 또는 마감(선착순 초과)으로 판정된 경우
            throw new IllegalStateException("쿠폰 발급 조건(선착순, 중복 여부)을 만족하지 못했습니다.");
        }

        // --- 2. Redis 검증 통과 후, RDBMS 최종 발급 기록 ---

        // 락이 없는 일반 조회 (Read Only)
        // 비관적 락 제거: Redis가 순서를 제어하므로 DB 락은 불필요하며 성능 저하만 야기합니다.
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // ⚠️ (Optional) 마지막 안전 장치: 재고 확인 및 중복 검사는 Redis에서 이미 처리했지만,
        // DB 정합성이 꼭 필요하다면 여기서 한번 더 검사합니다.
        // 단, Redis 설계가 정확하다면 이 검사는 거의 항상 통과해야 합니다.

        // 1. DB 재고 증가 (최종 발급 수량 기록)
        // Redis ZADD 성공 후, 해당 요청이 실제 DB에 기록되어야 할 N번째 요청임을 의미합니다.
        coupon.issue(); // issuedQuantity 증가

        // 2. 발급된 쿠폰 정보 저장 (RDBMS 영속성)
        IssuedCoupon issuedCoupon = new IssuedCoupon(user, coupon);
        IssuedCoupon savedCoupon = issuedCouponRepository.save(issuedCoupon);

        // 3. Redis 최종 발급 기록 (다음 요청의 중복 검사용)
        // DB 트랜잭션이 성공적으로 커밋될 경우에만 Redis Set에 기록되어 영구적인 중복 발급을 방지합니다.
        redisService.recordIssuedUser(couponId, userId);

        return savedCoupon;
    }
}
