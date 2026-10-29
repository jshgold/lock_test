package com.service;

import com.entity.Item;
import jakarta.persistence.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PessimisticLockTest {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private PessimisticLockTest() {

    }

    private static class Holder {
        private static final PessimisticLockTest instance = new PessimisticLockTest();
    }

    public static PessimisticLockTest getInstance() {
        return Holder.instance;
    }
    /* Double Check Null 방식
    private static volatile PessimisticLockTest instance;
    private PessimisticLockTest() {

    }

    public static synchronized PessimisticLockTest getInstance() {
        if (instance == null) {
            synchronized(PessimisticLockTest.class) {
                if (instance == null) {
                    instance = new PessimisticLockTest();
                }
            }
        }
        return instance;
    }

     */


    public static void executePessimisticLock(EntityManagerFactory emf) {
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

    public static void initDataOnce(EntityManagerFactory emf) {
        // false일 때만 true로 바꾸고 통과 (동시에 여러 스레드가 와도 딱 1명만 성공)
        if (initialized.compareAndSet(false, true)) {
            initData(emf);
        }
    }

    private static void initData(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new Item(1L, 100));
        em.getTransaction().commit();
        em.close();
    }

    public static void verifyResult(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        Item item = em.find(Item.class, 1L);
        System.out.println("최종 재고: " + item.getStock());
        em.close();
    }
}
