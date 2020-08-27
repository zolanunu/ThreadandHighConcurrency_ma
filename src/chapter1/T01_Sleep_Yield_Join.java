package chapter1;

public class T01_Sleep_Yield_Join {
    // 线程的几个方法
    public static void main(String[] args) {
        // testSleep();
        // testYield();
        testJoin();
    }
    // 1. sleep（） 方法就是睡眠，当前线程暂停一段时间让别的线程去运行，sleep是怎么复活的？
    // 根据你的睡眠时间而定，等睡眠时间到了规定的时间，自动复活
    static void testSleep() {
        new Thread(()->{
            for(int i = 0; i < 10; i++) {
            System.out.println(i + "sleep");
            try {
                //System.out.println("sleep-------------");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }}).start();
    }

    // 2. yield 方法： 当线程正在执行的时候停止运行，进入到等待队列中，回到等待队列里面，
    // 等待系统重新调度算法里面，还依然有可能把你哥哥回去的这个线程拿回来继续执行
    // 另一种可能是把原来等待的那些拿出一个来执行，所以yield的作用是让出一下CPU，后面线程能不能抢到资源执行就完全看资源调度的情况
    static void testYield() {
        new Thread(()->{
            for(int i = 0; i < 10; i++) {
                System.out.println(i + "yield");
                if(i %10 == 0) {
                    Thread.yield();
                }
            }
        }).start();
        new Thread(()->{
            for(int i = 0; i < 10; i++) {
                System.out.println("-------------------yield2--" + i);
                if(i %10 == 0) {
                    Thread.yield();
                }
            }
        }).start();
    }

    // 3 join方法：自身线程t2加入了其他的线程t1，自身线程等待，等到调用的线程t1执行完毕， 自身线程t2才运行
    static void testJoin() {
        Thread t1 = new Thread(()->{
            for(int i = 0; i < 10; i++) {
                System.out.println(i + "join: t1");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread t2 = new Thread(()->{
            try {
                System.out.println("t1 join in t2");
                t1.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for(int i = 0; i < 10; i++) {
                System.out.println(i + "join t2");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();
        t2.start();
    }
}
