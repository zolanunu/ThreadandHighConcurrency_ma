package chapter2;

import org.omg.CORBA.INTERNAL;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 比如在5秒中以后程序执行完毕以后就可能得到这把锁，如果得不到就不行
 * */
public class ReentrantLock3 {
    Lock lock = new ReentrantLock();
    void m1() {
        try {
            lock.lock();
            for(int i = 0; i < 3; i++) {
                TimeUnit.SECONDS.sleep(1);
                System.out.println(i);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 使用trylock进行尝试锁定锁，不管是否锁定成功，方法都会继续执行
     * 可以根据trylock的返回值来确定是否锁定
     * */

    void m2() {
//        boolean locked = lock.tryLock();
//        System.out.println("m2...." + locked);
//        if(locked) {
//            lock.unlock();
//        }
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS); // 5秒后申请锁
            System.out.println("m2...." + locked);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(locked) {
                lock.unlock();
            }
        }
    }


    public static void main(String[] args) {
        ReentrantLock3 r3 = new ReentrantLock3();
        new Thread(r3::m1).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(r3::m2).start();
    }
}
