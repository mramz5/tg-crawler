//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2025
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.drinkless.tdlib;


import lombok.AllArgsConstructor;
import org.drinkless.tdlib.ui.TerminalWindow;
import org.drinkless.tdlib.util.ObservableInteger;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.drinkless.tdlib.Constant.*;
import static org.drinkless.tdlib.ui.TerminalWindow.*;
import static org.drinkless.tdlib.util.ChatUtil.*;
import static org.drinkless.tdlib.util.CommandParamsExtractor.*;

/**
 * Example class for TDLib usage from Java.
 */
public final class Searcher {
    static Client client = null;

    static TdApi.AuthorizationState authorizationState = null;
    static volatile boolean haveAuthorization = false;
    static volatile boolean needQuit = false;
    static volatile boolean canQuit = false;

    static final Lock authorizationLock = new ReentrantLock();
    static final Condition gotAuthorization = authorizationLock.newCondition();

    public static final Lock waitForInput = new ReentrantLock();
    public static final Condition gotInput = waitForInput.newCondition();

    static final ConcurrentMap<Long, TdApi.User> users = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, TdApi.BasicGroup> basicGroups = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, TdApi.Supergroup> supergroups = new ConcurrentHashMap<>();
    static final ConcurrentMap<Integer, TdApi.SecretChat> secretChats = new ConcurrentHashMap<>();

    static final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<>();
    static final NavigableSet<OrderedChat> mainChatList = new TreeSet<>();
    static boolean haveFullMainChatList = false;

    static final ConcurrentMap<Long, TdApi.UserFullInfo> usersFullInfo = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, TdApi.BasicGroupFullInfo> basicGroupsFullInfo = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, TdApi.SupergroupFullInfo> supergroupsFullInfo = new ConcurrentHashMap<>();

    static final String newLine = System.lineSeparator();
    static final String commandsLine = String.format("""
            Enter command :(
             %s - GetChats [limit],
             %s <chatId> - GetChat,
             %s - GetMe,
             %s <chatId> <message> - SendMessage,
             %s {--chats chats comma separated} {--words keywords comma separated} [--size size] [--page size] - search chats for given keywords,
             %s - LogOut,
             %s - Quit
            ):\s""", GCS, GC, ME, SM, SCW, LO, Q);
    static final String warningsLine = """
            WARNING!
            DO NOT LOG INTO YOUR ACCOUNT FROM TOO MANY DEVICES,
            AS THIS MAY PREVENT NEW DEVICES FROM SENDING REQUESTS TO THE SERVER.
            ===================================================================
            """;
    static volatile String currentPrompt = null;

    static void print(TerminalWindow terminalWindow, String str) {
        if (currentPrompt != null)
            terminalWindow.appendLine(formattedOutputLine(currentPrompt));
        terminalWindow.appendLine(formattedOutputLine(str));
        if (currentPrompt != null)
            terminalWindow.appendLine(formattedOutputLine(currentPrompt));
    }

    private static void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState, TerminalWindow terminalWindow) {
        if (authorizationState != null) {
            Searcher.authorizationState = authorizationState;
        }
        switch (Searcher.authorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
                TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
                request.databaseDirectory = "./tdlib";
                request.useMessageDatabase = true;
                request.useSecretChats = true;
                request.apiId = 94575;
                request.apiHash = "a3406de8d171bb422bb6ddf3bbd800e2";
                request.systemLanguageCode = "en";
                request.deviceModel = "Desktop";
                request.applicationVersion = "1.0";

                client.send(request, new AuthorizationRequestHandler(terminalWindow));
                break;
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
                String phoneNumber = promptString(terminalWindow, "Please enter phone number(international format): ");
                client.send(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), new AuthorizationRequestHandler(terminalWindow));
                break;
            }
            case TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR: {
                String link = ((TdApi.AuthorizationStateWaitOtherDeviceConfirmation) Searcher.authorizationState).link;
                terminalWindow.appendLine(formattedUserLine("Please confirm this login link on another device: " + link));
                break;
            }
            case TdApi.AuthorizationStateWaitEmailAddress.CONSTRUCTOR: {
                String emailAddress = promptString(terminalWindow, "Please enter email address: ");
                client.send(new TdApi.SetAuthenticationEmailAddress(emailAddress), new AuthorizationRequestHandler(terminalWindow));
                break;
            }
            case TdApi.AuthorizationStateWaitEmailCode.CONSTRUCTOR: {
                String code = promptString(terminalWindow, "Please enter email authentication code: ");
                client.send(new TdApi.CheckAuthenticationEmailCode(new TdApi.EmailAddressAuthenticationCode(code)), new AuthorizationRequestHandler(terminalWindow));
                break;
            }
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                String code = promptString(terminalWindow, "Please enter authentication code: ");
                client.send(new TdApi.CheckAuthenticationCode(code), new AuthorizationRequestHandler(terminalWindow));
                break;
            }
            case TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR: {
                String firstName = promptString(terminalWindow, "Please enter your first name: ");
                String lastName = promptString(terminalWindow, "Please enter your last name: ");
                client.send(new TdApi.RegisterUser(firstName, lastName, false), new AuthorizationRequestHandler(terminalWindow));
                break;
            }
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR: {
                String password = promptString(terminalWindow, "Please enter password: ");
                client.send(new TdApi.CheckAuthenticationPassword(password), new AuthorizationRequestHandler(terminalWindow));
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR:
                haveAuthorization = true;
                authorizationLock.lock();
                try {
                    gotAuthorization.signal();
                } finally {
                    authorizationLock.unlock();
                }
                break;
            case TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR:
                haveAuthorization = false;
                print(terminalWindow, "Logging out");
                break;
            case TdApi.AuthorizationStateClosing.CONSTRUCTOR:
                haveAuthorization = false;
                print(terminalWindow, "Closing");
                break;
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR:
                print(terminalWindow, "Closed");
                if (!needQuit) {
                    client = Client.create(new UpdateHandler(terminalWindow), null, null); // recreate client after previous has closed
                } else {
                    canQuit = true;
                }
                break;
            default:
                terminalWindow.appendLine(formattedErrorLine("Unsupported authorization state:" + newLine + Searcher.authorizationState));
        }
    }


    private static String promptString(TerminalWindow terminalWindow, String prompt) {
        if (prompt != null)
            terminalWindow.appendLine(formattedOutputLine(prompt));
        currentPrompt = null;
        String trimmed;
        try {
            waitForInput.lock();
            gotInput.await();
            trimmed = terminalWindow.getCurrentPrompt().trim();

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            waitForInput.unlock();
        }
        terminalWindow.appendLine(formattedUserLine(trimmed));
        return trimmed;
    }


    public static String getCommand(TerminalWindow terminalWindow, String promptString) {
        String command = promptString(terminalWindow, promptString);
        try {
            if (!command.isEmpty()) {
                waitForInput.lock();
                gotInput.signal();
                waitForInput.unlock();
            }
            handleInput(terminalWindow, command);
        } catch (ArrayIndexOutOfBoundsException e) {
            print(terminalWindow, "Not enough arguments");
        }
        return command;
    }

    public static void handleInput(TerminalWindow terminalWindow, String command) {
        String[] commands = command.split("\\s+", 2);

        switch (commands[0]) {
            case GCS:

                int[] limit = {20};
                extractGCSParams(commands[1], limit);
                getMainChatList(terminalWindow, limit[0], client, mainChatList, haveFullMainChatList, newLine, chats);
                break;

            case GC:

                client.send(new TdApi.GetChat(getChatId(commands[1])), new DefaultHandler(terminalWindow));
                break;
            case ME:

                client.send(new TdApi.GetMe(), new DefaultHandler(terminalWindow));
                break;
            case SM:

                long[] chatId = {0};
                String[] message = {""};
                extractSMParams(commands[1], chatId, message);
                sendMessage(chatId[0], message[0], client, new DefaultHandler(terminalWindow));
                break;

            case LO:

                haveAuthorization = false;
                client.send(new TdApi.LogOut(), new DefaultHandler(terminalWindow));
                break;
            case Q:
                needQuit = true;
                haveAuthorization = false;
                client.send(new TdApi.Close(), new DefaultHandler(terminalWindow));
                System.exit(0);
                break;
            case SCW:

                List<String> chatNameList = new ArrayList<>(), keywords = new ArrayList<>();
                int[] numberOfPages = {0}, size = {0};
                String unRefinedCommand = commands[1];
                extractSCWParams(unRefinedCommand, chatNameList, keywords, numberOfPages, size);
                terminalWindow.appendLine(formattedOutputLine("\n"));
                ObservableInteger allMessageCount = new ObservableInteger(0, terminalWindow.getCurrentPosition());

                for (String chatName : chatNameList)
                    getMessagesByKeywordsInChannelTitle(
                            terminalWindow,
                            client,
                            chatName,
                            keywords,
                            TPage.of(0, numberOfPages[0], size[0], 0),
                            allMessageCount,
                            new DefaultHandler(terminalWindow));
                break;
            default:
                if (haveAuthorization && !command.isBlank())
                    terminalWindow.appendLine(formattedErrorLine("Unsupported command: " + command));
        }
    }


    public static void run(TerminalWindow terminalWindow) {
        try {
            // set log message handler to handle only fatal errors (0) and plain log messages (-1)
            Client.setLogMessageHandler(0, new LogMessageHandler(terminalWindow));

            // disable TDLib log and redirect fatal errors and plain log messages to a file
            try {

                Client.execute(new TdApi.SetLogVerbosityLevel(0));
                Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27, false)));
            } catch (Client.ExecutionException error) {
                throw new IOError(new IOException("Write access to the current directory is required"));
            }

            // create client
            client = Client.create(new UpdateHandler(terminalWindow), null, null);

            terminalWindow.appendLine(formattedSystemLine("Tips: ↑/↓ history • Ctrl+L clear • Ctrl+R toggle input RTL/LTR "));
            terminalWindow.appendLine(formattedWarnLine(warningsLine));

//             main loop
            while (!needQuit) {
                // await authorization
                authorizationLock.lock();
                try {
                    while (!haveAuthorization) {
                        gotAuthorization.await();
                    }
                } finally {
                    authorizationLock.unlock();
                }

                while (haveAuthorization) {
                    terminalWindow.appendLine(formattedOutputLine(commandsLine));
                    getCommand(terminalWindow, currentPrompt);
                }
            }
            while (!canQuit) {
                Thread.sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class OrderedChat implements Comparable<OrderedChat> {
        public final long chatId;
        final TdApi.ChatPosition position;

        public OrderedChat(long chatId, TdApi.ChatPosition position) {
            this.chatId = chatId;
            this.position = position;
        }

        @Override
        public int compareTo(OrderedChat o) {
            if (this.position.order != o.position.order) {
                return o.position.order < this.position.order ? -1 : 1;
            }
            if (this.chatId != o.chatId) {
                return o.chatId < this.chatId ? -1 : 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            OrderedChat o = (OrderedChat) obj;
            return this.chatId == o.chatId && this.position.order == o.position.order;
        }
    }

    @AllArgsConstructor
    private static class DefaultHandler implements Client.ResultHandler {
        private TerminalWindow terminalWindow;

        @Override
        public void onResult(TdApi.Object object) {
            print(terminalWindow, object.toString());
        }
    }

    @AllArgsConstructor
    private static class UpdateHandler implements Client.ResultHandler {
        private TerminalWindow terminalWindow;

        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.UpdateAuthorizationState.CONSTRUCTOR:
                    onAuthorizationStateUpdated(((TdApi.UpdateAuthorizationState) object).authorizationState, terminalWindow);
                    break;

                case TdApi.UpdateUser.CONSTRUCTOR:
                    TdApi.UpdateUser updateUser = (TdApi.UpdateUser) object;
                    users.put(updateUser.user.id, updateUser.user);
                    break;
                case TdApi.UpdateUserStatus.CONSTRUCTOR: {
                    TdApi.UpdateUserStatus updateUserStatus = (TdApi.UpdateUserStatus) object;
                    TdApi.User user = users.get(updateUserStatus.userId);
                    synchronized (user) {
                        user.status = updateUserStatus.status;
                    }
                    break;
                }
                case TdApi.UpdateBasicGroup.CONSTRUCTOR:
                    TdApi.UpdateBasicGroup updateBasicGroup = (TdApi.UpdateBasicGroup) object;
                    basicGroups.put(updateBasicGroup.basicGroup.id, updateBasicGroup.basicGroup);
                    break;
                case TdApi.UpdateSupergroup.CONSTRUCTOR:
                    TdApi.UpdateSupergroup updateSupergroup = (TdApi.UpdateSupergroup) object;
                    supergroups.put(updateSupergroup.supergroup.id, updateSupergroup.supergroup);
                    break;
                case TdApi.UpdateSecretChat.CONSTRUCTOR:
                    TdApi.UpdateSecretChat updateSecretChat = (TdApi.UpdateSecretChat) object;
                    secretChats.put(updateSecretChat.secretChat.id, updateSecretChat.secretChat);
                    break;

                case TdApi.UpdateNewChat.CONSTRUCTOR: {
                    TdApi.UpdateNewChat updateNewChat = (TdApi.UpdateNewChat) object;
                    TdApi.Chat chat = updateNewChat.chat;
                    synchronized (chat) {
                        chats.put(chat.id, chat);

                        TdApi.ChatPosition[] positions = chat.positions;
                        chat.positions = new TdApi.ChatPosition[0];
                        setChatPositions(chat, positions, mainChatList);
                    }
                    break;
                }
                case TdApi.UpdateChatTitle.CONSTRUCTOR: {
                    TdApi.UpdateChatTitle updateChat = (TdApi.UpdateChatTitle) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.title = updateChat.title;
                    }
                    break;
                }
                case TdApi.UpdateChatPhoto.CONSTRUCTOR: {
                    TdApi.UpdateChatPhoto updateChat = (TdApi.UpdateChatPhoto) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.photo = updateChat.photo;
                    }
                    break;
                }
                case TdApi.UpdateChatPermissions.CONSTRUCTOR: {
                    TdApi.UpdateChatPermissions update = (TdApi.UpdateChatPermissions) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.permissions = update.permissions;
                    }
                    break;
                }
                case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                    TdApi.UpdateChatLastMessage updateChat = (TdApi.UpdateChatLastMessage) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.lastMessage = updateChat.lastMessage;
                        setChatPositions(chat, updateChat.positions, mainChatList);
                    }
                    break;
                }
                case TdApi.UpdateChatPosition.CONSTRUCTOR: {
                    TdApi.UpdateChatPosition updateChat = (TdApi.UpdateChatPosition) object;
                    if (updateChat.position.list.getConstructor() != TdApi.ChatListMain.CONSTRUCTOR) {
                        break;
                    }

                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        int i;
                        for (i = 0; i < chat.positions.length; i++) {
                            if (chat.positions[i].list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                                break;
                            }
                        }
                        TdApi.ChatPosition[] new_positions = new TdApi.ChatPosition[chat.positions.length + (updateChat.position.order == 0 ? 0 : 1) - (i < chat.positions.length ? 1 : 0)];
                        int pos = 0;
                        if (updateChat.position.order != 0) {
                            new_positions[pos++] = updateChat.position;
                        }
                        for (int j = 0; j < chat.positions.length; j++) {
                            if (j != i) {
                                new_positions[pos++] = chat.positions[j];
                            }
                        }
                        assert pos == new_positions.length;

                        setChatPositions(chat, new_positions, mainChatList);
                    }
                    break;
                }
                case TdApi.UpdateChatReadInbox.CONSTRUCTOR: {
                    TdApi.UpdateChatReadInbox updateChat = (TdApi.UpdateChatReadInbox) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId;
                        chat.unreadCount = updateChat.unreadCount;
                    }
                    break;
                }
                case TdApi.UpdateChatReadOutbox.CONSTRUCTOR: {
                    TdApi.UpdateChatReadOutbox updateChat = (TdApi.UpdateChatReadOutbox) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId;
                    }
                    break;
                }
                case TdApi.UpdateChatActionBar.CONSTRUCTOR: {
                    TdApi.UpdateChatActionBar updateChat = (TdApi.UpdateChatActionBar) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.actionBar = updateChat.actionBar;
                    }
                    break;
                }
                case TdApi.UpdateChatAvailableReactions.CONSTRUCTOR: {
                    TdApi.UpdateChatAvailableReactions updateChat = (TdApi.UpdateChatAvailableReactions) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.availableReactions = updateChat.availableReactions;
                    }
                    break;
                }
                case TdApi.UpdateChatDraftMessage.CONSTRUCTOR: {
                    TdApi.UpdateChatDraftMessage updateChat = (TdApi.UpdateChatDraftMessage) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.draftMessage = updateChat.draftMessage;
                        setChatPositions(chat, updateChat.positions, mainChatList);
                    }
                    break;
                }
                case TdApi.UpdateChatMessageSender.CONSTRUCTOR: {
                    TdApi.UpdateChatMessageSender updateChat = (TdApi.UpdateChatMessageSender) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.messageSenderId = updateChat.messageSenderId;
                    }
                    break;
                }
                case TdApi.UpdateChatMessageAutoDeleteTime.CONSTRUCTOR: {
                    TdApi.UpdateChatMessageAutoDeleteTime updateChat = (TdApi.UpdateChatMessageAutoDeleteTime) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.messageAutoDeleteTime = updateChat.messageAutoDeleteTime;
                    }
                    break;
                }
                case TdApi.UpdateChatNotificationSettings.CONSTRUCTOR: {
                    TdApi.UpdateChatNotificationSettings update = (TdApi.UpdateChatNotificationSettings) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.notificationSettings = update.notificationSettings;
                    }
                    break;
                }
                case TdApi.UpdateChatPendingJoinRequests.CONSTRUCTOR: {
                    TdApi.UpdateChatPendingJoinRequests update = (TdApi.UpdateChatPendingJoinRequests) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.pendingJoinRequests = update.pendingJoinRequests;
                    }
                    break;
                }
                case TdApi.UpdateChatReplyMarkup.CONSTRUCTOR: {
                    TdApi.UpdateChatReplyMarkup updateChat = (TdApi.UpdateChatReplyMarkup) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.replyMarkupMessageId = updateChat.replyMarkupMessageId;
                    }
                    break;
                }
                case TdApi.UpdateChatBackground.CONSTRUCTOR: {
                    TdApi.UpdateChatBackground updateChat = (TdApi.UpdateChatBackground) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.background = updateChat.background;
                    }
                    break;
                }
                case TdApi.UpdateChatTheme.CONSTRUCTOR: {
                    TdApi.UpdateChatTheme updateChat = (TdApi.UpdateChatTheme) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.themeName = updateChat.themeName;
                    }
                    break;
                }
                case TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR: {
                    TdApi.UpdateChatUnreadMentionCount updateChat = (TdApi.UpdateChatUnreadMentionCount) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.unreadMentionCount = updateChat.unreadMentionCount;
                    }
                    break;
                }
                case TdApi.UpdateChatUnreadReactionCount.CONSTRUCTOR: {
                    TdApi.UpdateChatUnreadReactionCount updateChat = (TdApi.UpdateChatUnreadReactionCount) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.unreadReactionCount = updateChat.unreadReactionCount;
                    }
                    break;
                }
                case TdApi.UpdateChatVideoChat.CONSTRUCTOR: {
                    TdApi.UpdateChatVideoChat updateChat = (TdApi.UpdateChatVideoChat) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.videoChat = updateChat.videoChat;
                    }
                    break;
                }
                case TdApi.UpdateChatDefaultDisableNotification.CONSTRUCTOR: {
                    TdApi.UpdateChatDefaultDisableNotification update = (TdApi.UpdateChatDefaultDisableNotification) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.defaultDisableNotification = update.defaultDisableNotification;
                    }
                    break;
                }
                case TdApi.UpdateChatHasProtectedContent.CONSTRUCTOR: {
                    TdApi.UpdateChatHasProtectedContent updateChat = (TdApi.UpdateChatHasProtectedContent) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.hasProtectedContent = updateChat.hasProtectedContent;
                    }
                    break;
                }
                case TdApi.UpdateChatIsTranslatable.CONSTRUCTOR: {
                    TdApi.UpdateChatIsTranslatable update = (TdApi.UpdateChatIsTranslatable) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.isTranslatable = update.isTranslatable;
                    }
                    break;
                }
                case TdApi.UpdateChatIsMarkedAsUnread.CONSTRUCTOR: {
                    TdApi.UpdateChatIsMarkedAsUnread update = (TdApi.UpdateChatIsMarkedAsUnread) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.isMarkedAsUnread = update.isMarkedAsUnread;
                    }
                    break;
                }
                case TdApi.UpdateChatBlockList.CONSTRUCTOR: {
                    TdApi.UpdateChatBlockList update = (TdApi.UpdateChatBlockList) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.blockList = update.blockList;
                    }
                    break;
                }
                case TdApi.UpdateChatHasScheduledMessages.CONSTRUCTOR: {
                    TdApi.UpdateChatHasScheduledMessages update = (TdApi.UpdateChatHasScheduledMessages) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.hasScheduledMessages = update.hasScheduledMessages;
                    }
                    break;
                }

                case TdApi.UpdateMessageMentionRead.CONSTRUCTOR: {
                    TdApi.UpdateMessageMentionRead updateChat = (TdApi.UpdateMessageMentionRead) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.unreadMentionCount = updateChat.unreadMentionCount;
                    }
                    break;
                }
                case TdApi.UpdateMessageUnreadReactions.CONSTRUCTOR: {
                    TdApi.UpdateMessageUnreadReactions updateChat = (TdApi.UpdateMessageUnreadReactions) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.unreadReactionCount = updateChat.unreadReactionCount;
                    }
                    break;
                }

                case TdApi.UpdateUserFullInfo.CONSTRUCTOR:
                    TdApi.UpdateUserFullInfo updateUserFullInfo = (TdApi.UpdateUserFullInfo) object;
                    usersFullInfo.put(updateUserFullInfo.userId, updateUserFullInfo.userFullInfo);
                    break;
                case TdApi.UpdateBasicGroupFullInfo.CONSTRUCTOR:
                    TdApi.UpdateBasicGroupFullInfo updateBasicGroupFullInfo = (TdApi.UpdateBasicGroupFullInfo) object;
                    basicGroupsFullInfo.put(updateBasicGroupFullInfo.basicGroupId, updateBasicGroupFullInfo.basicGroupFullInfo);
                    break;
                case TdApi.UpdateSupergroupFullInfo.CONSTRUCTOR:
                    TdApi.UpdateSupergroupFullInfo updateSupergroupFullInfo = (TdApi.UpdateSupergroupFullInfo) object;
                    supergroupsFullInfo.put(updateSupergroupFullInfo.supergroupId, updateSupergroupFullInfo.supergroupFullInfo);
                    break;
                default:
                    System.out.println("Unsupported update:" + newLine + object);
            }
        }
    }

    @AllArgsConstructor
    private static class AuthorizationRequestHandler implements Client.ResultHandler {
        private TerminalWindow terminalWindow;

        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.Error.CONSTRUCTOR:
                    terminalWindow.appendLine(formattedErrorLine("Receive an error:" + newLine + object));
                    onAuthorizationStateUpdated(null, terminalWindow); // repeat last action
                    break;
                case TdApi.Ok.CONSTRUCTOR:
                    // result is already received through UpdateAuthorizationState, nothing to do
                    break;
                default:
                    terminalWindow.appendLine(formattedErrorLine("Receive wrong response from TDLib:" + newLine + object));
            }
        }
    }

    @AllArgsConstructor
    private static class LogMessageHandler implements Client.LogMessageHandler {
        private TerminalWindow terminalWindow;
        private final Logger logger = Logger.getLogger(LogMessageHandler.class.getName());

        @Override
        public void onLogMessage(int verbosityLevel, String message) {
            if (verbosityLevel == 0) {
                logger.log(Level.SEVERE, "Error occurred!");
                onFatalError(message);
                terminalWindow.appendLine(formattedErrorLine(message));
            }
            logger.log(verbosityLevel == 1 ? Level.SEVERE :
                            verbosityLevel == 2 ? Level.INFO :
                                    verbosityLevel == 3 ? Level.CONFIG :
                                            verbosityLevel == 4 ? Level.FINE :
                                                    verbosityLevel == 5 ? Level.FINER :
                                                            Level.FINEST,
                    message
            );
        }


    }

    private static void onFatalError(String errorMessage) {
        final class ThrowError implements Runnable {
            private final String errorMessage;
            private final AtomicLong errorThrowTime;

            private ThrowError(String errorMessage, AtomicLong errorThrowTime) {
                this.errorMessage = errorMessage;
                this.errorThrowTime = errorThrowTime;
            }

            @Override
            public void run() {
                if (isDatabaseBrokenError(errorMessage) || isDiskFullError(errorMessage) || isDiskError(errorMessage)) {
                    processExternalError();
                    return;
                }

                errorThrowTime.set(System.currentTimeMillis());
                throw new ClientError("TDLib fatal error: " + errorMessage);
            }

            private void processExternalError() {
                errorThrowTime.set(System.currentTimeMillis());
                throw new ExternalClientError("Fatal error: " + errorMessage);
            }

            final class ClientError extends Error {
                private ClientError(String message) {
                    super(message);
                }
            }

            final class ExternalClientError extends Error {
                public ExternalClientError(String message) {
                    super(message);
                }
            }

            private boolean isDatabaseBrokenError(String message) {
                return message.contains("Wrong key or database is corrupted") ||
                        message.contains("SQL logic error or missing database") ||
                        message.contains("database disk image is malformed") ||
                        message.contains("file is encrypted or is not a database") ||
                        message.contains("unsupported file format") ||
                        message.contains("Database was corrupted and deleted during execution and can't be recreated");
            }

            private boolean isDiskFullError(String message) {
                return message.contains("PosixError : No space left on device") ||
                        message.contains("database or disk is full");
            }

            private boolean isDiskError(String message) {
                return message.contains("I/O error") || message.contains("Structure needs cleaning");
            }
        }

        final AtomicLong errorThrowTime = new AtomicLong(Long.MAX_VALUE);
        new Thread(new ThrowError(errorMessage, errorThrowTime), "TDLib fatal error thread").start();

        // wait at least 10 seconds after the error is thrown
        while (errorThrowTime.get() >= System.currentTimeMillis() - 10000) {
            try {
                Thread.sleep(1000 /* milliseconds */);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
