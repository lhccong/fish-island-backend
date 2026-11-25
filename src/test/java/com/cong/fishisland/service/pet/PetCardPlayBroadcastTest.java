package com.cong.fishisland.service.pet;

import com.cong.fishisland.mapper.PetCardPlayMapper.Player;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Command;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Owned;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Stock;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.ws.response.BroadcastResponse;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.admin.BroadcastService;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import com.cong.fishisland.websocket.service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PetCardPlayBroadcastTest {
    private final PetCardPlayStore store = mock(PetCardPlayStore.class);
    private final PetCardCatalogStore catalog = mock(PetCardCatalogStore.class);
    private final WebSocketService socket = mock(WebSocketService.class);
    private final TestTransactions transactions = new TestTransactions();
    private final ObjectMapper json = new ObjectMapper();
    private final Pool pool = new Pool();
    private PetCardPlayService service;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUserName("测试岛民");
        UserService users = mock(UserService.class);
        when(users.getById(1L)).thenReturn(user);
        AchievementBroadcastService broadcasts = new AchievementBroadcastService(new BroadcastService(socket), users);
        service = new PetCardPlayService(store, catalog, mock(UserPointsService.class), transactions, json, broadcasts);

        pool.setId("island");
        pool.setName("海岛初遇");
        pool.setWeights(Collections.singletonMap("common", 1));
        when(catalog.pool("island")).thenReturn(pool);
        when(catalog.drawable(pool)).thenReturn(true);
        when(catalog.hasShinyCard("island")).thenReturn(true);
        when(catalog.poolCards("island")).thenReturn(Arrays.asList(
                card("common", "普通卡"), card("shiny", "极光\n灵狐")));
        when(store.player(1L)).thenReturn(new Player(null, 0, 0, null));
        when(store.stock(1L, "island")).thenReturn(new Stock("island", 2, 0));
    }

    @Test
    void guaranteedShinyIsBroadcastAfterCommitAndReceiptReplayDoesNotBroadcastAgain() {
        Command command = opening("open");
        service.submit(1L, command);
        when(store.receipt(1L, command.getRequestId())).thenReturn(json.valueToTree(command));
        service.submit(1L, command);

        assertBroadcast("恭喜「测试岛民」抽到了闪耀卡「极光 灵狐」！");
        verify(store, times(5)).collect(eq(1L), anyString(), anyBoolean());
    }

    @Test
    void batchBroadcastsOneTotalIncludingAlreadyOwnedShinyCards() {
        pool.setWeights(Collections.singletonMap("shiny", 1));
        when(store.collection(1L)).thenReturn(Collections.singletonList(new Owned("shiny", 1, 0, false, 1)));
        Command command = opening("openBatch");
        command.setQuantity(2);

        assertEquals(10, service.submit(1L, command).getBatch().getDuplicateCards());

        assertBroadcast("恭喜「测试岛民」抽到了 10 张闪耀卡！");
        verify(store, times(10)).collect(1L, "shiny", true);
    }

    @Test
    void openingWithoutShinyDoesNotBroadcast() {
        when(store.stock(1L, "island")).thenReturn(new Stock("island", 2, 1));
        service.submit(1L, opening("open"));
        assertTrue(transactions.committed);
        verifyNoInteractions(socket);
    }

    @Test
    void rollbackDiscardsBroadcastAndDispatchFailureDoesNotUndoCommit() {
        Command command = opening("open");
        doThrow(new IllegalStateException("synthetic receipt failure")).doNothing()
                .when(store).receipt(eq(1L), eq(command.getRequestId()), any());
        assertThrows(IllegalStateException.class, () -> service.submit(1L, command));
        assertFalse(transactions.committed);
        verifyNoInteractions(socket);

        doThrow(new IllegalStateException("synthetic dispatch failure")).when(socket).sendToAllOnline(any());
        assertNotNull(service.submit(1L, command));
        assertBroadcast("恭喜「测试岛民」抽到了闪耀卡「极光 灵狐」！");
    }

    private void assertBroadcast(String content) {
        ArgumentCaptor<WSBaseResp> sent = ArgumentCaptor.forClass(WSBaseResp.class);
        verify(socket).sendToAllOnline(sent.capture());
        WSBaseResp<?> frame = sent.getValue();
        assertEquals("broadcast", frame.getType());
        assertEquals(content, ((BroadcastResponse) frame.getData()).getContent());
        assertTrue(transactions.committed);
    }

    private static Card card(String grade, String name) {
        Card card = new Card();
        card.setId(grade);
        card.setGrade(grade);
        card.setName(name);
        return card;
    }

    private static Command opening(String type) {
        Command command = new Command();
        command.setType(type);
        command.setPoolId("island");
        command.setRequestId(UUID.randomUUID().toString());
        return command;
    }

    private class TestTransactions extends AbstractPlatformTransactionManager {
        private boolean committed;

        @Override protected Object doGetTransaction() { return new Object(); }

        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override protected void doCommit(DefaultTransactionStatus status) {
            if (!committed) verifyNoInteractions(socket);
            committed = true;
        }

        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }
}
