package com.emre.nexusai;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Dns;

/**
 * DNS chain for carrier/mobile-network compatibility.
 * Tries each resolver in order and returns IPv4 only to avoid broken IPv6 paths
 * observed on some mobile networks.
 */
public final class ResilientDns implements Dns {
    private final List<Dns> resolvers;

    public ResilientDns(List<Dns> resolvers) {
        if (resolvers == null || resolvers.isEmpty()) {
            throw new IllegalArgumentException("En az bir DNS resolver gerekli");
        }
        this.resolvers = Collections.unmodifiableList(new ArrayList<>(resolvers));
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new UnknownHostException("Boş hostname");
        }

        UnknownHostException last = null;
        for (Dns resolver : resolvers) {
            if (resolver == null) continue;
            try {
                List<InetAddress> addresses = resolver.lookup(hostname);
                List<InetAddress> ipv4 = new ArrayList<>();
                if (addresses != null) {
                    for (InetAddress address : addresses) {
                        if (address instanceof Inet4Address) ipv4.add(address);
                    }
                }
                if (!ipv4.isEmpty()) return ipv4;
                last = new UnknownHostException(hostname + ": resolver IPv4 döndürmedi");
            } catch (UnknownHostException ex) {
                last = ex;
            } catch (RuntimeException ex) {
                UnknownHostException wrapped = new UnknownHostException(hostname + ": " + ex.getMessage());
                wrapped.initCause(ex);
                last = wrapped;
            }
        }

        if (last != null) throw last;
        throw new UnknownHostException(hostname);
    }
}
