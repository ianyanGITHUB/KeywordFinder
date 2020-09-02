package sample.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {
    //当前可用CPU数如何合理地估算线程池大小？http://ifeve.com/how-to-calculate-threadpool-size/
    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final Integer QUEUE_SIZE = 500;
    private static final String THREAD_NAME = "WORD_KEYWORD_SEARCH";
    public static volatile ThreadPoolExecutor executor;
    public static volatile ListeningExecutorService listeningExecutor;

    public static void init() {
        initTaskThreadPool();
    }

    private static ThreadPoolExecutor doThreadPoolExecutor(int coreSize, int maxSize, int queueSize, String threadName, long timeOut) {
        //线程名
        String threadNameStr = new StringBuilder(threadName).append("-%d").toString();
        //**ThreadFactoryBuilder**：线程工厂类就是将一个线程的执行单元包装成为一个线程对象，比如线程的名称,线程的优先级,线程是否是守护线程等线程；guava为了我们方便的创建出一个ThreadFactory对象,我们可以使用ThreadFactoryBuilder对象自行创建一个线程.
        ThreadFactory threadNameVal = new ThreadFactoryBuilder().setNameFormat(threadNameStr).build();
        //线程池
        return new ThreadPoolExecutor(coreSize, maxSize, timeOut, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueSize), threadNameVal, new ThreadPoolExecutor.AbortPolicy());
    }

    private static void initTaskThreadPool() {
        executor = doThreadPoolExecutor(PROCESSORS, PROCESSORS * 4, QUEUE_SIZE, THREAD_NAME, 400L);
        listeningExecutor =  MoreExecutors.listeningDecorator(executor);
    }
//**ListeningExecutorService** :由于普通的线程池，返回的Future，功能比较单一；Guava 定义了 ListenableFuture接口并继承了JDK concurrent包下的Future 接口，ListenableFuture 允许你注册回调方法(callbacks)，在运算（多线程执行）完成的时候进行调用。


}
