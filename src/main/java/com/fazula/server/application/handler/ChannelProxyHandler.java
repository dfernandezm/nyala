package com.fazula.server.application.handler;


import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Singleton
public class ChannelProxyHandler {

    public void proxy(final RoutingContext routingContext) {
        String url = "https://e10.habrox.xyz/ingestnb4s/espn3_sur/f.m3u8";
        String proxyUrlOutput = proxyUrl(url);
        routingContext
                .response()
                .putHeader("Content-Type", "application/vnd.apple.mpegurl")
                .end(proxyUrlOutput);
    }

    public static String proxyUrl(String url) {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }

        try {
            URL urlChannel = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)  urlChannel.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", "https://espn-live.stream/stream/54015.html");
            connection.setRequestProperty("Origin", "https://espn-live.stream/stream/54015.html");
            connection.setRequestProperty("User-Agent", " Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
            connection.connect();
            int statusCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String output = IOUtils.toString(br);
            log.info("{} -> Response code: {}, Content Type: {}", url, statusCode, contentType);
            log.info("Response content: {}", output);
            return output;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
