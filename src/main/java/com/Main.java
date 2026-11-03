package com;

import com.run.AtomicQueryRun;
import com.run.OptimisticLockRun;
import com.run.PessimisticLockRun;


public class Main {

    public static void main(String[] args) throws InterruptedException{
        long startTime = System.currentTimeMillis();
        //비관적 락 테스트
//        PessimisticLockRun.run();
        //데드락 테스트
//        DeadLockRun.run();

        //원자성 쿼리
//        AtomicQueryRun.run();

        //낙관적 락
        OptimisticLockRun.run();
        long endTime = System.currentTimeMillis();
        System.out.printf("실행시간 : %d ms",endTime - startTime);
    }

}