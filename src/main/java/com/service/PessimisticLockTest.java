package com.service;

import com.entity.Item;
import jakarta.persistence.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PessimisticLockTest {
    private static AtomicBoolean initialized = new AtomicBoolean(false);
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

    public static PessimisticLockTest getInstance() {
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


    public void executePessimisticLock(EntityManagerFactory emf) {
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
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void initDataOnce(EntityManagerFactory emf) {

        // false일 때만 true로 바꾸고 통과 (동시에 여러 스레드가 와도 딱 1명만 성공)
        if (initialized.compareAndSet(false, true)) {
//            initDataDead(emf);
//            initData(emf);
            initData(emf,1_000_000);
        }
    }

    private void initDataDead(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new Item(1L, 100)); // 첫 번째 자원
        em.persist(new Item(2L, 100)); // 두 번째 자원
        em.getTransaction().commit();
        em.close();
    }

    private void initData(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(new Item(1L, 1_000_000));
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private void initData(EntityManagerFactory emf, int stock) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new Item(1L, stock));
        em.getTransaction().commit();
        em.close();
    }

    public void verifyResult(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        Item item = em.find(Item.class, 1L);
        System.out.println("최종 재고: " + item.getStock());
        em.close();
    }

    public boolean decreaseStockAtomic(EntityManagerFactory emf, Long itemId, int quantity) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // JPQL 벌크 연산으로 단일 UPDATE 쿼리 직접 실행
            int updatedRows = em.createQuery(
                            "UPDATE Item i SET i.stock = i.stock - :quantity " +
                                    "WHERE i.id = :id AND i.stock >= :quantity"
                    )
                    .setParameter("quantity", quantity)
                    .setParameter("id", itemId)
                    .executeUpdate();

            tx.commit();

            // 영향받은 행(Row) 수가 1이면 차감 성공, 0이면 조건(stock >= quantity) 미충족으로 실패
            return updatedRows > 0;
        } catch (Exception e) {
            tx.rollback();
            return false;
        } finally {
            em.close();
        }
    }


}
