package chapter2;

import java.util.concurrent.CountDownLatch;
/**
 *  主线程先启动了五个线程，然后主线程进入等待状态，
 *  当这五个线程都执行完任务之后主线程才结束了等待。
 * */
public class CountDownLatch1 {
    public static void main(String[] args) {
        CountDownLatch count = new CountDownLatch(5);
        Service service = new Service(count);
        Runnable task = () -> service.exec();
        for(int i = 0; i < 5; i++) {
            Thread thread = new Thread(task);
            thread.start();
        }
        System.out.println("main thread await...");
        try {
            count.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("main thread end await...");
    }


}
