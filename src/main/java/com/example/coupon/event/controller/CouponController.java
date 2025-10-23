package com.example.coupon.event.controller;

import com.example.coupon.event.model.IssuedCoupon;
import com.example.coupon.event.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@Slf4j
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 비관적 락을 사용하여 쿠폰을 발급하는 API
     * POST /api/coupons/issue-with-lock/{couponId}/{userId}
     */
    @PostMapping("/api/coupons/issue-with-lock/{couponId}/{userId}")
    public ResponseEntity<IssuedCoupon> issueCouponWithLock(
            @PathVariable Long couponId,
            @PathVariable Long userId) {
        try {
            IssuedCoupon issuedCoupon = couponService.issueCouponWithLock(couponId, userId);
            return new ResponseEntity<>(issuedCoupon, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 락을 사용하지 않고 쿠폰을 발급하는 API (동시성 문제 발생)
     * POST /api/coupons/issue-without-lock/{couponId}/{userId}
     */
    @PostMapping("/api/coupons/issue-without-lock/{couponId}/{userId}")
    public ResponseEntity<IssuedCoupon> issueCouponWithoutLock(
            @PathVariable Long couponId,
            @PathVariable Long userId) {
        try {
            IssuedCoupon issuedCoupon = couponService.issueCouponWithoutLock(couponId, userId);
            return new ResponseEntity<>(issuedCoupon, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    /**
     * Redis Sorted Set을 사용하여 선착순 쿠폰을 발급하는 API
     * POST /api/coupons/issue-redis/{couponId}/{userId}
     * * Redis를 통해 선착순 및 중복 검사를 수행하여 높은 처리량을 보장합니다.
     */
    @PostMapping("/api/coupons/issue-redis/{couponId}/{userId}")
    public ResponseEntity<IssuedCoupon> issueCouponWithRedis(
            @PathVariable Long couponId,
            @PathVariable Long userId) {
        try {
            log.info("Request received (Redis): CouponId={}, UserId={}", couponId, userId);

            // service.issueCoupon() 메서드는 Redis의 registerApplicant 호출을 포함합니다.
            IssuedCoupon issuedCoupon = couponService.issueCoupon(couponId, userId);

            // 발급 성공 (201 Created)
            return new ResponseEntity<>(issuedCoupon, HttpStatus.CREATED);

        } catch (IllegalStateException e) {
            // Redis 검증에서 중복이거나 선착순 마감으로 인해 실패한 경우
            log.warn("Issue failed (Redis) for UserId={}: {}", userId, e.getMessage());
            // 400 Bad Request 또는 409 Conflict 사용 가능
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        } catch (IllegalArgumentException e) {
            // 쿠폰/유저 ID가 DB에 없는 등 데이터 문제인 경우
            log.error("Issue error (Redis) for UserId={}: {}", userId, e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            // 그 외 DB 트랜잭션 오류 등 예상치 못한 오류
            log.error("Internal Server Error during Redis issue for UserId={}", userId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
