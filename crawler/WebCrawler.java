package info.kgeorgiy.ja.Shpileva.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private Set<String> nextLayerLinks = ConcurrentHashMap.newKeySet();


    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.downloader = downloader;
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> visited = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errs = new ConcurrentHashMap<>();

        // :NOTE: обход сайтов получается не совсем честным

        // пусть есть такое дерево сайтов, я делаю download(A, depth = 4).
        // Но из-за того, что сайт C медленный, download дойдет до E через сайт B с depth = 1 раньше, чем через C с depth = 2
        // Из-за этого мы не пойдем скачивать F, G, H, хотяя должны
        //       A
        //     /  \
        //    B    C
        //    |    |
        //    D   /
        //    \  /
        //     E
        //    /|\
        //   F G H
        // :UPD: Исправила. Я отошла от обхода рекурсией и сейчас ссылки обхожу слой за слоем

        nextLayerLinks.add(url);
        if (depth >= 1) {
            // :NOTE: не очень хорошо, что у вас download рекурсивный, подумайте о не рекурсивном решении.
            // :UPD: теперь не рекурсивный, обходим сайты слой за слоем.
            for (int i = 1; i <= depth; i++) {
                int finalCurLayer = i;
                Phaser phaser = new Phaser(1 + nextLayerLinks.size());
                String[] processingLinks = nextLayerLinks.toArray(new String[0]);
                nextLayerLinks = ConcurrentHashMap.newKeySet();
                for (String pageUrl : processingLinks) {
                    downloaders.submit(() -> {
                        try {
                            if (!visited.contains(pageUrl) && !errs.containsKey(pageUrl)) {
                                Document page = downloader.download(pageUrl);
                                visited.add(pageUrl);
                                if (finalCurLayer != depth) {
                                    phaser.register();
                                    extractors.submit(() -> linkFromDoc(page, errs, pageUrl, phaser));
                                }
                            }
                        } catch (IOException downloadFailure) {
                            errs.put(pageUrl, downloadFailure);
                        } finally {
                            phaser.arrive();
                        }
                    });
                }
                phaser.arriveAndAwaitAdvance();
            }
        }

        // :NOTE: можно ли сразу не добавляться в visited сайты, которые упали с ошибкой?
        // :UPD: Поправила. Теперь в downloaded добавляю только после успешной загрузки
        return new Result(new ArrayList<>(visited), errs);
    }

    private void linkFromDoc(Document page, Map<String, IOException> errs, String url, Phaser phaser) {
        try {
            nextLayerLinks.addAll(page.extractLinks());
        } catch (IOException extractLinksFailure) {
            errs.put(url, extractLinksFailure);
        } finally {
            phaser.arrive();
        }
    }

    static void printErrors() {
        System.err.println("Invalid input arguments, follow this format:");
        System.err.println("WebCrawler url [depth [downloads [extractors [perHost]]]]");
    }

    static int[] validate(String[] args) {
        int[] arguments = new int[args.length];
        for (var i = 0; i < args.length; i++) {
            try {
                arguments[i] = Integer.parseInt(args[i]);
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("All arguments must be an integer value.");
            }
        }
        return arguments;
    }

    @Override
    public void close() {
        shutDownExec("downloaders", downloaders);
        shutDownExec("extractors", extractors);
    }

    private void shutDownExec(String name, ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
                System.err.println("Executor service '" + name + "' did not terminate in the specified time.");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        // :NOTE: в java не принято объявлять переменные в одну строчку, надо разделить на три строки.
        // :UPD: исправлено ниже
        if (args == null || args.length != 5) {
            printErrors();
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Args can't be null.");
            return;
        }
        int[] result = validate(args);
        int downloaders = result[2];
        int extractors = result[3];
        int perHost = result[4];

        try (WebCrawler crawler =
                     new WebCrawler(new CachingDownloader(0.0), downloaders, extractors, perHost)) {
            crawler.download(args[0], result[1]);
        } catch (IOException e) {
            System.err.println("Error occurred while downloading.");
        }
    }
}
