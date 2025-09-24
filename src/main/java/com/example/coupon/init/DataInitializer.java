package com.example.coupon.init;

import com.example.coupon.event.model.Coupon;
import com.example.coupon.event.repository.CouponRepository;
import com.example.coupon.user.User;
import com.example.coupon.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    public DataInitializer(UserRepository userRepository, CouponRepository couponRepository) {
        this.userRepository = userRepository;
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1000개의 유저 데이터 삽입
        for (int i = 1; i <= 1000; i++) {
            User user = new User();
            user.setName("user" + i);
            user.setEmail("user" + i + "@example.com");
            userRepository.save(user);
        }

        // 3개의 쿠폰 데이터 삽입
        // totalQuantity를 100, 200, 300으로 다르게 설정하여 테스트에 활용
        for (int i = 1; i <= 3; i++) {
            Coupon coupon = new Coupon();
            coupon.setName("Coupon" + i);
            coupon.setTotalQuantity(i * 100);
            coupon.setIssuedQuantity(0);
            couponRepository.save(coupon);
        }
    }
}
