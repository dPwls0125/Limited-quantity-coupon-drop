package com.example.coupon.init;

import com.example.coupon.event.model.Coupon;
import com.example.coupon.event.repository.CouponRepository;
import com.example.coupon.user.User;
import com.example.coupon.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            User user = new User();
            user.setName("user" + i);
            user.setEmail("user" + i + "@example.com");
            users.add(user);
        }
        userRepository.saveAll(users);

        List<Coupon> coupons = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Coupon coupon = new Coupon();
            coupon.setName("Coupon" + i);
            coupon.setTotalQuantity(100);
            coupon.setIssuedQuantity(0);
            coupons.add(coupon);
        }
        couponRepository.saveAll(coupons);
    }
}
