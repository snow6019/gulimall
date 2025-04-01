package com.atguigu.gulimall.product.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadTest {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.execute(()->{
            System.out.println("1");
        });
        int maxValue = Integer.MAX_VALUE;
        System.out.println(maxValue);
        pool.execute(()->{
            System.out.println("2");
        });
        pool.execute(()->{
            System.out.println("3");
        });
    }
}
