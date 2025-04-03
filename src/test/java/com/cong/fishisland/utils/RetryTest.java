package com.cong.fishisland.utils;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.datasource.hostpost.ZhiBo8DataSource;
import com.cong.fishisland.model.entity.hot.HotPost;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryTest {

    @Mock
    private RetryTemplate retryTemplate;

    @InjectMocks
    private ZhiBo8DataSource zhiBo8DataSource;

    private MockedStatic<Jsoup> mockedJsoup;

    @BeforeEach
    void setUp() {
        retryTemplate = new RetryTemplate();
        zhiBo8DataSource = new ZhiBo8DataSource(retryTemplate);

        // 统一在 @BeforeEach 里 Mock Jsoup
        mockedJsoup = Mockito.mockStatic(Jsoup.class);
    }

    @AfterEach
    void tearDown() {
        // 确保每次测试后释放静态 Mock，防止污染下一个测试
        if (mockedJsoup != null) {
            mockedJsoup.close();
        }
    }

    @Test
    void testFetchHotPost_WithRetries() throws Exception {
        // 模拟 Jsoup 连接和 Document
        Connection mockConnection = mock(Connection.class);
        Document mockDocument = mock(Document.class);

        // 让 Jsoup.connect() 返回 mockConnection
        mockedJsoup.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
        when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
        when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);

        // 第一次和第二次调用 get() 抛出 IOException，第三次返回正常数据
        when(mockConnection.get())
                .thenThrow(new IOException("网络异常-1"))
                .thenThrow(new IOException("网络异常-2"))
                .thenReturn(mockDocument);

        // Mock 解析逻辑，返回空数据列表
        when(mockDocument.select(anyString())).thenReturn(new org.jsoup.select.Elements());

        // 执行测试
        HotPost hotPost = zhiBo8DataSource.getHotPost();

        // 断言：重试后应该成功获取数据
        assertNotNull(hotPost);
        assertEquals("直播吧体育热榜", hotPost.getName());

        // 验证 Jsoup.connect() 调用了三次
        verify(mockConnection, times(3)).get();
    }

    @Test
    void testFetchHotPost_AllRetriesFail() throws Exception {
        // 模拟 Jsoup 连接
        Connection mockConnection = mock(Connection.class);
        mockedJsoup.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
        when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
        when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);

        // 模拟 Jsoup 失败三次
        when(mockConnection.get()).thenThrow(new IOException("网络异常-1"))
                .thenThrow(new IOException("网络异常-2"))
                .thenThrow(new IOException("网络异常-3"));

        // 执行测试，并断言抛出 BusinessException
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            zhiBo8DataSource.getHotPost();
        });

        assertEquals("获取数据失败", exception.getMessage());

        // 确保 Jsoup.connect().get() 调用了三次
        verify(mockConnection, times(3)).get();
    }
}

