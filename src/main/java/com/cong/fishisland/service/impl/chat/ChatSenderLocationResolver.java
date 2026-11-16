package com.cong.fishisland.service.impl.chat;

import com.cong.fishisland.model.vo.IpLocationVO;
import com.cong.fishisland.service.IpLocationService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/** Optional location metadata never holds up persistence or acknowledgement. */
@Component
public class ChatSenderLocationResolver {
    private final IpLocationService locations;
    private final Executor executor;
    private final Cache<String, IpLocationVO> cache = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).build();
    private final Cache<String, Boolean> failures = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(1, TimeUnit.MINUTES).build();
    private final Set<String> pending = ConcurrentHashMap.newKeySet();

    public ChatSenderLocationResolver(IpLocationService locations,
            @Qualifier("chatMetadataExecutor") Executor executor) {
        this.locations = locations;
        this.executor = executor;
    }

    public IpLocationVO cachedOrSchedule(String address) {
        String ip = publicLiteral(address);
        if (ip == null) return null;
        IpLocationVO cached = cache.getIfPresent(ip);
        if (cached != null) return cached;
        if (failures.getIfPresent(ip) != null || !pending.add(ip)) return null;
        try {
            executor.execute(() -> {
                try {
                    IpLocationVO result = locations.getLocationByIp(ip);
                    if (result != null) cache.put(ip, result);
                    else failures.put(ip, true);
                } catch (RuntimeException ignored) {
                    failures.put(ip, true);
                } finally {
                    pending.remove(ip);
                }
            });
        } catch (RejectedExecutionException ignored) {
            failures.put(ip, true);
            pending.remove(ip);
        }
        return null;
    }

    private String publicLiteral(String address) {
        if (address == null) return null;
        String literal = address.trim();
        // Restrict to numeric literals before InetAddress, so this cannot perform DNS.
        if (literal.contains(":")) {
            if (!literal.matches("[0-9a-fA-F:.]{2,45}")) return null;
        } else {
            if (!literal.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return null;
            for (String part : literal.split("\\.")) if (Integer.parseInt(part) > 255) return null;
        }
        try {
            InetAddress ip = InetAddress.getByName(literal);
            byte[] bytes = ip.getAddress();
            if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isSiteLocalAddress()
                    || ip.isLinkLocalAddress() || ip.isMulticastAddress()
                    || (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc)) return null;
            return ip.getHostAddress();
        } catch (UnknownHostException ignored) {
            return null;
        }
    }
}
