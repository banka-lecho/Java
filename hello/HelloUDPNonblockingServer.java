package info.kgeorgiy.ja.Shpileva.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class HelloUDPNonblockingServer extends AbstractServer implements HelloServer {
    // ясен хуй, что это
    private static ExecutorService listener;
    // ясен хуй что это
    private DatagramChannel datagramChannel;
    // ясен хуй, что это
    Selector selector;
    // очередь из буфферов?????????????/
    private static final Queue<ByteBuffer> buffers = new ConcurrentLinkedQueue<>();
    // очеедь из ответов????????????///
    private final Queue<Answer> answers = new ConcurrentLinkedQueue<>();

    private static class Answer {
        // у каждого ответа есть буффер, из которого он получает инфу и адресс, по которому он подключается к каналу
        private final SocketAddress adress;
        private final String buffer;

        Answer(SocketAddress adress, String buffer) {
            this.buffer = buffer;
            this.adress = adress;
        }
    }

    /**
     * Starts a new Hello server.
     * This method should return immediately.
     *
     * @param port    server port.
     * @param threads number of working threads.
     */
    @Override
    public void start(int port, int threads) {
        try {
            // заводим селектор
            selector = Selector.open();
            // открываем канал
            datagramChannel = DatagramChannel.open();
            // подвязываем канал к опредленному адресу
            datagramChannel.bind(new InetSocketAddress(port));
            // ставим его в неблокирующий режим
            datagramChannel.configureBlocking(false);
            // регистриуем его с ключом чтения
            datagramChannel.register(selector, OP_READ);

            // завдоим под каждый поток буффер, который будем соразмерен с
            for (int thread = 0; thread < threads; thread++) {
                buffers.add(ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize()));
            }

            // пул потоков
            poolWorkers = newFixedThreadPool(threads);
            // слушаетль, который будет просшивать данный порт и сигналы на нем
            listener = newSingleThreadExecutor();
            // в него сабмичем таски: то, что мы хотим услышать
            listener.submit(() -> {
                try {
                    listen();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            System.err.println("An error occurred while processing request.");
        }
    }

    private void listen() throws IOException {
        while (!Thread.interrupted() && !datagramChannel.socket().isClosed()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
                iterator.remove();
            }
        }
    }

    private void write(SelectionKey key) {
        // у нас есть очередь ответов, которые ждут пока их обработают
        Answer answer = answers.poll();
        if (answer != null) {
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            // кладем в буффер ответ
            buffer = ByteBuffer.wrap(answer.buffer.getBytes(StandardCharsets.UTF_8));
            try {
                // пытаемся получить канал по данному ключу
                DatagramChannel channel = (DatagramChannel) key.channel();
                // и отправить ответ клиенту по данному адрессу
                channel.send(buffer, answer.adress);
            } catch (IOException e) {
                System.err.println("An error occurred while processing request.");
            }
        }
        // меняем ключ на чтение
        key.interestOpsOr(OP_READ);
    }

    private void read(SelectionKey key) {
        // если ключ на чтение, то в пул потоков отправляем добавлять в массив ответов ответы
        try {
            // пытаемся получить канал по заданному ключу
            DatagramChannel channel = (DatagramChannel) key.channel();
            // берем буффер, который последний ждет, чтобы в него запихнули ответ и сразу удаляем его
            ByteBuffer bb = buffers.poll();
            // если у нас нет никаких буфферов
            if (bb == null) {
                selector.wakeup();
                return;
            }
            // buffer.flip() подготавливает буффер к записи по каналу или для получения
            bb.clear();
            SocketAddress address = channel.receive(bb);
            poolWorkers.submit(() -> {
                answers.add(
                        new Answer(
                                address,
                                "Hello, " + StandardCharsets.UTF_8.decode(bb.flip())
                        )
                );
                key.interestOps(OP_WRITE);
                buffers.add(bb);
                selector.wakeup();
            });
        } catch (IOException e) {
            System.err.println("An error occurred while processing request.");
        }
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        try {
            datagramChannel.close();
            selector.close();
        } catch (IOException e) {
            System.err.println("An I/O error occurs.");
        }
        Utils.shutdownWorkers(listener);
        Utils.shutdownWorkers(poolWorkers);
    }

    public static void main(String[] args) {
        main(args, 1);
    }
}
