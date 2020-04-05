package com.tesco.substitutions.infrastructure.module;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class SubstitutionsBinder {
   public static void main(String[] args) throws IOException {

      Document doc = Jsoup.connect("https://dailyiptvm3u.com/listas-iptv-espana-movistar-m3u-actualiza/").get();
      log.info(doc.title());
      Elements m3uRows = doc.select("table.da-attachments-table tr.m3u");
      Elements links = m3uRows.select("td.attachment-title a");
      Elements dates = m3uRows.select("td.attachment-date");

      Map<String, String> datesToLinks = new HashMap<>();

      IntStream.range(0, links.size())
              .forEach(i -> datesToLinks.put(dates.get(i).text(), links.get(i).attr("href")));
      datesToLinks.forEach((k, v) -> log.info("{} -> {}", k, v ));

      String link2 = datesToLinks.get("April 5, 2020 4:36 pm");
      Set<String> linksToCheck = datesToLinks.keySet();

      linksToCheck.stream().filter(key -> key.startsWith("April 3")).forEach(key -> {
         log.info("===== Checking for date {} =======", key);
         String uri = datesToLinks.get(key);
         try {
            readListOnLink(uri);
         } catch (IOException e) {
           log.warn("Error connecting to {}", uri);
         }
      });
      readListOnLink(link2);
   }

   private static void readListOnLink(String link) throws IOException {
      log.info("Reading List on Link: {}", link);
      URL url = new URL(link);
      BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
      String line;
      Map<String, String> channelToUrl = new HashMap<>();
      while ((line = read.readLine()) != null) {
         if (line.startsWith("#EXTINF")) {
            String channelName = line.split(",")[1];
            String nextLine = read.readLine();
            if (nextLine != null) {
               if (nextLine.startsWith("http")) {
                  channelToUrl.put(channelName, nextLine);
               }
            }
         }
      }
      read.close();

      Map<String, String> cleanedChannels = channelToUrl.entrySet().stream().filter(entry -> {
         String channel =  entry.getKey();
         String channelUrl = entry.getValue();
         try {
            boolean isOnline =  isOnline(channelUrl);
            log.info("Checking {}", channel);
            log.info("[{}]: {} -> {}", isOnline ? "available" : "UNAVAILABLE", channel, channelUrl);
            return isOnline;
         } catch (IOException e) {
            log.warn("[{}]: {} -> {}" ,"unavailable", channel, channelUrl);
            return false;
         }
      }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      cleanedChannels.entrySet().stream().forEach(entry -> {
         log.info("[CLEANED] {} -> {}", entry.getKey(), entry.getValue());
      });
   }

   public static boolean isOnline(String url) throws IOException {
      URL urlChannel = new URL(url);
      HttpURLConnection connection = (HttpURLConnection)  urlChannel.openConnection();
      connection.setConnectTimeout(1000);
      connection.setReadTimeout(1000);
      connection.setRequestMethod("GET");
      connection.connect();
      int statusCode = connection.getResponseCode();
      String contentType = connection.getContentType();
      log.info("{} -> Response code: {}, Content Type: {}", url, statusCode, contentType);
      return  statusCode == 200;
   }
}

