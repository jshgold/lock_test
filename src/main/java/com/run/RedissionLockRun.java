package com.run;

import com.service.PessimisticLockTest;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class RedissionLockRun {
    private static final PessimisticLockTest pl = PessimisticLockTest.getInstance();
    public static void run() throws InterruptedException{

        // 1. Redisson 클라이언트 설정 및 생성 (로컬 Redis 기본 포트 6379)
        Config config = new Config();
            config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient redisson = Redisson.create(config);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lockTestUnit");

        // 초기 재고 100개 세팅 (기존 Item 엔티티 재사용, @Version은 없어도 됨)
        pl.initDataOnce(emf);

        int threadCount = 10_000;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

            System.out.println("=== Redisson 분산 락 테스트 시작 ===");

            for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = pl.decreaseStockWithDistributedLock(redisson, emf, 1L);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        redisson.shutdown(); // Redisson 클라이언트 종료

        System.out.println("최종 성공 횟수: " + successCount.get());
        System.out.println("최종 실패 횟수: " + failCount.get());

        pl.verifyResult(emf);
        emf.close();
    }
}
