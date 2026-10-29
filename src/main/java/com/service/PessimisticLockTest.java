package com.service;

import com.entity.Item;
import jakarta.persistence.*;
import java.util.concurrent.*;

public class PessimisticLockTest {
    public static void main(String[] args) throws InterruptedException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lockTestUnit");

        initData(emf);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("PostgreSQL 비관적 락 테스트 시작...");
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    executePessimisticLock(emf);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        verifyResult(emf);
        emf.close();
    }

    private static void executePessimisticLock(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // PostgreSQL에 SELECT * FROM Item WHERE id=1 FOR UPDATE 쿼리를 전송하여 배타 락 획득
            Item item = em.find(Item.class, 1L, LockModeType.PESSIMISTIC_WRITE);
            item.decreaseStock();

            tx.commit(); // 커밋 시점에 락 해제
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }

    private static void initData(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new Item(1L, 100));
        em.getTransaction().commit();
        em.close();
    }

    private static void verifyResult(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        Item item = em.find(Item.class, 1L);
        System.out.println("최종 재고: " + item.getStock());
        em.close();
    }
}
