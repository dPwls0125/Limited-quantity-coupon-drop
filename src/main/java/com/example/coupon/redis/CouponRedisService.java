package com.example.coupon.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CouponRedisService {

    private final StringRedisTemplate redisTemplate;
    private static final long MAX_COUPON_COUNT = 100;

    public CouponRedisService(final StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 선착순 쿠폰 발급을 시도하고 결과를 반환합니다.
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 성공 시 true, 중복 또는 마감 시 false
     */
    public boolean registerApplicant(Long couponId, Long userId) {
        String zsetKey = "coupon:" + couponId + ":applicants"; // Sorted Set (선착순 대기열)
        String issuedSetKey = "coupon:" + couponId + ":issued_users"; // Set (중복 검사용)
        String userIdStr = String.valueOf(userId);
        long currentTimeMillis = System.currentTimeMillis();

        // 1. 중복 검사 (Redis Set)
        // Set에 이미 사용자가 있는지 확인합니다.
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(issuedSetKey, userIdStr))) {
            return false; // 이미 발급받음 (중복)
        }

        // 2. 선착순 등록 시도 (Redis Sorted Set)
        // ZSET의 현재 크기를 확인하고, 100명 미만이면 등록합니다.
        Long applicantCount = redisTemplate.opsForZSet().size(zsetKey);
        log.info(Thread.currentThread().getName() + ": 선착순 등록 완료");

        if (applicantCount != null && applicantCount < MAX_COUPON_COUNT) {
            // Redis ZADD 명령어: Score (타임스탬프)를 기준으로 사용자(Member) 추가
            // ZADD는 이미 존재하는 멤버를 업데이트하므로, 추가 시도 여부를 파악하기 위해
            // 별도의 중복 검사 로직(1번)이 필수입니다.
            Boolean isAdded = redisTemplate.opsForZSet().add(zsetKey, userIdStr, currentTimeMillis);

            // ZADD가 성공하고 (null이 아님), 새로운 멤버로 추가되었다면
            if (Boolean.TRUE.equals(isAdded)) {
                return true; // 선착순 등록 성공
            }
        }
        return false; // 선착순 마감 또는 등록 실패
    }

    /**
     * 발급 성공 후, 중복 방지를 위해 Redis Set에 사용자 ID를 최종 기록합니다.
     */
    public void recordIssuedUser(Long couponId, Long userId) {
        String issuedSetKey = "coupon:" + couponId + ":issued_users";
        redisTemplate.opsForSet().add(issuedSetKey, String.valueOf(userId));
    }
}
