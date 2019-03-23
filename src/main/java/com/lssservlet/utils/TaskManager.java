package com.lssservlet.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskManager {
    protected static final Logger log = LogManager.getLogger(TaskManager.class);
    private static AtomicInteger _taskCount = new AtomicInteger();
    private static Boolean _isStop = false;

    private static ConcurrentHashMap<String, ThreadPoolExecutor> _pool = new ConcurrentHashMap<String, ThreadPoolExecutor>();

    private static class TaskThreadFactory implements ThreadFactory {
        private AtomicInteger _count = new AtomicInteger(0);
        private String _name = null;

        public TaskThreadFactory(String name) {
            _name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            String threadName = _name + "-" + _count.addAndGet(1);
            t.setName(threadName);
            return t;
        }
    }

    private static <T> void startTask(String name, int corePoolSize, int maximumPoolSize, T t, Consumer<T> handler) {
        ThreadPoolExecutor poolExecutor = _pool.get(name);
        if (poolExecutor == null) {
            synchronized (_pool) {
                poolExecutor = _pool.get(name);
                if (poolExecutor == null) {
                    poolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>(), new TaskThreadFactory(name), (ta, executor) -> {
                                Task task = (Task) ta;
                                if (task != null)
                                    task.onRejected();
                            }) {
                        @Override
                        protected void beforeExecute(Thread t, Runnable r) {
                            super.beforeExecute(t, r);
                            Task task = (Task) r;
                            if (task != null)
                                task.beforeExecute();
                        }

                        @Override
                        protected void afterExecute(Runnable r, Throwable t) {
                            super.afterExecute(r, t);
                            Task task = (Task) r;
                            if (task != null)
                                task.afterExecute();
                            _taskCount.decrementAndGet();
                        }
                    };
                    _pool.put(name, poolExecutor);
                }
            }
        } else {
            if (poolExecutor.getMaximumPoolSize() != maximumPoolSize
                    || poolExecutor.getCorePoolSize() != corePoolSize) {
                log.warn("different pool size:{}", name);
            }
        }
        poolExecutor.execute(new Task(t, handler));
        _taskCount.incrementAndGet();
    }

    public static <T> void runTaskOnThread(String threadName, Consumer<T> handler) {
        startTask(threadName, 1, 1, null, handler);
    }

    public static <T> void runTaskOnThreadPool(String threadPoolName, int poolSize, Consumer<T> handler) {
        startTask(threadPoolName, poolSize, poolSize, null, handler);
    }

    public static Boolean isIdle() {
        if (_taskCount.get() == 0)
            return true;
        return false;
    }

    public static Boolean isStop() {
        return _isStop;
    }

    public static void stop() {
        _isStop = true;
        _pool.forEach((k, v) -> {
            v.shutdown();
            try {
                if (!v.awaitTermination(60, TimeUnit.SECONDS)) {
                    v.shutdownNow();
                    if (!v.awaitTermination(60, TimeUnit.SECONDS))
                        log.error("_threadPool did not terminate");
                }
            } catch (InterruptedException ie) {
                v.shutdownNow();
                // Preserve interrupt status
                // Thread.currentThread().interrupt();
            }
        });
    }

    private static class Task<T, R> implements Runnable {
        private final Consumer<T> _handler;
        private final T _data;

        public Task(T t, Consumer<T> handler) {
            _handler = handler;
            _data = t;
        }

        @Override
        public void run() {
            try {
                this._handler.accept(_data);
            } catch (Exception e) {
                log.error("Task run error", e);
            }
        }

        public T getData() {
            return _data;
        }

        private void beforeExecute() {
            // log.info("beforeExecute");
        }

        private void afterExecute() {
            // log.info("afterExecute");
        }

        private void onRejected() {
            // log.info("onRejected");
        }
    }
}
