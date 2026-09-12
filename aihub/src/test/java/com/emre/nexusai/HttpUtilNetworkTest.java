package com.emre.nexusai;

import static org.junit.Assert.assertTrue;

import okhttp3.OkHttpClient;
import org.junit.Test;

public class HttpUtilNetworkTest {
    @Test public void httpClientUsesResilientDns() {
        OkHttpClient client = HttpUtil.clientForTests(1_000, 2_000);
        assertTrue(client.dns() instanceof ResilientDns);
    }

    @Test public void httpClientRetriesConnectionFailures() {
        OkHttpClient client = HttpUtil.clientForTests(1_000, 2_000);
        assertTrue(client.retryOnConnectionFailure());
    }
}
