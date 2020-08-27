package chapter2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Service {
    private CountDownLatch latch;
    public Service(CountDownLatch latch) {
        this.latch = latch;
    }
    public void exec() {
        try {
            System.out.println(Thread.currentThread().getName() + " execute task. ");
            sleep(2);
            System.out.println(Thread.currentThread().getName() + " finished task. ");
        } finally {
            latch.countDown();
        }
    }

    public void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
