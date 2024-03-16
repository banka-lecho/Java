package info.kgeorgiy.ja.Shpileva.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import static java.lang.Math.min;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class implements ScalarIP
 *
 * @author Anastasia Shpileva
 */
public class IterativeParallelism implements ScalarIP {
    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        this.mapper = null;
    }

    public static void joinInterruptThread(List<Thread> listOfThreads) throws InterruptedException {
        InterruptedException exception = null;
        int threadsSize = listOfThreads.size();
        for (int i = 0; i < threadsSize; i++) {
            Thread thread = listOfThreads.get(i);
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
                for (int j = i; j < threadsSize; j++) {
                    listOfThreads.get(j).interrupt();
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * divides the array into streams and operates on them
     *
     * @param threads    number or concurrent threads.
     * @param values     some elements.
     * @param func       function for operating with primary values in threads.
     * @param funcReturn function for operating with primary values in threads.
     * @return <R> result of multi-threaded partitioning and func.
     * @throws InterruptedException if thread are interrupts.
     */
    private <T, R> R multithreadingExecution(List<? extends T> values, int threads,
                                             Function<? super Stream<? extends T>, R> func,
                                             Function<? super Stream<R>, R> funcReturn) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("No such elements in values");
        }
        if (threads > values.size()) {
            threads = values.size();
        }
        List<Thread> listOfThreads = new ArrayList<>();

        int sizeOfSublist = values.size() / threads;
        int countOfResidue = values.size() % threads;

        List<R> listOfRes = new ArrayList<>(Collections.nCopies(threads, null));
        List<Stream<? extends T>> list = new ArrayList<>();

        int left = 0;
        for (int i = 0; i < threads; i++) {
            int right = min((left + sizeOfSublist + (i < countOfResidue ? 1 : 0)), values.size());
            final int l = left;
            final int r = right;
            final int index = i;
            if (mapper == null) {
                final Thread th = new Thread(() -> listOfRes.set(index, func.apply(values.subList(l, r).stream())));
                left = right;
                listOfThreads.add(th);
                th.start();
            } else {
                list.add(values.subList(l, r).stream());
                left = right;
            }
        }
        if (mapper == null) {
            joinInterruptThread(listOfThreads);
            return funcReturn.apply(listOfRes.stream());
        } else {
            return funcReturn.apply((mapper.map(func, list)).stream());
        }
    }

    /**
     * @param threads    number or concurrent threads.
     * @param values     some elements.
     * @param comparator compator for to comparing elements of any type.
     * @param <T>        type of value.
     * @return minimum result of function maximum.
     * @throws InterruptedException if thread are interrupts.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<? super Stream<? extends T>, T> funcMax = stream -> stream.max(comparator).orElse(null);
        return multithreadingExecution(values, threads, funcMax, funcMax);
    }

    /**
     * @param threads    number or concurrent threads.
     * @param values     some elements.
     * @param comparator compator for to comparing elements of any type.
     * @param <T>        type of value.
     * @return minimum result of function minimum.
     * @throws InterruptedException if thread are interrupts.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * @param threads   number or concurrent threads.
     * @param values    some elements.
     * @param predicate predicate for comparison between elements.
     * @param <T>       type of value.
     * @return result answer of predicate.
     * @throws InterruptedException if thread are interrupts.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return multithreadingExecution(values, threads,
                stream -> stream.allMatch(predicate),
                booleanStream -> booleanStream.allMatch(Boolean::booleanValue));
    }

    /**
     * @param threads   number or concurrent threads.
     * @param values    some elements.
     * @param predicate predicate for comparison between elements.
     * @param <T>       type of value.
     * @return result answer of predicate.
     * @throws InterruptedException if threads are interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    /**
     * @param threads   number or concurrent threads.
     * @param values    some elements.
     * @param predicate predicate for comparison between elements.
     * @param <T>       type of value.
     * @return result count of elements that that satisfy the conditions.
     * @throws InterruptedException if threads are interrupted.
     */
    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return multithreadingExecution(values, threads,
                stream -> stream.filter(predicate).count(),
                stream -> stream.reduce(Long::sum).orElse(0L)).intValue();
    }
}
