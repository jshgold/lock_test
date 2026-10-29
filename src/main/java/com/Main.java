package com;

import com.service.PessimisticLockTest;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {
    private static final PessimisticLockTest pl = PessimisticLockTest.getInstance();

    public static void main(String[] args) throws InterruptedException{
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lockTestUnit");

        pl.initDataOnce(emf);

        int threadCount = 1_000_000;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("PostgreSQL 비관적 락 테스트 시작...");
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    pl.executePessimisticLock(emf);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        pl.verifyResult(emf);
        emf.close();
    }
}