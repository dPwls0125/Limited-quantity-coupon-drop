package com.example.coupon.event.service;

import com.example.coupon.event.model.Coupon;
import com.example.coupon.event.model.IssuedCoupon;
import com.example.coupon.event.repository.CouponRepository;
import com.example.coupon.event.repository.IssuedCouponRepository;
import com.example.coupon.user.User;
import com.example.coupon.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    private final IssuedCouponRepository issuedCouponRepository;

    public CouponService(CouponRepository couponRepository, UserRepository userRepository, IssuedCouponRepository issuedCouponRepository) {
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.issuedCouponRepository = issuedCouponRepository;
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
}
