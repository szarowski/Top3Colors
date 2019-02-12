package com.colors;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static java.util.stream.Collectors.toList;

public class Top3Colors {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        var startTime = LocalDateTime.now();
        if (args.length > 3) {
            System.err.format("Incorrect number of arguments, exiting!\n");
            return;
        }
        if (args.length == 0) {
            System.out.println("Top Three Colors Finder:");
            System.out.println("------------------------");
            System.out.println("The first parameter is an input file name - mandatory");
            System.out.println("The second parameter is an output file name - mandatory");
            System.out.println("The third parameter is an explicit concurrency level - optional");
            return;
        }
        var inputFile = args[0];
        var outputFile = args[1];
        // estimate number of cores based on Xmx used (2 << 28 corresponds to 512 MB for a single thread execution)
        int cores = (int) (Runtime.getRuntime().maxMemory() / (2 << 28));
        int availableCores = Runtime.getRuntime().availableProcessors();
        cores = availableCores < cores ? availableCores : cores > 0 ? cores : 1;
        try {
            Files.deleteIfExists(Path.of(outputFile));
            Files.createFile(Path.of(outputFile));
            if (args.length == 3) {
                // explicitly set a number of cores as a 2nd parameter
                cores = Integer.parseInt(args[2]);
            }
        } catch (IOException ioe) {
            System.err.format("Cannot write to output file name: %s, exiting!\n", outputFile);
            return;
        } catch (NumberFormatException nfe) {
            System.err.format("Concurrency level should be a number: %s, exiting!\n", args[1]);
            return;
        }
        var forkJoinPool = new ForkJoinPool(cores);
        try {
            forkJoinPool.submit(() -> {
                try {
                    Files.lines(Paths.get(inputFile))
                            .map(line -> readImageAsync(inputFile, line))
                            .map(Top3Colors::findTop3Colors)
                            .parallel()
                            .forEach(sie -> {
                                try {
                                    Files.writeString(Paths.get(outputFile), String.format("%s,#%s,#%s,#%s\n", sie.getKey(),
                                            Integer.toHexString(sie.getValue().get(0)).toUpperCase(),
                                            Integer.toHexString(sie.getValue().get(1)).toUpperCase(),
                                            Integer.toHexString(sie.getValue().get(2)).toUpperCase()),
                                            StandardOpenOption.APPEND);
                                } catch (IOException ioe) {
                                    System.err.format("Cannot write to output file name: %s, exiting!\n", outputFile);
                                }
                                sie.getValue().clear();
                            });
                    System.out.format("Finished processing URLs in %d seconds.\n",
                            ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()));
                } catch (IOException ioe) {
                    System.err.format("Invalid input file name: %s, exiting!\n", inputFile);
                } catch (Throwable t) {
                    System.err.print(t.getMessage());
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.format("Processing of images has been interrupted for the file name: %s, exiting!\n", inputFile);
        } finally {
            forkJoinPool.shutdown();
        }
    }

    private static Map.Entry<String, BufferedImage> readImageAsync(String inputFile, String line) {
        BufferedImage bufferedImage;
        try {
            var request = HttpRequest.newBuilder().uri(URI.create(line)).build();
            bufferedImage = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(HttpResponse::body)
                    .thenApply(is -> {
                        try {
                            var bi = ImageIO.read(is);
                            is.close();
                            return bi;
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    })
                    .get();
        } catch (IllegalArgumentException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(String.format("Cannot process a URL: %s in the input file: %s, exiting!\n",
                    line, inputFile));
        }
        if (bufferedImage == null) {
            throw new RuntimeException(String.format("Cannot process a URL: %s in the input file: %s. Insufficient memory? Exiting!\n",
                    line, inputFile));
        }
        return new AbstractMap.SimpleImmutableEntry<>(line, bufferedImage);
    }

    private static Map.Entry<String, List<Integer>> findTop3Colors(Map.Entry<String, BufferedImage> sie) {
        var colorMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < sie.getValue().getWidth(); i++) {
            for (int j = 0; j < sie.getValue().getHeight(); j++) {
                colorMap.merge(sie.getValue().getRGB(i, j), 1, Integer::sum);
            }
        }
        var top3Colors = colorMap.entrySet().stream().sorted(
                Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(3)
                .map(Map.Entry::getKey).collect(toList());
        sie.getValue().flush();
        colorMap.clear();
        return new AbstractMap.SimpleImmutableEntry<>(sie.getKey(), top3Colors);
    }
}