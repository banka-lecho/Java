package info.kgeorgiy.ja.Shpileva.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.function.Function;
import java.util.*;

public class ParallelMapperImpl implements ParallelMapper {
    int count; // :NOTE: Race
    private RuntimeException taskRunException; // :NOTE: Race
    private final List<Thread> workingThreads = new ArrayList<>();
    private final Queue<Runnable> queue = new ArrayDeque<>();

    /**
     * Constructor creates, run and launch given count of threads.
     *
     * @param threads count of threads
     */
    public ParallelMapperImpl(int threads) {
        for (int i = 0; i < threads; i++) {
            workingThreads.add(new Thread(() -> {
                Runnable task;
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        synchronized (queue) {
                            while (queue.isEmpty()) {
                                queue.wait();
                            }
                            task = queue.poll();
                        }
                        try {
                            task.run();
                        } catch (RuntimeException ignored) {

                        }
                    }
                } catch (InterruptedException ignored) {
                }
            }));
            workingThreads.get(i).start();
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performed in parallel.
     *
     * @param f    function
     * @param args arguments of function
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        // :NOTE: Race
        count = args.size();
        List<R> interResult = new ArrayList<>(Collections.nCopies(count, null));
        for (int i = 0; i < args.size(); i++) {
            final int position = i;
            Runnable task = () -> {
                try {
                    interResult.set(position, f.apply(args.get(position)));
                } catch (RuntimeException e) {
                    if (taskRunException == null) {
                        taskRunException = e;
                    } else {
                        taskRunException.addSuppressed(e);
                    }
                }
                synchronized (this) {
                    if (--count == 0) {
                        notify();
                    }
                }
            };
            // :NOTE: addAll
            synchronized (queue) {
                queue.add(task);
                queue.notify();
            }
        }
        if (taskRunException != null) {
            throw taskRunException;
        }

        return waitAllThreads(interResult); // :NOTE: Race
    }

    /**
     * Function is responsible for waiting
     * for all threads to complete and throwing all exceptions.
     */
    public synchronized <R> List<R> waitAllThreads(List<R> result) throws InterruptedException {
        if (count != 0) {
            wait();
        }
        return result;
    }

    /**
     * Stops all threads. All unfinished mappings are left in undefined state.
     */
    @Override
    public void close() {
        for (Thread workingThread : workingThreads) {
            workingThread.interrupt();
        }
        for (Thread workingThread : workingThreads) {
            try {
                workingThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}