package com.emre.nexusai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Dns;
import org.junit.Test;

public class ResilientDnsTest {
    @Test public void fallsBackWhenPrimaryDnsFails() throws Exception {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();

        Dns primary = hostname -> {
            primaryCalls.incrementAndGet();
            throw new UnknownHostException("carrier dns failed");
        };
        Dns fallback = hostname -> {
            fallbackCalls.incrementAndGet();
            return Collections.singletonList(InetAddress.getByName("93.184.216.34"));
        };

        ResilientDns dns = new ResilientDns(Arrays.asList(primary, fallback));
        List<InetAddress> result = dns.lookup("example.com");

        assertEquals(1, primaryCalls.get());
        assertEquals(1, fallbackCalls.get());
        assertEquals("93.184.216.34", result.get(0).getHostAddress());
    }

    @Test public void prefersIpv4AndDropsIpv6ForMobileCompatibility() throws Exception {
        Dns source = hostname -> Arrays.asList(
                InetAddress.getByName("2001:4860:4860::8888"),
                InetAddress.getByName("8.8.8.8"));

        ResilientDns dns = new ResilientDns(Collections.singletonList(source));
        List<InetAddress> result = dns.lookup("dns.google");

        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof Inet4Address);
    }

    @Test(expected = UnknownHostException.class)
    public void throwsOnlyAfterEveryResolverFails() throws Exception {
        Dns first = hostname -> { throw new UnknownHostException("one"); };
        Dns second = hostname -> { throw new UnknownHostException("two"); };
        ResilientDns dns = new ResilientDns(Arrays.asList(first, second));
        dns.lookup("missing.example");
    }
}
