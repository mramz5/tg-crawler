package org.drinkless.tdlib;

import javax.swing.*;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.drinkless.tdlib.TerminalApp.*;

public class ChatUtil {

    static void getMessagesByKeywordsInChannelTitle(JTextPane console,
                                                    Client client,
                                                    String channelName,
                                                    String[] keywords,
                                                    TPage page,
                                                    Client.ResultHandler defaultHandler) {
        client.send(new TdApi.SearchPublicChat(channelName), object -> {
            if (TdApi.Error.CONSTRUCTOR == object.getConstructor()) {
                appendLine(console, formattedOutputLine("channel '" + channelName + "' not found"));
                sendMessage(getChatId(channelName), "channel '" + channelName + "' not found", client, defaultHandler);
                return;
            }
            TdApi.Chat chat = (TdApi.Chat) object;
            TdApi.ChatType chatType = chat.type;
            if (!(chatType.getConstructor() == TdApi.ChatTypeSupergroup.CONSTRUCTOR) ||
                    !((TdApi.ChatTypeSupergroup) chatType).isChannel) {
                appendLine(console, formattedOutputLine("'" + channelName + "'is not a channel"));
                sendMessage(getChatId(channelName), "'" + channelName + "'is not a channel", client, defaultHandler);
                return;
            }
            getMessagesByKeywords(console,client, chat.id, keywords, page,channelName);
        });
    }

    private static void getMessagesByKeywords(JTextPane console,
                                              Client client,
                                              long chatId,
                                              String[] keywords,
                                              TPage page,
                                              String channelName) {

        ChatPaginator paginator = new ChatPaginator(client, chatId, page.pageSize);
        CompletableFuture<TdApi.FoundChatMessages> chain = CompletableFuture.completedFuture(null);
        for (String keyword : keywords)
            for (int i = 0; i < page.numberOfPages; i++)
                chain = chain.thenCompose(v -> {
                    System.out.println("console.getComponentCount() : " + console.getComponentCount());
                    return paginator.getNextPage(keyword, console, channelName);
                });
    }


    static void sendMessage(long chatId,
                            String message,
                            Client client,
                            Client.ResultHandler defaultHandler) {
        // initialize reply markup just for testing
        TdApi.InlineKeyboardButton[] row = {
                new TdApi.InlineKeyboardButton("https://telegram.org?1", new TdApi.InlineKeyboardButtonTypeUrl()),
                new TdApi.InlineKeyboardButton("https://telegram.org?2", new TdApi.InlineKeyboardButtonTypeUrl()),
                new TdApi.InlineKeyboardButton("https://telegram.org?3", new TdApi.InlineKeyboardButtonTypeUrl())
        };
        TdApi.ReplyMarkup replyMarkup = new TdApi.ReplyMarkupInlineKeyboard(new TdApi.InlineKeyboardButton[][]{row, row, row});

        TdApi.InputMessageContent content = new TdApi.InputMessageText(new TdApi.FormattedText(message, null), null, true);
        client.send(new TdApi.SendMessage(chatId, 0, null, null, replyMarkup, content), defaultHandler);
    }

    static void getMainChatList(JTextPane console,
                                final int limit,
                                Client client,
                                NavigableSet<Crawler.OrderedChat> mainChatList,
                                boolean haveFullMainChatList,
                                String newLine,
                                ConcurrentMap<Long, TdApi.Chat> chats,
                                Client.ResultHandler defaultHandler) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(haveFullMainChatList);
        synchronized (mainChatList) {
            if (!atomicBoolean.get() && limit > mainChatList.size()) {
                // send LoadChats request if there are some unknown chats and have not enough known chats
                client.send(new TdApi.LoadChats(new TdApi.ChatListMain(), limit - mainChatList.size()), object -> {
                    switch (object.getConstructor()) {
                        case TdApi.Error.CONSTRUCTOR:
                            if (((TdApi.Error) object).code == 404) {
                                synchronized (mainChatList) {
                                    atomicBoolean.set(true);
                                }
                            } else {
                                appendLine(console,formattedErrorLine("Receive an error for LoadChats:" + newLine + object));
//                                System.err.println("Receive an error for LoadChats:" + newLine + object);
                            }
                            break;
                        case TdApi.Ok.CONSTRUCTOR:
                            // chats had already been received through updates, let's retry request
                            getMainChatList(console, limit, client, mainChatList, haveFullMainChatList, newLine, chats, defaultHandler);
                            break;
                        default:
                            appendLine(console, formattedErrorLine("Receive wrong response from TDLib:" + newLine + object));
//                            System.err.println("Receive wrong response from TDLib:" + newLine + object);
                    }
                });
                return;
            }

            Iterator<Crawler.OrderedChat> iter = mainChatList.iterator();
            appendLine(console, formattedUserLine(""));
//            System.out.println();
            appendLine(console, formattedUserLine("First " + limit + " chat(s) out of " + mainChatList.size() + " known chat(s):"));
//            System.out.println("First " + limit + " chat(s) out of " + mainChatList.size() + " known chat(s):");
            for (int i = 0; i < limit && i < mainChatList.size(); i++) {
                long chatId = iter.next().chatId;
                TdApi.Chat chat = chats.get(chatId);
                synchronized (chat) {
                    appendLine(console, formattedUserLine(chatId + ": " + chat.title));
//                    System.out.println(chatId + ": " + chat.title);
                }
            }
        }
    }

    static long getChatId(String arg) {
        long chatId = 0;
        try {
            chatId = Long.parseLong(arg);
        } catch (NumberFormatException ignored) {
        }
        return chatId;
    }

    static void setChatPositions(TdApi.Chat chat, TdApi.ChatPosition[]
            positions, NavigableSet<Crawler.OrderedChat> mainChatList) {
        synchronized (mainChatList) {
            synchronized (chat) {
                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isRemoved = mainChatList.remove(new Crawler.OrderedChat(chat.id, position));
                        assert isRemoved;
                    }
                }

                chat.positions = positions;

                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isAdded = mainChatList.add(new Crawler.OrderedChat(chat.id, position));
                        assert isAdded;
                    }
                }
            }
        }
    }

    //    static TdApi.Chat getChannelByTitle(Client client,
//                                        String chatName,
//                                        Client.ResultHandler defaultHandler) {
//        TdApi.Chat[] channel = new TdApi.Chat[1];
//        client.send(new TdApi.SearchPublicChat(chatName), object -> {
//            if (TdApi.Error.CONSTRUCTOR == object.getConstructor()) {
//                sendMessage(getChatId(chatName), "channel '" + chatName + "' not found", client, defaultHandler);
//                return;
//            }
//            TdApi.Chat chat = (TdApi.Chat) object;
//            TdApi.ChatType chatType = chat.type;
//            if (!(chatType.getConstructor() == TdApi.ChatTypeSupergroup.CONSTRUCTOR) ||
//                    !((TdApi.ChatTypeSupergroup) chatType).isChannel) {
//                sendMessage(getChatId(chatName), "'" + chatName + "'is not a channel", client, defaultHandler);
//                return;
//            }
//            channel[0] = chat;
//        });
//        return channel[0];
//    }

}

