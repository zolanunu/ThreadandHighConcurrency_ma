package chapter2;

import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock 锁默认是非公平锁
 * 公平锁： 谁等在前面就让谁执行，而不是后来的人能抢到执行的机会
 * 公平锁：线程上会先检查队列里面有没有原来等着的，如果有的话他就会先进队列等待别人运行
 * */
public class ReentrantLock5 extends Thread {
    private static ReentrantLock lock = new ReentrantLock(true); // true：表示是公平锁
    public void run() {
        for(int i = 0; i < 100; i++) {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "获得锁");
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        ReentrantLock5 r5 = new ReentrantLock5();
        Thread th1 = new Thread(r5);
        Thread th2 = new Thread(r5);
        th1.start();
        th2.start();
    }
}
