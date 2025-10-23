package com.example.coupon.event.repository;

import com.example.coupon.event.model.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon,Long> {

    boolean existsByUserIdAndCouponId(long userId, long couponId);
}
