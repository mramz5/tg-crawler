package org.drinkless.tdlib.ui;

import lombok.Getter;
import org.drinkless.tdlib.Crawler;
import org.drinkless.tdlib.util.History;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.drinkless.tdlib.Crawler.gotInput;
import static org.drinkless.tdlib.Crawler.waitForInput;

public class TerminalWindow {
    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private static final Color BLACK_GREY = new Color(30, 30, 30);
    private static final Font terminalFont = new Font("Tahoma", Font.BOLD, 15);
    @Getter
    private final JTextPane console = new JTextPane();
    @Getter
    private final JTextField input = new JTextField();
    private final JFrame frame = new JFrame("tg-crawler");
    private final JScrollPane scroll = new JScrollPane(console);
    private final History history = new History(input);

    static void addInputOrientationListener(JTextField input) {
        input.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "toggleRTL");
        input.getActionMap().put("toggleRTL", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean rtl = input.getComponentOrientation().isLeftToRight();
                input.setComponentOrientation(rtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
            }
        });
    }

    private static JButton getLikButton() {
        JButton linkButton = new JButton("Browse in telegram");

        Color normal = new Color(4, 98, 152);
        Color hover = new Color(8, 72, 119);
        Color press = new Color(5, 36, 58);

        linkButton.setBackground(normal);
        linkButton.setContentAreaFilled(false);
        linkButton.setForeground(new Color(211, 211, 211));
        linkButton.setMargin(new Insets(1, 5, 1, 5));
        linkButton.setOpaque(true);
        linkButton.setRolloverEnabled(true);
        linkButton.setFont(terminalFont);

        linkButton.getModel().addChangeListener(e -> {
            ButtonModel m = linkButton.getModel();
            linkButton.setBackground(m.isPressed() ? press : (m.isRollover() ? hover : normal));
        });
        return linkButton;
    }

    public static StyledString formattedErrorLine(String input) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(112, 15, 15), true);
    }

    public static StyledString formattedWarnLine(String input) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(158, 155, 8), true);
    }

    public static StyledString formattedUserLine(String input) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " $ " + input, new Color(31, 115, 3), true);
    }

    public static StyledString formattedOutputLine(String out) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " > " + out, Color.WHITE, false);
    }

    public static StyledString formattedSystemLine(String msg) {
        String ts = "\n[" + LocalTime.now().format(TS) + "]";
        return new StyledString(ts + " # " + msg, Color.CYAN, false);
    }

    static void addInputClearListener(JTextField input, JTextPane console) {
        input.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "clear");
        input.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                console.setText("");
            }
        });
    }

    public void init() {
        SwingUtilities.invokeLater(this::createAndShow);
    }

    private JFrame getFrame() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(900, 560);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
        return frame;
    }

    private JTextField setTextField() {
        input.setFont(terminalFont);
        input.setBackground(BLACK_GREY);
        input.setForeground(Color.WHITE);
        input.setCaretColor(Color.WHITE);
        input.requestFocusInWindow();
        input.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return input;
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private void getBottom(JFrame frame, JTextField input) {
        JLabel prompt = new JLabel(">");
        prompt.setFont(terminalFont);
        prompt.setForeground(new Color(31, 115, 3));
        prompt.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBackground(BLACK_GREY);
        bottom.add(prompt, BorderLayout.WEST);
        bottom.add(input, BorderLayout.CENTER);

        frame.add(bottom, BorderLayout.SOUTH);
    }

    private JTextPane setConsole() {
        console.setEditable(false);
        console.setFont(terminalFont);
        console.setBackground(BLACK_GREY);
        console.setForeground(BLACK_GREY);
        DefaultCaret caret = (DefaultCaret) console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        int topMargin = (int) (screenSize.height * 0.01);
        int leftMargin = (int) (screenSize.width * 0.01);
        int rightMargin = (int) (screenSize.width * 0.3);

        console.setMargin(new Insets(topMargin, leftMargin, topMargin, rightMargin));
        return console;
    }

    private void getScrollPane() {
        scroll.setBorder(BorderFactory.createEmptyBorder());
        frame.add(scroll, BorderLayout.CENTER);
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

    private void addInputHistoryListener() {
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> {
                        if (!history.isEmpty()) {
                            history.setHI_VK_UP();
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_DOWN -> {
                        if (!history.isEmpty()) {
                            history.setHI_VK_DOWN();
                            e.consume();
                        }
                    }
                }
            }
        });
    }

    void handleUserInputs(JTextField input,
                          History history) {
        // Enter → send
        input.addActionListener(e -> {
            String cmd = input.getText();
            if (cmd == null) cmd = "";
            String trimmed = cmd.trim();

            if (!trimmed.isEmpty()) {
                history.add(trimmed);

//                Crawler.setConsole(console);
                /* Function<String, CompletableFuture<Void>> handler = s->*/
                CompletableFuture.runAsync(() -> {
                    waitForInput.lock();
                    gotInput.signal();
                    waitForInput.unlock();

                    Crawler.handleInput(this, trimmed);
                    input.setText("");
                }, Executors.newFixedThreadPool(
                        1,
                        r -> {
                            Thread t = new Thread(r, "terminal-io");
                            t.setDaemon(true);
                            return t;
                        }));
            }

            if (isProbablyRTL(trimmed)) {
                input.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            } else if (!trimmed.isEmpty()) {
                input.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            }
            input.requestFocusInWindow();
        });
    }

    private void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JFrame frame = getFrame();
            JTextField input = setTextField();
            getBottom(frame, input);
            JTextPane textPane = setConsole();
            getScrollPane();

            addInputHistoryListener();
            addInputClearListener(input, textPane);
            addInputOrientationListener(input);
            handleUserInputs(input, history);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void appendLine(StyledString s) {
        StyledDocument doc = console.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), s.text + "\n", style(s));
            console.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    //append link button with line break
    public void appendLinkButtonLB(String link) {

        JButton linkButton = getLikButton();

        addLinkListener(link, linkButton);
        console.insertComponent(linkButton);

        StyledString styledLB = new StyledString("\n", Color.WHITE, false);

        StyledDocument doc = console.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), styledLB.text, style(styledLB));
            console.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
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
