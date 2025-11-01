package org.drinkless.tdlib;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.drinkless.tdlib.ChatUtil.*;
import static org.drinkless.tdlib.ChatUtil.getChatId;
import static org.drinkless.tdlib.ChatUtil.getMessagesByKeywordsInChannelTitle;
import static org.drinkless.tdlib.Crawler.*;

public class TerminalApp {
    static final JTextPane console = new JTextPane();
    static final JTextField input = new JTextField();
    static String caughtCommand;
    static List<String> history = new ArrayList<>();
    static final int[] histIndex = {-1};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TerminalApp::createAndShow);
        Crawler.setConsole(console);
        Crawler.main(args);
    }

    static JFrame getFrame() {
        JFrame frame = new JFrame("tg-crawler");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(900, 560);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
        return frame;
    }

    static JTextField getTextField() {
        // --- Input field ---
        input.setFont(new Font("DialogInput", Font.PLAIN, 16));
        input.setBackground(Color.BLACK);
        input.setForeground(Color.WHITE);
        input.setCaretColor(Color.WHITE);
        input.requestFocusInWindow();
        input.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return input;
    }

    static JPanel getBottom(JFrame frame, JTextField input) {
        JLabel prompt = new JLabel(">");
        prompt.setFont(new Font("DialogInput", Font.BOLD, 16));
        prompt.setForeground(new Color(31, 115, 3));
        prompt.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBackground(Color.BLACK);
        bottom.add(prompt, BorderLayout.WEST);
        bottom.add(input, BorderLayout.CENTER);

        frame.add(bottom, BorderLayout.SOUTH);
        return bottom;
    }

    static JTextPane getConsole(JTextPane console) {
        // --- Console area ---
        if (console == null)
            console = new JTextPane();

        console.setEditable(false);
        console.setFont(new Font("DialogInput", Font.PLAIN, 15));
        console.setBackground(Color.BLACK);
        console.setForeground(Color.WHITE);
        DefaultCaret caret = (DefaultCaret) console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        return console;
    }

    static JScrollPane getScrollPane(JTextPane console, JFrame frame) {
        JScrollPane scroll = new JScrollPane(console);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        frame.add(scroll, BorderLayout.CENTER);
        return scroll;
    }


    static void addInputHistoryListener(JTextField input) {
        // History
        // ↑ / ↓ history
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> {
                        if (!history.isEmpty()) {
                            if (histIndex[0] < 0) histIndex[0] = history.size() - 1;
                            else histIndex[0] = Math.max(0, histIndex[0] - 1);
                            input.setText(history.get(histIndex[0]));
                            input.setCaretPosition(input.getText().length());
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_DOWN -> {
                        if (!history.isEmpty()) {
                            if (histIndex[0] >= history.size() - 1) {
                                histIndex[0] = -1;
                                input.setText("");
                            } else {
                                histIndex[0]++;
                                input.setText(history.get(histIndex[0]));
                                input.setCaretPosition(input.getText().length());
                            }
                            e.consume();
                        }
                    }
                }
            }
        });
    }

    static void addInputClearListener(JTextField input, JTextPane console) {
        // Ctrl+L clears console
        input.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "clear");
        input.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                console.setText("");
            }
        });
    }

    static void addInputOrientationListener(JTextField input) {
        // Ctrl+R toggles ONLY the input orientation
        input.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "toggleRTL");
        input.getActionMap().put("toggleRTL", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean rtl = input.getComponentOrientation().isLeftToRight();
                input.setComponentOrientation(rtl
                        ? ComponentOrientation.RIGHT_TO_LEFT
                        : ComponentOrientation.LEFT_TO_RIGHT);
            }
        });
    }

    static void handleUserInputs(JTextField input, JTextPane console, List<String> history, int[] histIndex) {
        // Enter → send
        input.addActionListener(e -> {
            String cmd = input.getText();
            if (cmd == null) cmd = "";
            String trimmed = cmd.strip();

//            appendLine(console, formattedUserLine(trimmed));

            if (!trimmed.isEmpty()) {
                history.add(trimmed);
                histIndex[0] = -1;

                // Async “backend” executor
                ExecutorService ioPool = Executors.newFixedThreadPool(
                        1,
                        r -> {
                            Thread t = new Thread(r, "terminal-io");
                            t.setDaemon(true);
                            return t;
                        });

                // Demo command handler (replace with your real async code)
                // Takes a command string -> returns a future that completes later
                Crawler.setConsole(console);
                Function<String, CompletableFuture<String>> handler = demoHandler(ioPool);

                handler.apply(trimmed)
                        .thenAccept(result -> appendLine(console, formattedOutputLine(result)))
                        .exceptionally(ex -> {
                            appendLine(console, formattedErrorLine(ex));
                            return null;
                        });
            }

            input.setText("");

            // Optional auto RTL detection
            if (isProbablyRTL(trimmed)) {
                input.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            } else if (!trimmed.isEmpty()) {
                input.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            }

//            set the focus again
            input.requestFocusInWindow();
        });
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JFrame frame = TerminalApp.getFrame();
            JTextField input = TerminalApp.getTextField();
            JPanel bottom = TerminalApp.getBottom(frame, input);
            JTextPane textPane = TerminalApp.getConsole(console);
            JScrollPane scrollPane = TerminalApp.getScrollPane(textPane, frame);



            addInputHistoryListener(input);
            addInputClearListener(input, textPane);
            addInputOrientationListener(input);
            handleUserInputs(input, console, history, histIndex);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


//        // Async pool
//        ExecutorService ioPool = Executors.newFixedThreadPool(
//                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
//                r -> {
//                    Thread t = new Thread(r, "terminal-io");
//                    t.setDaemon(true);
//                    return t;
//                });
//
//        Function<String, CompletableFuture<String>> handler = demoHandler(ioPool);
    }

    // ---------- Async handler ----------
    private static Function<String, CompletableFuture<String>> demoHandler(ExecutorService pool) {
        //if more than 10kb then clear the console
        if (console.getText().length() > 1024 * 10)
            console.setText("");
        return command -> CompletableFuture.supplyAsync(() -> {
            try {
                caughtCommand = command;
                if (!command.isEmpty()) {
                    waitForInput.lock();
                    gotInput.signal();
                }

                String[] commands = command.split(" ");

                switch (commands[0]) {
                    case "gcs": {
                        int limit = 20;
                        if (commands.length > 1) {
                            limit = toInt(commands[1]);
                        }
                        getMainChatList(console, limit, client, mainChatList, haveFullMainChatList, newLine, chats, defaultHandler);
                        break;
                    }
                    case "gc":
                        client.send(new TdApi.GetChat(getChatId(commands[1])), defaultHandler);
                        break;
                    case "me":
                        client.send(new TdApi.GetMe(), defaultHandler);
                        break;
                    case "sm": {
                        sendMessage(getChatId(commands[1]), commands[2], client, defaultHandler);
                        break;
                    }
                    case "lo":
                        haveAuthorization = false;
                        client.send(new TdApi.LogOut(), defaultHandler);
                        break;
                    case "q":
                        needQuit = true;
                        haveAuthorization = false;
                        client.send(new TdApi.Close(), defaultHandler);
                        System.exit(0);
                        break;
                    case "scw":
                        // scw akharinkhabar,Tasnimnews سرقت,آگاهی 5 5
                        String[] chatNameList = commands[1].split(",");
                        String[] keywords = commands[2].split(",");
                        int size = 0, numberOfPages = 0;
                        if (commands.length > 3)
                            size = Integer.parseInt(commands[3]);
                        if (commands.length > 4)
                            numberOfPages = Integer.parseInt(commands[4]);

                        for (String chatName : chatNameList)
                            getMessagesByKeywordsInChannelTitle(console, client,
                                    chatName,
                                    keywords,
                                    TPage.of(0, numberOfPages, size, 0),
                                    defaultHandler);
                        break;
                    default:
                        if (haveAuthorization)
                            appendLine(console, TerminalApp.formattedErrorLine("Unsupported command: " + command));
//                        System.err.println("Unsupported command: " + command);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                print("Not enough arguments");
            } finally {
                waitForInput.unlock();
            }
            return "";
        }, pool);
    }

    // ---------- Console printing ----------
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    static void appendLine(JTextPane pane, StyledString s) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = pane.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), s.text + "\n", style(s));
                pane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    static StyledString formattedErrorLine(String input) {
        String ts = "[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(112, 15, 15), true);
    }

    static StyledString formattedWarnLine(String input) {
        String ts = "[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(158, 155, 8), true);
    }

    static StyledString formattedUserLine(String input) {
        String ts = "[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(31, 115, 3), true);
    }

    static StyledString formattedOutputLine(String out) {
        String ts = "[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " > " + out, Color.WHITE, false);
    }

    static StyledString formattedSystemLine(String msg) {
        String ts = "[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " # " + msg, Color.CYAN, false);
    }

    private static StyledString formattedErrorLine(Throwable ex) {
        String ts = "[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " ! " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()),
                Color.RED, false);
    }

    record StyledString(String text, Color color, boolean bold) {
    }

    private static AttributeSet style(StyledString s) {
        SimpleAttributeSet as = new SimpleAttributeSet();
        StyleConstants.setForeground(as, s.color());
        StyleConstants.setBold(as, s.bold());
        StyleConstants.setFontFamily(as, "DialogInput");
        StyleConstants.setFontSize(as, 15);
        return as;
    }

    private static boolean isProbablyRTL(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) {
            byte dir = Character.getDirectionality(c);
            if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) return false;
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) return true;
        }
        return false;
    }
}
