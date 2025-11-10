package org.drinkless.tdlib;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static java.lang.Character.isWhitespace;
import static org.drinkless.tdlib.ChatUtil.*;
import static org.drinkless.tdlib.Crawler.*;

public class TerminalApp {
    static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    static final JTextPane console = new JTextPane();
    static final JTextField input = new JTextField();
    static String caughtCommand;
    static List<String> history = new ArrayList<>();
    static final int[] histIndex = {-1};
    static final Color BLACK_GREY = new Color(30, 30, 30);

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
        input.setFont(new Font("Tahoma", Font.PLAIN, 16));
        input.setBackground(BLACK_GREY);
        input.setForeground(Color.WHITE);
        input.setCaretColor(Color.WHITE);
        input.requestFocusInWindow();
        input.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return input;
    }

    static JPanel getBottom(JFrame frame, JTextField input) {
        JLabel prompt = new JLabel(">");
        prompt.setFont(new Font("Tahoma", Font.BOLD, 16));
        prompt.setForeground(new Color(31, 115, 3));
        prompt.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBackground(BLACK_GREY);
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
        console.setFont(new Font("Tahoma", Font.PLAIN, 15));
        console.setBackground(BLACK_GREY);
        console.setForeground(BLACK_GREY);
        DefaultCaret caret = (DefaultCaret) console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        int topMargin = (int) (screenSize.height * 0.03);
        int leftMargin = (int) (screenSize.width * 0.03);
        console.setMargin(new Insets(topMargin, leftMargin, topMargin, (int) (screenSize.width * 0.7)));
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
//        if more than 10kb then clear the console
//        if (console.getText().length() > 1024 * 10)
//            console.setText("");
        return command -> CompletableFuture.supplyAsync(() -> {
            try {
                caughtCommand = command;
                if (!command.isEmpty()) {
                    waitForInput.lock();
                    gotInput.signal();
                }

                String[] commands = command.split("\\s+", 2);

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
                        String unRefinedCommand = commands[1];
//                        scw    --words سردار قنبری  , سردار به گذر   , سرقت از بانک   --chats tasnimnews,akharinkhabar   , farsna     --page 5   --size 10
                        int startOfChannels = unRefinedCommand.indexOf("--chats") + 7, stopOfChannels = 0;
                        int startOfWords = unRefinedCommand.indexOf("--words") + 7, stopOfWords = 0;
                        int startOfPage = unRefinedCommand.indexOf("--page") + 6, stopOfPage = 0;
                        int startOfSize = unRefinedCommand.indexOf("--size") + 6, stopOfSize = 0;

                        boolean stopOfChannelsSet = false,
                                stopOfWordsSet = false,
                                stopOfPageSet = false,
                                stopOfSizeSet = false;

                        if (!isWhitespace(unRefinedCommand.charAt(startOfChannels))
                                || !isWhitespace(unRefinedCommand.charAt(startOfWords))
                                || !isWhitespace(unRefinedCommand.charAt(startOfPage))
                                || !isWhitespace(unRefinedCommand.charAt(startOfSize)))
                            throw new IllegalArgumentException("invalid command");


                        for (int i = 0; i < unRefinedCommand.length(); i++) {
                            if (i >= startOfChannels && (i == startOfWords || i == startOfPage || i == startOfSize || i == unRefinedCommand.length() - 1) && !stopOfChannelsSet) {
                                stopOfChannels = (i == startOfWords ? i - 7 : i == unRefinedCommand.length() - 1 ? i : i - 6); // i-6 is for both page anz size
                                stopOfChannelsSet = true;
                            }

                            if (i >= startOfWords && (i == startOfChannels || i == startOfPage || i == startOfSize || i == unRefinedCommand.length() - 1) && !stopOfWordsSet) {
                                stopOfWords = (i == startOfChannels ? i - 7 : i == unRefinedCommand.length() - 1 ? i : i - 6);
                                stopOfWordsSet = true;
                            }

                            if (i >= startOfPage && (i == startOfChannels || i == startOfWords || i == startOfSize || i == unRefinedCommand.length() - 1) && !stopOfPageSet) {
                                stopOfPage = (i == startOfChannels || i == startOfWords ? i - 7 : i == unRefinedCommand.length() - 1 ? i + 1 : i - 6);
                                stopOfPageSet = true;
                            }

                            if (i >= startOfSize && (i == startOfChannels || i == startOfWords || i == startOfPage || i == unRefinedCommand.length() - 1) && !stopOfSizeSet) {
                                stopOfSize = (i == startOfChannels || i == startOfWords ? i - 7 : i == unRefinedCommand.length() - 1 ? i + 1 : i - 6);
                                stopOfSizeSet = true;
                            }
                        }


                        List<String> chatNameList = Arrays.stream(unRefinedCommand
                                        .substring(startOfChannels, stopOfChannels)
                                        .split(","))
                                .map(String::trim)
                                .toList();

                        String[] keywords = Arrays.stream(unRefinedCommand
                                        .substring(startOfWords, stopOfWords)
                                        .trim()
                                        .split(","))
                                .map(String::trim)
                                .toArray(String[]::new);
//
                        int numberOfPages = startOfPage == 0 ? 1 : Integer.parseInt(unRefinedCommand
                                .substring(startOfPage, stopOfPage)
                                .trim());

                        int size = stopOfSize == 0 ? 10 : Integer.parseInt(unRefinedCommand
                                .substring(startOfSize, stopOfSize)
                                .trim());

//                        String[] chatNameList = commands[1].split(",");
//                        String[] keywords = commands[2].split(",");
//                        int size = 0, numberOfPages = 0;
//                        if (commands.length > 3)
//                            size = Integer.parseInt(commands[3]);
//                        if (commands.length > 4)
//                            numberOfPages = Integer.parseInt(commands[4]);

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

//    String[] extractKeywords(String input) {
//        String trimmed = input.trim();
//        int numberOfDQ = 0, stop = 0;
//        int start = trimmed.indexOf("\"");
//        for (int i = 0; i < trimmed.length(); i++) {
//            if (trimmed.charAt(i) == '"') {
//                stop = i;
//                numberOfDQ++;
//            }
//            if (numberOfDQ %2==0)
//                return input.
//
//        }
//    }

    // ---------- Console printing ----------
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    static void appendLine(JTextPane pane, StyledString s) {
//        SwingUtilities.invokeLater(() -> {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), s.text + "\n", style(s));
            pane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
//        });
    }

    //append link button with line break
    static void appendLinkButtonLB(JTextPane pane, String link) {

        JButton linkButton = getLikButton();

        addLinkListener(link, linkButton);
        console.insertComponent(linkButton);

        StyledString styledLB = new StyledString("\n}", Color.WHITE, false);

        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), styledLB.text, style(styledLB));
            pane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private static JButton getLikButton() {
        JButton linkButton = new JButton("Link");
        Color normal = new Color(4, 98, 152);
        Color hover = new Color(8, 72, 119);
        Color press = new Color(5, 36, 58);
        linkButton.setBackground(normal);
        linkButton.setContentAreaFilled(false);
        linkButton.setForeground(new Color(211, 211, 211));
        linkButton.setMargin(new Insets(1, 5, 1, 5));
        linkButton.setOpaque(true);
        linkButton.setRolloverEnabled(true);
        linkButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        linkButton.getModel().addChangeListener(e -> {
            ButtonModel m = linkButton.getModel();
            linkButton.setBackground(m.isPressed() ? press : (m.isRollover() ? hover : normal));
        });
        return linkButton;
    }

    static void addLinkListener(String link, JButton linkButton) {
        linkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 1) {
                    try {
                        Desktop.getDesktop().browse(new URI(link));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    static StyledString formattedErrorLine(String input) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(112, 15, 15), true);
    }

    static StyledString formattedWarnLine(String input) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(158, 155, 8), true);
    }

    static StyledString formattedUserLine(String input) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(31, 115, 3), true);
    }

    static StyledString formattedOutputLine(String out) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " > " + out, Color.WHITE, false);
    }

    static StyledString formattedSystemLine(String msg) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " # " + msg, Color.CYAN, false);
    }

    private static StyledString formattedErrorLine(Throwable ex) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " ! " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()),
                Color.RED, false);
    }

    record StyledString(String text, Color color, boolean bold) {
    }

    private static AttributeSet style(StyledString s) {
        SimpleAttributeSet as = new SimpleAttributeSet();
        StyleConstants.setForeground(as, s.color());
        StyleConstants.setBold(as, s.bold());
        StyleConstants.setFontFamily(as, "Tahoma");
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
