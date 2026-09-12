package com.emre.nexusai;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

final class NetworkStack {
    private static final OkHttpClient BOOTSTRAP = new OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private static final Dns RESILIENT_DNS = buildDns();

    private NetworkStack() { }

    static OkHttpClient client(int connectTimeoutMs, int readTimeoutMs) {
        int connect = Math.max(1_000, connectTimeoutMs);
        int read = Math.max(1_000, readTimeoutMs);
        return new OkHttpClient.Builder()
                .dns(RESILIENT_DNS)
                .connectTimeout(connect, TimeUnit.MILLISECONDS)
                .readTimeout(read, TimeUnit.MILLISECONDS)
                .writeTimeout(Math.max(connect, 10_000), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    private static Dns buildDns() {
        List<Dns> resolvers = new ArrayList<>();

        // Fast path: use Android/carrier DNS when it works. ResilientDns filters
        // IPv6 results so broken mobile IPv6 routes do not stall requests.
        resolvers.add(Dns.SYSTEM);

        try {
            DnsOverHttps cloudflare = new DnsOverHttps.Builder()
                    .client(BOOTSTRAP)
                    .url(HttpUrl.get("https://cloudflare-dns.com/dns-query"))
                    .bootstrapDnsHosts(
                            InetAddress.getByName("1.1.1.1"),
                            InetAddress.getByName("1.0.0.1"))
                    .includeIPv6(false)
                    .build();
            resolvers.add(cloudflare);
        } catch (Exception ignored) {
            // Keep the chain alive; Google/system can still resolve.
        }

        try {
            DnsOverHttps google = new DnsOverHttps.Builder()
                    .client(BOOTSTRAP)
                    .url(HttpUrl.get("https://dns.google/dns-query"))
                    .bootstrapDnsHosts(
                            InetAddress.getByName("8.8.8.8"),
                            InetAddress.getByName("8.8.4.4"))
                    .includeIPv6(false)
                    .build();
            resolvers.add(google);
        } catch (Exception ignored) {
            // System/Cloudflare remain available.
        }

        return new ResilientDns(resolvers);
    }
}
