package com.example.coupon.event.controller;

import com.example.coupon.event.model.IssuedCoupon;
import com.example.coupon.event.service.CouponService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
}
