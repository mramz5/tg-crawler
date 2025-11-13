package org.drinkless.tdlib;

import org.drinkless.tdlib.dto.MessageDTO;
import org.drinkless.tdlib.mapper.MessageMapper;
import org.drinkless.tdlib.ui.TerminalWindow;
import org.drinkless.tdlib.util.ObservableInteger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.drinkless.tdlib.ui.TerminalWindow.formattedOutputLine;

public class ChatPaginator {

    private final Client client;
    private final long chatId;
    private final int pageSize;
    private long fromMessageId = 0;
    private final ObservableInteger allMessageCount;

    public ChatPaginator(Client client, long chatId, int pageSize, ObservableInteger allMessageCount) {
        this.client = client;
        this.chatId = chatId;
        this.pageSize = pageSize;
        this.allMessageCount = allMessageCount;
    }

    public CompletableFuture<TdApi.FoundChatMessages> getNextPage(String keyword, TerminalWindow terminalWindow, String channelName) {
        TdApi.SearchChatMessages query = new TdApi.SearchChatMessages(
                chatId,
                null,
                keyword,
                null,
                fromMessageId,
                0,
                pageSize + 1,
                null
        );

        CompletableFuture<TdApi.FoundChatMessages> future = new CompletableFuture<>();

        client.send(query, result -> {
            if (result.getConstructor() == TdApi.FoundChatMessages.CONSTRUCTOR) {
                TdApi.FoundChatMessages messages = (TdApi.FoundChatMessages) result;

//                allMessageCount.onEvent(messages.messages.length,terminalWindow);

                fromMessageId = messages.messages[messages.messages.length - 1].id;

                List<MessageDTO> lastResult = Arrays.stream(messages.messages)
                        .map(m -> MessageMapper.toDTO(m, channelName))
                        .toList().subList(0, messages.messages.length - 1);

                for (MessageDTO messageDTO : lastResult) {
                    terminalWindow.appendLine(formattedOutputLine(messageDTO.firstPartToString()));
                    terminalWindow.appendLinkButtonLB(messageDTO.messageLink);
                }

                future.complete(messages);
            } else {
                future.completeExceptionally(new RuntimeException("Error: " + result));
            }
        });
        return future;
    }
}