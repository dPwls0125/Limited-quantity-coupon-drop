package com.example.coupon;

import com.example.coupon.event.model.Coupon;
import com.example.coupon.event.model.IssuedCoupon;
import com.example.coupon.event.repository.CouponRepository;
import com.example.coupon.event.repository.IssuedCouponRepository;
import com.example.coupon.event.service.CouponService;
import com.example.coupon.user.User;
import com.example.coupon.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private UserRepository userRepository;

    private Coupon testCoupon;
    private User testUser;

    @BeforeEach
    void setUp() {
        testCoupon = new Coupon();
        testUser = new User();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() {

        // Given: 필요한 Mock 객체의 행동 설정
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(testCoupon));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(issuedCouponRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(false);
        given(issuedCouponRepository.save(any(IssuedCoupon.class))).willReturn(new IssuedCoupon(testUser, testCoupon));

        // When: 메소드 호출
        IssuedCoupon result = couponService.issueCouponWithLock(1L, 1L);

        // Then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getCoupon()).isEqualTo(testCoupon);
    }

    @Test
    @DisplayName("쿠폰이 모두 소진된 경우 실패")
    void issueCoupon_OutOfStock() {

        // Given: 쿠폰 재고를 모두 소진된 상태로 설정
        // 이 부분은 Coupon 엔티티에 세터가 필요합니다.
        testCoupon.setIssuedQuantity(testCoupon.getTotalQuantity());
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(testCoupon));

        // When/Then: 예외 발생을 검증
        assertThrows(IllegalStateException.class, () -> couponService.issueCouponWithLock(1L, 1L));
    }

    @Test
    @DisplayName("유저가 이미 쿠폰을 발급받은 경우 실패")
    void issueCoupon_AlreadyIssued() {
        // Given: 유저가 이미 쿠폰을 발급받았다고 가정
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(testCoupon));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(issuedCouponRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(true);

        // When/Then: 예외 발생을 검증
        assertThrows(IllegalStateException.class, () -> couponService.issueCouponWithLock(1L, 1L));
    }
}
