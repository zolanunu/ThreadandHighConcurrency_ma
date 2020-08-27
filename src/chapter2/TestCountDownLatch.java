package chapter2;

import java.util.concurrent.CountDownLatch;

public class TestCountDownLatch {
    public static void main(String[] args) {
        //usingCountDownLatch();
        usingJoin();
    }
    private static void usingCountDownLatch() {
        Thread[] threads = new Thread[10]; // 100个线程
        CountDownLatch countDownLatch = new CountDownLatch(threads.length);
        System.out.println("countDownLatch : " + countDownLatch.getCount());
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                int result = 0;
                for(int j = 0; j < 10; j++) {
                    result += j;
                }
                countDownLatch.countDown();
                System.out.println("countDownLatch : " + countDownLatch.getCount());
                System.out.println(result + "thread's name" + Thread.currentThread().getName());
            });
        }
        for(int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        try {
            countDownLatch.await(); // 主线程调用了await
            System.out.println("countDownLatch : " + countDownLatch.getCount());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("end latch");
    }

    private static void usingJoin() {
        Thread[] threads = new Thread[100];
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                int result = 0;
                for(int j = 0; j < 1000; j++) {
                    result += j;
                }
            });
        }
        for(int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for(int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("end join");
    }
}
