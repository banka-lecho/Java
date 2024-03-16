package info.kgeorgiy.ja.Shpileva.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class HelloUDPNonblockingClient extends AbstractClient implements HelloClient {
    static class ThreadRequestBuffer {
        int threadNumber;
        int requestNumber;
        ByteBuffer bytes;

        public ThreadRequestBuffer(int threadId, int requestId, int bufferSize) {
            this.bytes = ByteBuffer.allocate(bufferSize);
            this.threadNumber = threadId;
            this.requestNumber = requestId;
        }
    }

    int restAllRequests;

    /**
     * Runs Hello client.
     * This method should return when all requests are completed.
     *
     * @param host     server host
     * @param port     server port
     * @param prefix   request prefix
     * @param threads  number of request threads
     * @param requests number of requests per thread.
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            restAllRequests = requests;

            InetAddress inetAddress = InetAddress.getByName(host);
            SocketAddress address = new InetSocketAddress(inetAddress, port);

            List<DatagramChannel> channels = new ArrayList<>();
            try {
                Selector selector = Selector.open();
                for (int i = 0; i < threads; i++) {
                    // открываем канал
                    DatagramChannel channel = DatagramChannel.open();
                    // конектим его по адресу
                    channel.connect(address);
                    // переводим в неблокирующий режим
                    channel.configureBlocking(false);
                    // связываем канал с селектором и буффером потокв
                    channel.register(selector, OP_WRITE,
                            new ThreadRequestBuffer(i, 0, channel.socket().getReceiveBufferSize()));
                    // добавляем канал в лист каналов
                    channels.add(channel);
                }
                // пока у нас запросы не кончились
                while (restAllRequests >= 0 && !selector.keys().isEmpty() && !Thread.interrupted()) {
                    sendRequest(selector, prefix, threads, requests, address);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (UnknownHostException e) {
            System.err.println("Fail to find host IP address. Exception: " + e.getMessage());
        }
    }

    public void sendRequest(Selector selector, String prefix, int threadNumber,
                            int requestNumber, SocketAddress address) throws IOException {
        // получаем набор ключей, к которым привязан буффер и канал
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        // ответ селектора
        selector.select(10);
        // если ключи пусты, то заполняем их ключами записи
        if (selectedKeys.isEmpty()) {
            for (SelectionKey key : selector.keys()) {
                key.interestOps(OP_WRITE);
            }
        }
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = keys.iterator();
        DatagramChannel channel;
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            try {
                ThreadRequestBuffer trb = (ThreadRequestBuffer) key.attachment();
                if (key.isReadable()) {
                    // получаем канал прикрепп=ленный к этому ключу
                    channel = (DatagramChannel) key.channel();
                    // получаем буффер из этого канала
                    channel.receive(trb.bytes.clear());
                    trb.bytes.flip();
                    // заводим в строку запроса наш ответ из буффера
                    String response = StandardCharsets.UTF_8.decode(trb.bytes).toString();
                    // проверяем наш ответ
                    if (response.contains(String.format(
                            "%s%d_%d", prefix, trb.threadNumber + 1, trb.requestNumber + 1
                    ))) {
                        System.out.printf("RECI: %s\n", response);
                        // увеличиваем количество запросов, которые выполнились
                        trb.requestNumber++;
                    }
                    // меняем интерес у ключа
                    key.interestOps(OP_WRITE);
                    // если все наши запросы уже выполнились, то всё ок, закрываем канал
                    if (trb.requestNumber >= requestNumber) {
                        channel.close();
                    }
                } else if (key.isWritable()) {
                    // опять же получаем канал, который прикреплен к этому ключу
                    channel = (DatagramChannel) key.channel();
                    // формируем запрос
                    String bodyRequest = String.format(
                            "%s%d_%d", prefix, trb.threadNumber + 1, trb.requestNumber + 1
                    );
                    // отправляем запрос
                    channel.send(
                            ByteBuffer.wrap(bodyRequest.getBytes(StandardCharsets.UTF_8)),
                            address
                    );
                    // меняем ключ на чтение
                    key.interestOps(OP_READ);
                    System.out.printf("SEND: %s\n", bodyRequest);
                }
            } finally {
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) {
        main(args, 1);
    }
}