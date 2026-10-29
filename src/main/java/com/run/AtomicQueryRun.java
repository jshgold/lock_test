package com.run;

import com.service.PessimisticLockTest;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicQueryRun {
    private static final PessimisticLockTest pl = PessimisticLockTest.getInstance();

    public static void run() throws InterruptedException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lockTestUnit");

        // 초기 재고: 10개 설정
        pl.initDataOnce(emf);

        int threadCount = 1_000_000; // 100명이 동시 차감 시도
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        System.out.println("=== 원자적 쿼리(Atomic Query) 동시성 테스트 시작 ===");

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = pl.decreaseStockAtomic(emf, 1L, 1);
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

        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수(재고 부족): " + failCount.get());

        pl.verifyResult(emf);
        emf.close();
    }
}
