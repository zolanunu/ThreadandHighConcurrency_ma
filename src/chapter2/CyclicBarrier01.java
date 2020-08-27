package chapter2;

import org.omg.CORBA.INTERNAL;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrier01 {
    public static void main(String[] args) {
        //CyclicBarrier barrier01 = new CyclicBarrier(20);
        CyclicBarrier barrier02 = new CyclicBarrier(20, ()->{System.out.println("满人了");});
        /** CyclicBarrier barrier03 = new CyclicBarrier(20, new Runnable() {
            public void run() {
                System.out.println("满人了,发车");
            }
        });
         * */
        for(int i = 0; i < 100; i++) {
            new Thread(() -> {
               try {
                   //barrier01.await();
                   barrier02.await();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               } catch (BrokenBarrierException e) {
                   e.printStackTrace();
               }
            }).start();
        }
    }
}
