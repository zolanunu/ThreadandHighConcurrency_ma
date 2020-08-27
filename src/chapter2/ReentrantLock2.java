package chapter2;

import java.sql.Time;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/***
 * ReentrantLock1： 中显示了synchronized 是可重入的，ReentrantLock锁可以替代Synchronized（自动解锁的）
 * lock必需要手动解锁
 *
 * ReentrantLock 与 Synchronized
 * */
public class ReentrantLock2 {
    Lock lock = new ReentrantLock();
    void m1() {
        try {
            lock.lock(); // Synchronized(this)
            for(int i = 0; i < 10; i++) {
                TimeUnit.SECONDS.sleep(1);
                System.out.println(i);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    void m2() {
        try {
            lock.lock();
            System.out.println("m2....");
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ReentrantLock2 r2 = new ReentrantLock2();
        new Thread(r2::m1).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(r2::m2).start();
    }
}
