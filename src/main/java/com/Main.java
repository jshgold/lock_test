package com;

import com.run.AtomicQueryRun;


public class Main {

    public static void main(String[] args) throws InterruptedException{

        //비관적 락 테스트
//        PessimisticLockRun.run();
        //데드락 테스트
//        DeadLockRun.run();

        //원자성 쿼리
        AtomicQueryRun.run();
    }
}