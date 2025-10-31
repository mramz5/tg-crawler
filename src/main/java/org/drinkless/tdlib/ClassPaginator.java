package org.drinkless.tdlib;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class ChatPaginator {

    private final Client client;
    private final long chatId;
    private final int pageSize;
    private long fromMessageId = 0;


    public ChatPaginator(Client client, long chatId, int pageSize) {
        this.client = client;
        this.chatId = chatId;
        this.pageSize = pageSize;
    }

    public CompletableFuture<TdApi.FoundChatMessages> getNextPage(String keyword) {
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

                fromMessageId = messages.messages[messages.messages.length - 1].id;

                List<MessageDTO> lastResult = Arrays.stream(messages.messages)
                        .map(MessageMapper::toDTO)
                        .toList().subList(0, messages.messages.length - 1);

                System.out.println(lastResult);

                future.complete(messages);
            } else {
                future.completeExceptionally(new RuntimeException("Error: " + result));
            }
        });
        return future;
    }
}