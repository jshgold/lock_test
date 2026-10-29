package com.run;

import com.entity.Item;
import com.service.PessimisticLockTest;
import jakarta.persistence.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class DeadLockRun {
    private static final PessimisticLockTest pl = PessimisticLockTest.getInstance();

    public static void run() throws InterruptedException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lockTestUnit");

        // Item 1과 Item 2를 각각 데이터베이스에 생성
        pl.initDataOnce(emf);

        CountDownLatch latch = new CountDownLatch(2);
        // 두 스레드가 각각 첫 번째 락을 획득할 때까지 기다렸다가 동시에 다음 단계로 넘어가게 하는 장치
        CyclicBarrier barrier = new CyclicBarrier(2);

        System.out.println("=== 데드락 재현 테스트 시작 ===");

        // 스레드 A: Item 1 락 획득 -> Item 2 락 요구
        Thread threadA = new Thread(() -> {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                System.out.println("Thread-A: Item 1 배타 락 획득 시도");
                em.find(Item.class, 1L, LockModeType.PESSIMISTIC_WRITE);
                System.out.println("Thread-A: Item 1 락 획득 완료");

                barrier.await(); // Thread-B가 Item 2 락을 획득할 때까지 대기

                System.out.println("Thread-A: Item 2 배타 락 획득 시도 (대기 발생)");
                em.find(Item.class, 2L, LockModeType.PESSIMISTIC_WRITE);

                tx.commit();
                System.out.println("Thread-A: 커밋 완료");
            } catch (Exception e) {
                System.out.println("Thread-A 예외 발생 (롤백됨): " + e.getCause().getMessage());
                tx.rollback();
            } finally {
                em.close();
                latch.countDown();
            }
        });

        // 스레드 B: Item 2 락 획득 -> Item 1 락 요구
        Thread threadB = new Thread(() -> {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                System.out.println("Thread-B: Item 2 배타 락 획득 시도");
                em.find(Item.class, 2L, LockModeType.PESSIMISTIC_WRITE);
                System.out.println("Thread-B: Item 2 락 획득 완료");

                barrier.await(); // Thread-A가 Item 1 락을 획득할 때까지 대기

                System.out.println("Thread-B: Item 1 배타 락 획득 시도 (대기 발생)");
                em.find(Item.class, 1L, LockModeType.PESSIMISTIC_WRITE);

                tx.commit();
                System.out.println("Thread-B: 커밋 완료");
            } catch (Exception e) {
                System.out.println("Thread-B 예외 발생 (롤백됨): " + e.getCause().getMessage());
                tx.rollback();
            } finally {
                em.close();
                latch.countDown();
            }
        });

        threadA.start();
        threadB.start();

        latch.await();
        System.out.println("=== 데드락 테스트 종료 ===");
        emf.close();
    }
}
