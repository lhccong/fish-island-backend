package com.cong.fishisland.thirdparty.music;

import com.cong.fishisland.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MusicUpstreamTest {
    @Test void recommendationsUseFreshFmResultsWithoutSearchOrExistingPlaylist() throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        MusicUpstream service = upstream(connection);
        String body = "{\"data\":{\"songs\":[{\"id\":\"new\",\"name\":\"推荐歌曲\",\"artist\":[{\"name\":\"歌手\"}],"
                + "\"url\":\"https://upstream.test/private?auth=synthetic\"},"
                + "{\"id\":\"new\",\"name\":\"重复歌曲\"}]}}";
        when(connection.getInputStream()).thenAnswer(call -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        for (String source : new String[]{"netease", "tencent"}) {
            JsonNode songs = service.recommendations(source);
            assertEquals(1, songs.size());
            assertEquals("new", songs.get(0).path("id").asText());
            assertEquals("歌手", songs.get(0).path("artist").asText());
            assertFalse(songs.toString().contains("url"));
            assertFalse(songs.toString().contains("synthetic"));
        }
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        verify(service, times(2)).openConnection(uri.capture());
        for (URI value : uri.getAllValues()) {
            assertTrue(value.getQuery().contains("type=fm"));
            assertFalse(value.getQuery().contains("search"));
            assertFalse(value.getQuery().contains("token"));
        }
    }

    @Test void recommendationsAreBoundedAndDoNotInventSongsOnEmptyOrInvalidResponses() throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        MusicUpstream service = upstream(connection);
        StringBuilder body = new StringBuilder("[");
        for (int index = 0; index < 25; index++) {
            if (index > 0) body.append(',');
            body.append("{\"id\":\"").append(index).append("\",\"name\":\"推荐歌曲\"}");
        }
        body.append(']');
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals(20, service.recommendations("netease").size());
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, service.recommendations("netease").size());
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("{\"error\":\"unsupported\"}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(BusinessException.class, () -> service.recommendations("netease"));
        assertThrows(BusinessException.class, () -> service.recommendations("unknown"));
    }

    private MusicUpstream upstream(HttpURLConnection connection) throws Exception {
        MusicProperties properties = new MusicProperties();
        properties.setApiUrl("https://upstream.example.test");
        properties.setToken("synthetic-test-token");
        MusicUpstream service = spy(new MusicUpstream(properties, new ObjectMapper()));
        doReturn(connection).when(service).openConnection(any(URI.class));
        when(connection.getResponseCode()).thenReturn(200);
        return service;
    }

    @Test void normalizesMetingRowsCachesSearchAndKeepsTokenInServerHeader() throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        MusicUpstream service = upstream(connection);
        String body = "[{\"id\":1,\"name\":\"合成\",\"artist\":[\"歌手甲\",{\"name\":\"歌手乙\"}],\"url\":\"https://upstream.test/api?auth=synthetic\"},"
                + "{\"title\":\"第二首\",\"author\":\"乙\",\"url\":\"http://127.0.0.1/api?type=url&id=second\"},"
                + "{\"title\":\"第三首\",\"author\":\"丙\",\"url\":\"third_mid\"}]";
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        JsonNode songs = service.search("netease", "合成", 1);
        assertEquals(3, songs.size()); assertEquals("1", songs.get(0).path("id").asText());
        assertEquals("歌手甲 / 歌手乙", songs.get(0).path("artist").asText());
        assertEquals("second", songs.get(1).path("id").asText());
        assertEquals("third_mid", songs.get(2).path("id").asText());
        assertFalse(songs.toString().contains("url")); assertFalse(songs.toString().contains("synthetic"));
        assertEquals(songs, service.search("netease", "合成", 1));
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        verify(service, times(1)).openConnection(uri.capture());
        assertFalse(uri.getValue().toString().contains("token"));
        verify(connection).setRequestProperty("Authorization", "Bearer synthetic-test-token");
        verify(connection).setInstanceFollowRedirects(false); verify(connection).disconnect();
    }

    @Test void doesNotExposeUpstreamAuthenticationErrorsOrFollowRedirects() throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        MusicUpstream service = upstream(connection);
        when(connection.getResponseCode()).thenReturn(401);
        BusinessException error = assertThrows(BusinessException.class, () -> service.search("netease", "合成", 1));
        assertTrue(error.getMessage().contains("密钥"));
        verify(connection, never()).getInputStream();
        reset(connection);
        when(connection.getResponseCode()).thenReturn(302);
        when(connection.getHeaderField("Location")).thenReturn("https://cdn.example.test/song.mp3");
        assertEquals("https://cdn.example.test/song.mp3", service.resolve("netease", "1"));
        verify(connection, never()).getInputStream();
    }

    @Test void rejectsMissingConfigParametersAndUnsafeMedia() {
        MusicUpstream service = new MusicUpstream(new MusicProperties(), new ObjectMapper());
        assertTrue(assertThrows(BusinessException.class, () -> service.search("netease", "合成", 1)).getMessage().contains("yml"));
        assertThrows(BusinessException.class, () -> service.search("arbitrary", "合成", 1));
        assertThrows(BusinessException.class, () -> service.search("netease", "合成", 6));
        for (String url : new String[]{"http://cdn.example.test/a", "https://127.0.0.1/a", "https://192.168.1.1/a",
                "https://upstream.test/api?type=url&id=1", "https://cdn.example.test/a?auth=synthetic", "https://music.163.com/song/media/outer/url?id=1"}) {
            assertThrows(BusinessException.class, () -> MusicUpstream.publicMediaUrl(url));
        }
    }

    @Test void rejectsEitherMissingAddressOrMissingTokenBeforeConnecting() throws Exception {
        MusicProperties properties = new MusicProperties();
        assertEquals("", properties.getApiUrl());
        MusicUpstream service = spy(new MusicUpstream(properties, new ObjectMapper()));
        properties.setToken("synthetic-test-token");
        assertTrue(assertThrows(BusinessException.class, () -> service.search("netease", "合成", 1))
                .getMessage().contains("尚未配置"));
        properties.setApiUrl("https://upstream.example.test");
        properties.setToken("");
        assertTrue(assertThrows(BusinessException.class, () -> service.search("netease", "合成", 1))
                .getMessage().contains("尚未配置"));
        verify(service, never()).openConnection(any(URI.class));
    }
}
