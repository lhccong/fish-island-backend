package com.cong.fishisland.service;

import com.cong.fishisland.model.vo.IpLocationVO;
import com.cong.fishisland.service.impl.chat.ChatSenderLocationResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatSenderLocationResolverTest {
    @Test void coldLookupReturnsBeforeWorkAndCoalescesUntilCached() {
        IpLocationService locations = mock(IpLocationService.class);
        List<Runnable> queued = new ArrayList<>();
        ChatSenderLocationResolver resolver = new ChatSenderLocationResolver(locations, queued::add);
        IpLocationVO result = new IpLocationVO();
        result.setRegionName("合成地区");
        when(locations.getLocationByIp("192.0.2.1")).thenReturn(result);
        assertNull(resolver.cachedOrSchedule("192.0.2.1"));
        assertNull(resolver.cachedOrSchedule(" 192.0.2.1 "));
        verifyNoInteractions(locations);
        assertEquals(1, queued.size());
        queued.remove(0).run();
        assertSame(result, resolver.cachedOrSchedule("192.0.2.1"));
        assertTrue(queued.isEmpty());
        verify(locations).getLocationByIp("192.0.2.1");
    }

    @Test void skipsPrivateAndInvalidAddressesWithoutDnsOrTasks() {
        IpLocationService locations = mock(IpLocationService.class);
        List<Runnable> queued = new ArrayList<>();
        ChatSenderLocationResolver resolver = new ChatSenderLocationResolver(locations, queued::add);
        for (String ip : new String[]{null, "", "localhost", "127.0.0.1", "10.0.1.1", "172.16.1.1",
                "192.168.1.1", "169.254.1.1", "0.0.0.0", "::", "::1", "fd00::1", "fc00::1",
                "fe80::1", "::ffff:127.0.0.1", "224.0.0.1", "999.1.1.1", "8.8.8.8/path"}) {
            assertNull(resolver.cachedOrSchedule(ip), ip);
        }
        assertTrue(queued.isEmpty());
        verifyNoInteractions(locations);
    }

    @Test void failuresAndEmptyResultsDoNotScheduleOnEveryMessage() {
        IpLocationService locations = mock(IpLocationService.class);
        List<Runnable> queued = new ArrayList<>();
        ChatSenderLocationResolver resolver = new ChatSenderLocationResolver(locations, queued::add);
        when(locations.getLocationByIp("192.0.2.1")).thenThrow(new IllegalStateException("synthetic"));
        for (String ip : new String[]{"192.0.2.1", "192.0.2.2"}) {
            assertNull(resolver.cachedOrSchedule(ip));
            queued.remove(0).run();
            assertNull(resolver.cachedOrSchedule(ip));
            assertTrue(queued.isEmpty());
            verify(locations).getLocationByIp(ip);
        }
    }

    @Test void saturationDoesNotExecuteOnTheRequestThreadOrThrow() {
        IpLocationService locations = mock(IpLocationService.class);
        ChatSenderLocationResolver resolver = new ChatSenderLocationResolver(locations, command -> {
            throw new RejectedExecutionException("synthetic saturation");
        });
        assertNull(resolver.cachedOrSchedule("192.0.2.1"));
        assertNull(resolver.cachedOrSchedule("192.0.2.1"));
        verifyNoInteractions(locations);
    }
}
