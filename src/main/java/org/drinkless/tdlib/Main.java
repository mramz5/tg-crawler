package org.drinkless.tdlib;

import org.drinkless.tdlib.ui.TerminalWindow;

public class Main {
    public static void main(String[] args) {
        TerminalWindow main = new TerminalWindow();
        main.init();
        Crawler.run(main);
    }
}