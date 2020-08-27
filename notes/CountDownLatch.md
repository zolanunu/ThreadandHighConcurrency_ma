# Java并发之CountDownLatch

## 初始CountDownLatch

countDownLatch的作用就是等待其他的线程都执行完任务，必要时可以对各个任务的执行结果进行汇总，然后主线程才继续
	
## 原理剖析

countDown()： 计数器-1 （并不是一个线程只能调用一次，当同一个线程调用多次countDown()方法的时候，每次都会-1）

await()：调用该方法的线程处于等待状态，一般是主线程调用， 也不是只能一个线程调用该方法，可以多线程调用，那么这几个线程都会处于等待的状态，并且以共享模式同一把锁

```	
public class CountDownLatchExample {
  public static void main(String[] args) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(5);
    Service service = new Service(latch);
    Runnable task = () -> service.exec();
	for (int i = 0; i < 5; i++) {
      Thread thread = new Thread(task);
      thread.start();
    }
	System.out.println("main thread await. ");
    latch.await();
    System.out.println("main thread finishes await. ");
  }
}
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
	private void sleep(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
    }
  }
}
```	

在上面的例子中，首先声明了一个CountDownLatch对象，并且由主线程创建了5个线程，分别执行任务，在每个任务中，当前线程会休眠2秒。在启动线程之后，主线程调用了CountDownLatch.await()方法，此时，主线程将在此处等待创建的5个线程执行完任务之后才继续往下:

```
Thread-0 execute task. 
Thread-1 execute task. 
Thread-2 execute task. 
Thread-3 execute task. 
Thread-4 execute task. 
main thread await. 
Thread-0 finished task. 
Thread-4 finished task. 
Thread-3 finished task. 
Thread-1 finished task. 
Thread-2 finished task. 
main thread finishes await. 
```
	
从输出结果可以看出，主线程先启动了五个线程，然后主线程进入等待状态，当这五个线程都执行完任务之后主线程才结束了等待。

上述代码中需要注意的是，在执行任务的线程中，使用了try...finally结构，该结构可以保证创建的线程发生异常时CountDownLatch.countDown()方法也会执行，也就保证了主线程不会一直处于等待状态。

CountDownLatch非常适合于对任务进行拆分，使其并行执行，比如某个任务执行2s，其对数据的请求可以分为五个部分，那么就可以将这个任务拆分为5个子任务，分别交由五个线程执行，执行完成之后再由主线程进行汇总，此时，总的执行时间将决定于执行最慢的任务，平均来看，还是大大减少了总的执行时间。

另外一种比较合适使用CountDownLatch的地方是使用某些外部链接请求数据的时候，比如图片。在本人所从事的项目中就有类似的情况，因为我们使用的图片服务只提供了获取单个图片的功能，而每次获取图片的时间不等，一般都需要1.5s~2s。当我们需要批量获取图片的时候，比如列表页需要展示一系列的图片，如果使用单个线程顺序获取，那么等待时间将会极长，此时我们就可以使用CountDownLatch对获取图片的操作进行拆分，并行的获取图片，这样也就缩短了总的获取时间。
	       
CountDownLatch是基于AbstractQueuedSynchronizer实现的，在AbstractQueuedSynchronizer中维护了一个volatile类型的整数state，volatile可以保证多线程环境下该变量的修改对每个线程都可见，并且由于该属性为整型，因而对该变量的修改也是原子的。创建一个CountDownLatch对象时，所传入的整数n就会赋值给state属性，当countDown()方法调用时，该线程就会尝试对state减一，而调用await()方法时，当前线程就会判断state属性是否为0，如果为0，则继续往下执行，如果不为0，则使当前线程进入等待状态，直到某个线程将state属性置为0，其就会唤醒在await()方法中等待的线程。如下是countDown()方法的源代码：

```
public void countDown() {
	sync.releaseShared(1);
}
```
	
sync也即一个继承了AbstractQueuedSynchronizer的类实例，该类是CountDownLatch的一个内部类，其声明如下：

```
	
private static final class Sync extends AbstractQueuedSynchronizer {
	private static final long serialVersionUID = 4982264981922014374L;
	Sync(int count) {
    	setState(count);
  }
	int getCount() {
    return getState();
  }
	protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;
  }
	protected boolean tryReleaseShared(int releases) {
    for (;;) {
      int c = getState();   // 获取当前state属性的值
      if (c == 0)   // 如果state为0，则说明当前计数器已经计数完成，直接返回
        return false;
      int nextc = c-1;
      if (compareAndSetState(c, nextc)) // 使用CAS算法对state进行设置
        return nextc == 0;  // 设置成功后返回当前是否为最后一个设置state的线程
    }
  }
}
```

这里tryReleaseShared(int)方法即对state属性进行减一操作的代码。可以看到，CAS也即compare and set的缩写，jvm会保证该方法的原子性，其会比较state是否为c，如果是则将其设置为nextc（自减1），如果state不为c，则说明有另外的线程在getState()方法和compareAndSetState()方法调用之间对state进行了设置，当前线程也就没有成功设置state属性的值，其会进入下一次循环中，如此往复，直至其成功设置state属性的值，即countDown()方法调用成功。
	
在countDown()方法中调用的sync.releaseShared(1)调用时实际还是调用的tryReleaseShared(int)方法，如下是releaseShared(int)方法的实现：

```	
public final boolean releaseShared(int arg) {
	if (tryReleaseShared(arg)) { // 先设置值，如果成功，才真正去释放队列中的节点
		doReleaseShared();
		return true;
	}
	return false;
}
```
	
可以看到，在执行sync.releaseShared(1)方法时，其在调用tryReleaseShared(int)方法时会在无限for循环中设置state属性的值，设置成功之后其会根据设置的返回值（此时state已经自减了一），即当前线程是否为将state属性设置为0的线程，来判断是否执行if块中的代码。
	
doReleaseShared()方法主要作用是唤醒调用了await()方法的线程。需要注意的是，如果有多个线程调用了await()方法，这些线程都是以共享的方式等待在await()方法处的，试想，如果以独占的方式等待，那么当计数器减少至零时，就只有一个线程会被唤醒执行await()之后的代码，这显然不符合逻辑。如下是doReleaseShared()方法的实现代码：

```	
	private void doReleaseShared() {
		for (;;) {
    Node h = head;  // 记录等待队列中的头结点的线程
    if (h != null && h != tail) {   // 头结点不为空，且头结点不等于尾节点
      int ws = h.waitStatus;
      if (ws == Node.SIGNAL) {  // SIGNAL状态表示当前节点正在等待被唤醒
        if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))    // 清除当前节点的等待状态
          continue;
        unparkSuccessor(h); // 唤醒当前节点的下一个节点
      } else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
        continue;
    }
    if (h == head)  // 如果h还是指向头结点，说明前面这段代码执行过程中没有其他线程对头结点进行过处理
      break;
  }
}
```
	
在doReleaseShared()方法中（始终注意当前方法是最后一个执行countDown()方法的线程执行的），首先判断头结点不为空，且不为尾节点，说明等待队列中有等待唤醒的线程，这里需要说明的是，在等待队列中，头节点中并没有保存正在等待的线程，其只是一个空的Node对象，真正等待的线程是从头节点的下一个节点开始存放的，因而会有对头结点是否等于尾节点的判断。在判断等待队列中有正在等待的线程之后，其会清除头结点的状态信息，并且调用unparkSuccessor(Node)方法唤醒头结点的下一个节点，使其继续往下执行。如下是unparkSuccessor(Node)方法的具体实现：
	
```
private void unparkSuccessor(Node node) {
  int ws = node.waitStatus;
  if (ws < 0)
    compareAndSetWaitStatus(node, ws, 0);   // 清除当前节点的等待状态
	Node s = node.next;
  if (s == null || s.waitStatus > 0) {  // s的等待状态大于0说明该节点中的线
	程已经被外部取消等待了
    s = null;
    // 从队列尾部往前遍历，找到最后一个处于等待状态的节点，用s记录下来
    for (Node t = tail; t != null && t != node; t = t.prev)
      if (t.waitStatus <= 0)
        s = t;
  }
  if (s != null)
    LockSupport.unpark(s.thread);   // 唤醒离传入节点最近的处于等待状态的节点线程
}
```
	
可以看到，unparkSuccessor(Node)方法的作用是唤醒离传入节点最近的一个处于等待状态的线程，使其继续往下执行。前面我们讲到过，等待队列中的线程可能有多个，而调用countDown()方法的线程只唤醒了一个处于等待状态的线程，这里剩下的等待线程是如何被唤醒的呢？其实这些线程是被当前唤醒的线程唤醒的。具体的我们可以看看await()方法的具体执行过程。如下是await()方法的代码：

```	
public void await() throws InterruptedException {
	sync.acquireSharedInterruptibly(1);
}
```
	
await()方法实际还是调用了Sync对象的方法acquireSharedInterruptibly(int)方法，如下是该方法的具体实现：

```	
public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
  if (Thread.interrupted())
    throw new InterruptedException();
  if (tryAcquireShared(arg) < 0)
    doAcquireSharedInterruptibly(arg);
}
```
	
可以看到acquireSharedInterruptibly(int)方法判断当前线程是否需要以共享状态获取执行权限，这里tryAcquireShared(int)方法是AbstractQueuedSynchronizer中的一个模板方法，其具体实现在前面的Sync类中，可以看到，其主要是判断state是否为零，如果为零则返回1，表示当前线程不需要进行权限获取，可直接执行后续代码，返回-1则表示当前线程需要进行共享权限。具体的获取执行权限的代码在doAcquireSharedInterruptibly(int)方法中，如下是该方法的具体实现：
	
```
private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
  final Node node = addWaiter(Node.SHARED); // 使用当前线程创建一个共享模式的节点
  boolean failed = true;
  try {
    for (;;) {
      final Node p = node.predecessor();    // 获取当前节点的前一个节点
      if (p == head) {  // 判断前一个节点是否为头结点
        int r = tryAcquireShared(arg);  // 查看当前线程是否获取到了执行权限
        if (r >= 0) {   // 大于0表示获取了执行权限
          setHeadAndPropagate(node, r); // 将当前节点设置为头结点，并且唤醒后面处于等待状态的节点
          p.next = null; // help GC
          failed = false;
          return;
        }
      }
      
      // 走到这一步说明没有获取到执行权限，就使当前线程进入“搁置”状态
      if (shouldParkAfterFailedAcquire(p, node) &&
          parkAndCheckInterrupt())
        throw new InterruptedException();
    }
  } finally {
    if (failed)
      cancelAcquire(node);
  }
}
```
	
在doAcquireSharedInterruptibly(int)方法中，首先使用当前线程创建一个共享模式的节点。然后在一个for循环中判断当前线程是否获取到执行权限，如果有（r >= 0判断）则将当前节点设置为头节点，并且唤醒后续处于共享模式的节点；如果没有，则对调用shouldParkAfterFailedAcquire(Node, Node)和
parkAndCheckInterrupt()方法使当前线程处于“搁置”状态，该“搁置”状态是由操作系统进行的，这样可以避免该线程无限循环而获取不到执行权限，造成资源浪费，这里也就是线程处于等待状态的位置，也就是说当线程被阻塞的时候就是阻塞在这个位置。当有多个线程调用await()方法而进入等待状态时，这几个线程都将等待在此处。这里回过头来看前面将的countDown()方法，其会唤醒处于等待队列中离头节点最近的一个处于等待状态的线程，也就是说该线程被唤醒之后会继续从这个位置开始往下执行，此时执行到tryAcquireShared(int)方法时，发现r大于0（因为state已经被置为0了），该线程就会调用setHeadAndPropagate(Node, int)方法，并且退出当前循环，也就开始执行awat()方法之后的代码。这里我们看看setHeadAndPropagate(Node, int)方法的具体实现：

```	
private void setHeadAndPropagate(Node node, int propagate) {
  Node h = head;
  setHead(node);    // 将当前节点设置为头节点
  // 检查唤醒过程是否需要往下传递，并且检查头结点的等待状态
  if (propagate > 0 || h == null || h.waitStatus < 0 ||
      (h = head) == null || h.waitStatus < 0) {
    Node s = node.next;
    if (s == null || s.isShared())  // 如果下一个节点是尝试以共享状态获取获取执行权限的节点，则将其唤醒
      doReleaseShared();
  }
}
```
	
setHeadAndPropagate(Node, int)方法主要作用是设置当前节点为头结点，并且将唤醒工作往下传递，在传递的过程中，其会判断被传递的节点是否是以共享模式尝试获取执行权限的，如果不是，则传递到该节点处为止（一般情况下，等待队列中都只会都是处于共享模式或者处于独占模式的节点）。也就是说，头结点会依次唤醒后续处于共享状态的节点，这也就是共享锁与独占锁的实现方式。这里doReleaseShared()方法也就是我们前面讲到的会将离头结点最近的一个处于等待状态的节点唤醒的方法。

## 参考


```
简书：
https://www.jianshu.com/p/128476015902
```