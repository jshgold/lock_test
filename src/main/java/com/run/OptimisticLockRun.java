package com.run;

import com.service.PessimisticLockTest;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimisticLockRun {
    private static final PessimisticLockTest pl = PessimisticLockTest.getInstance();

    public static void run() throws InterruptedException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lockTestUnit");

        // 초기 재고: 100개
        pl.initDataOnce(emf);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        System.out.println("=== PostgreSQL 낙관적 락(Optimistic Lock) 테스트 시작 ===");

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 최대 10회 재시도
                    boolean success = pl.executeWithRetry(emf, 1L, 10);
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

        System.out.println("최종 성공 횟수: " + successCount.get());
        System.out.println("최종 실패 횟수(재시도 초과): " + failCount.get());

        pl.verifyResult(emf);
        emf.close();
    }
}
