package com.gzp.m3u8;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U2Text {

    public static void main(String[] args) {
//        String m3u8Url = "https://www.goodiptv.club/bililive.m3u";
//        String m3u8Url = "https://mirror.ghproxy.com/https://raw.githubusercontent.com/Ftindy/IPTV-URL/main/cqyx.m3u";
        String m3u8Url = "https://mirror.ghproxy.com/https://raw.githubusercontent.com/Ftindy/IPTV-URL/main/IPTV.m3u";
        StringBuilder builder = new StringBuilder();
        try {
            Map<String, StringBuilder> categorizedLinks = parseAndCategorizeM3U8(m3u8Url);

            // Validate URLs concurrently
            validateUrlsConcurrently(categorizedLinks);

            // Print the results
            for (Map.Entry<String, StringBuilder> entry : categorizedLinks.entrySet()) {
                String str = entry.getKey() + entry.getValue().toString();
                builder.append(str).append("\n");
                System.out.println(str);
            }
            String name = m3u8Url.substring(m3u8Url.lastIndexOf("/"), m3u8Url.lastIndexOf("."));
            FileWriter writer = new FileWriter(new File("C:\\Users\\Administrator\\Desktop", name + ".txt"));
            writer.write(builder.toString());
            writer.flush();
            writer.close();
        } catch (IOException  e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, StringBuilder> parseAndCategorizeM3U8(String m3u8Url) throws IOException {
        Map<String, StringBuilder> categorizedLinks = new HashMap<>();

        HttpURLConnection connection = (HttpURLConnection) new URL(m3u8Url).openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            String tvgName = null;
            String groupTitle = null;
            String url = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#EXTINF")) {
                    // Extract tvg-name from the EXTINF line
//                    tvgName = extractValue(line, "tvg-name");
                    tvgName = extractTvgName(line);

                    // Extract group-title from the EXTINF line
                    groupTitle = extractValue(line, "group-title");
                } else if (!line.startsWith("#")) {
                    // If it's not a comment line, it's assumed to be the URL
                    url = line;
                    // Categorize the link based on group-title
                    if (groupTitle != null && url != null) {
                        // If the group-title is not in the map, add it with an empty StringBuilder
                        categorizedLinks.putIfAbsent(groupTitle, new StringBuilder(",#genre#\n"));

                        // Append the tvg-name and URL to the group-title's StringBuilder
                        categorizedLinks.get(groupTitle).append(tvgName).append(",").append(url).append("\n");

                        // Reset tvgName and groupTitle for the next entry
                        tvgName = null;
                        groupTitle = null;
                    }
                }
            }
        } finally {
            connection.disconnect();
        }

        return categorizedLinks;
    }

    private static String extractTvgName(String line) {
        String pattern = "tvg-name=\"(.*?)\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(line);

        if (m.find()) {
            return m.group(1);
        } else {
            return line.substring(line.lastIndexOf(",") + 1).trim();
        }
    }

    private static String extractValue(String line, String key) {
        String pattern = key + "=\"(.*?)\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(line);

        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }

    private static void validateUrlsConcurrently(Map<String, StringBuilder> categorizedLinks) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5); // Adjust the pool size as needed
        CountDownLatch latch = new CountDownLatch(categorizedLinks.size());
        Set<String> keysToRemove = new HashSet<>();

        for (Map.Entry<String, StringBuilder> entry : categorizedLinks.entrySet()) {
            String groupTitle = entry.getKey();
            StringBuilder urlStringBuilder = entry.getValue();

            executorService.execute(() -> {
                // Split the StringBuilder into lines
                String[] lines = urlStringBuilder.toString().split("\n");

                // Validate each URL
                boolean groupValid = true;
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        String tvgName = parts[0];
                        String url = parts[1];

                        try {
                            boolean isValid = validateUrl(url);
                            if (isValid) {
                                groupValid = false;
                                System.out.println("URL is not valid: " + url + "\tName: " + tvgName);
                            }
                        } catch (IOException e) {
//                            System.out.println("Error validating URL: " + url);
//                            e.printStackTrace();
                        }
                    }
                }

                // Add the group to keysToRemove if any URL is not valid
                if (!groupValid) {
                    keysToRemove.add(groupTitle);
                }
                System.out.println("latch = " + latch.getCount());
                latch.countDown();
            });
        }
        // Wait for all threads to finish
        latch.await();

        // Remove the invalid groups
        for (String keyToRemove : keysToRemove) {
            categorizedLinks.remove(keyToRemove);
        }
        // Shutdown the executorService when all tasks are complete
        executorService.shutdown();
    }

    private static boolean validateUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(5000); // Set connection timeout to 5 seconds
        connection.setReadTimeout(5000);    // Set read timeout to 5 seconds

        int responseCode = connection.getResponseCode();
        connection.disconnect();

        System.out.println("responseCode = " + responseCode);
        return responseCode == 400 || responseCode == 404|| responseCode == 403||responseCode == 503||responseCode == 502|| responseCode == -1;
    }
}
