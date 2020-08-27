package chapter1;

// 线程的几个状态 如何变化的
public class T01_ThreadState {
    static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println(this.getState());
            for(int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("myThread: " + i);
            }
        }
    }

    public static void main(String[] args) {
        Thread t = new Thread();
        System.out.println(t.getState()); // just new state
        t.start(); // start 后就是Runnable状态
//        try {
//            System.out.println("join");
//            t.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        // 然后join之后，结束了是一个Timenated状态
        System.out.println(t.getState());

    }
}
