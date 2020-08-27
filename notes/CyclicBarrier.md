# Java并发之CyclicBarrier

## 初始CyclicBarrier

同步辅助类：CyclicBarrier， 可以理解为栅栏，将若干个线程处于等待状态，什么时候栅栏满了，就将栅栏内的线程notify运行。CyclicBarrier可以重用。

barrier(屏障)与互斥量、读写锁、自旋锁不同，它不是用来保护临界区的。相反，它跟条件变量一样，是用来协同多线程一起工作的。

条件变量是多线程间传递状态的改变来达到协同工作的效果。屏障是多线程各自做自己的工作，如果某一线程完成了工作，就等待在屏障那里，直到其他线程的工作都完成了，再一起做别的事。

CyclicBarrier类是一个同步机制。它可以通过一些算法来同步线程处理的过程。换言之，就是所有的线程必须等待对方，直到所有的线程到达屏障，然后继续运行。之所以叫做“循环屏障”，是因为这个屏障可以被重复使用。

## 源码分析

### 成员变量

```
/** The lock for guarding barrier entry */
private final ReentrantLock lock = new ReentrantLock();
/** Condition to wait on until tripped */
private final Condition trip = lock.newCondition();
/** The number of parties */
private final int parties;
/* The command to run when tripped */
private final Runnable barrierCommand;
/** The current generation */
private Generation generation = new Generation();

/**
 * Number of parties still waiting. Counts down from parties to 0
 * on each generation.  It is reset to parties on each new
 * generation or when broken.
 */
private int count;
```

### 成员函数

```
/**
 * Updates state on barrier trip and wakes up everyone.
 * Called only while holding lock.
 */
private void nextGeneration();
/**
 * Sets current barrier generation as broken and wakes up everyone.
 * Called only while holding lock.
 */
private void breakBarrier();
/**
 *Main barrier code, covering the various policies.
 */
private int dowait(boolean timed, long nanos);

public int getParties() {return parties;}

/*挂起当前线程，直至所有线程都到达barrier状态再同时执行后续任务**/
public int await() throws InterruptedException;
/**让这些线程等待至一定的时间，如果还有线程没有到达barrier状态就直接让到达barrier的线程执行后续任务。*/
public int await(long timeout, TimeUnit unit);
public boolean isBroken();
public void reset();
public int getNumberWaiting();
```


### 构造函数

```
public CyclicBarrier(int parties, Runnable barrierAction);
public CyclicBarrier(int parties) {this(parties, null);}
```

构造函数的具体实现：

```
/**
 * Creates a new {@code CyclicBarrier} that will trip when the
 * given number of parties (threads) are waiting upon it, and which
 * will execute the given barrier action when the barrier is tripped,
 * performed by the last thread entering the barrier.
 *
 * @param parties the number of threads that must invoke {@link #await}
 * before the barrier is tripped
 * @param barrierAction the command to execute when the barrier is
 * tripped, or {@code null} if there is no action
 * @throws IllegalArgumentException if {@code parties} is less than 1
 */
public CyclicBarrier(int parties, Runnable barrierAction) {
	if (parties <= 0) throw new IllegalArgumentException();
	// parties表示“必须同时到达barrier的线程个数”。
	this.parties = parties;
	// count表示“处在等待状态的线程个数”。
	this.count = parties;
	// barrierCommand表示“parties个线程到达barrier时，会执行的动作”。
	this.barrierCommand = barrierAction;
}
```

### await函数实现

await函数有两个重载函数，如下：

```
/*挂起当前线程，直至所有线程都到达barrier状态再同时执行后续任务**/
public int await() throws InterruptedException;
/**让这些线程等待至一定的时间，如果还有线程没有到达barrier状态就直接让到达barrier的线程执行后续任务。*/
public int await(long timeout, TimeUnit unit);
```

await()方法的具体实现为:

```
public int await() throws InterruptedException, BrokenBarrierException {
    try {
        return dowait(false, 0L);
    } catch (TimeoutException toe) {
        throw new Error(toe); // cannot happen;
    }
}
```

注意： await() 函数是由dowait()实现的，其具体实现代码为：

```
private int dowait(boolean timed, long nanos)
    throws InterruptedException, BrokenBarrierException,
           TimeoutException {
    final ReentrantLock lock = this.lock;
    // 获取“独占锁(lock)”
    lock.lock();
    try {
        // 保存“当前的generation”
        final Generation g = generation;

        // 若“当前generation已损坏”，则抛出异常。
        if (g.broken)
            throw new BrokenBarrierException();

        // 如果当前线程被中断，则通过breakBarrier()终止CyclicBarrier，唤醒CyclicBarrier中所有等待线程。
        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }

       // 将“count计数器”-1
       int index = --count;
       // 如果index=0，则意味着“有parties个线程到达barrier”。
       if (index == 0) {  // tripped
           boolean ranAction = false;
           try {
               // 如果barrierCommand不为null，则执行该动作。
               final Runnable command = barrierCommand;
               if (command != null)
                   command.run();
               ranAction = true;
               // 唤醒所有等待线程，并更新generation。
               nextGeneration();
               return 0;    //这里等价于return index;
           } finally {
               if (!ranAction)
                   breakBarrier();
           }
       }

        // 当前线程一直阻塞，直到“有parties个线程到达barrier” 或 “当前线程被中断” 或 “超时”这3者之一发生，
        // 当前线程才继续执行。
        for (;;) {
            try {
                // 如果不是“超时等待”，则调用awati()进行等待；否则，调用awaitNanos()进行等待。
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                // 如果等待过程中，线程被中断，则执行下面的函数。
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {
                    Thread.currentThread().interrupt();
                }
            }

            // 如果“当前generation已经损坏”，则抛出异常。
            if (g.broken)
                throw new BrokenBarrierException();

            // 如果“generation已经换代”，则返回index。
            if (g != generation)
                return index;

            // 如果是“超时等待”，并且时间已到，则通过breakBarrier()终止CyclicBarrier，唤醒CyclicBarrier中所有等待线程。
            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        // 释放“独占锁(lock)”
        lock.unlock();
    }
}

```

dowait()的作用是：让当前的线程阻塞，加入到barrier状态中，当满足了barrier的parties个数时/当前线程中断/超时，当前线程就会执行（breakbarrier()）

- **注意(1)：** generation 

```
private static class Generation {
	boolean broken = false;
    }
```
同一批到达barrier状态的线程就属于同一代，通过generation对象来判断是第几批barrier

当已有parties个线程到达barrrier，generation就会被更新

```
private void nextGeneration() {
        // signal completion of last generation
	// signalAll()唤醒CyclicBarrier上所有的等待线程
        trip.signalAll();
        // set up next generation
        count = parties;
        generation = new Generation();
    }
```
- **注意(2)：**breakBarrier(): 当前线程中断/超时

```
private void breakBarrier() {
	// 中断表示broken设置为true，意味着“将该Generation中断”
	generation.broken = true;
	/**重新初始化count **/
	count = parties;
	/**唤醒CyclicBarrier上所有的等待线程。*/
	trip.signalAll();
}
```

- **注意(03) ：**int index = --count;

首先index标记count，用来判断是不是“有parties个线程到达barrier”，即index是不是为0。当index=0时，如果barrierCommand不为null，则执行该barrierCommand，barrierCommand就是我们创建CyclicBarrier时，传入的Runnable对象。然后，调用nextGeneration()进行换代工作，nextGeneration()的源码如上。

- **注意(04)：** 在for(;;)循环中。timed是用来表示当前是不是“超时等待”线程。如果不是，则通过trip.await()进行等待；否则，调用awaitNanos()进行超时等待。

## CyclicBarrier与CountDownLatch

CountDownLatch和CyclicBarrier都能够实现线程之间的等待，只不过它们侧重点不同：

CountDownLatch一般用于某个线程A等待若干个其他线程执行完任务之后，它才执行；

而CyclicBarrier一般用于一组线程互相等待至某个状态，然后这一组线程再同时执行；

另外，CountDownLatch是不能够重用的，而CyclicBarrier是可以重用的。

## 使用场景示例

```
public class CyclicBarrier02 {
    public static void main(String[] args) {
        int N = 4;
        CyclicBarrier barrier  = new CyclicBarrier(N);
        for(int i=0;i<N;i++)
            new Writer(barrier).start();
    }
    static class Writer extends Thread {
        private CyclicBarrier cyclicBarrier;
        public Writer(CyclicBarrier cyclicBarrier) {
            this.cyclicBarrier = cyclicBarrier;
        }
        @Override
        public void run() {
            System.out.println("线程" + Thread.currentThread().getName() + "正在写入数据...");
            try {
                Thread.sleep(5000);      //以睡眠来模拟写入数据操作
                System.out.println("线程" + Thread.currentThread().getName() + "写入数据完毕，等待其他线程写入完毕");
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            System.out.println("所有线程写入完毕，继续处理其他任务...");
        }
    }
}

```

运行结果：

```
线程Thread-0正在写入数据...
线程Thread-2正在写入数据...
线程Thread-3正在写入数据...
线程Thread-1正在写入数据...
线程Thread-1写入数据完毕，等待其他线程写入完毕
线程Thread-3写入数据完毕，等待其他线程写入完毕
线程Thread-2写入数据完毕，等待其他线程写入完毕
线程Thread-0写入数据完毕，等待其他线程写入完毕
所有线程写入完毕，继续处理其他任务...
所有线程写入完毕，继续处理其他任务...
所有线程写入完毕，继续处理其他任务...
所有线程写入完毕，继续处理其他任务...
```

```
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrier03 {
    private static int SIZE = 5;
    private static CyclicBarrier cb;
    public static void main(String[] args) {
        cb = new CyclicBarrier(SIZE, new Runnable () {
            public void run() {
                System.out.println("CyclicBarrier's parties is: "+ cb.getParties());
            }
        });
        // 新建5个任务
        for(int i=0; i<SIZE; i++)
            new InnerThread().start();
    }

    static class InnerThread extends Thread{
        public void run() {
            try {
                System.out.println(Thread.currentThread().getName() + " wait for CyclicBarrier.");
                // 将cb的参与者数量加1
                cb.await();
                // cb的参与者数量等于5时，才继续往后执行
                System.out.println(Thread.currentThread().getName() + " continued.");
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

```

运行结果：

```
Thread-0 wait for CyclicBarrier.
Thread-4 wait for CyclicBarrier.
Thread-2 wait for CyclicBarrier.
Thread-3 wait for CyclicBarrier.
Thread-1 wait for CyclicBarrier.
CyclicBarrier's parties is: 5
Thread-2 continued.
Thread-0 continued.
Thread-1 continued.
Thread-3 continued.
Thread-4 continued.
```

主线程中新建了5个线程，所有的这些线程都调用cb.await()等待。所有这些线程一直等待，直到cb中所有线程都达到barrier时，执行新建cb时注册的Runnable任务。

## 参考


掘金：

<https://juejin.im/entry/6844903487482904584>

<https://juejin.im/post/6844903860473954318>

博客园：

<https://www.cnblogs.com/dolphin0520/p/3920397.html>
