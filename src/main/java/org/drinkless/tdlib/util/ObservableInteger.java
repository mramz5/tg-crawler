package org.drinkless.tdlib.util;

import lombok.Getter;
import org.drinkless.tdlib.ui.TerminalWindow;


@Getter
public class ObservableInteger {
    private final int offset;
    private int value;

    public ObservableInteger(int value, int offset) {
        this.value = value;
        this.offset = offset;
    }

    public void onEvent(int incrementValue, TerminalWindow terminalWindow) {
        value += incrementValue;
//        terminalWindow.overwriteText(new TerminalWindow.StyledString("news count : " + value, BONE, false), offset);
    }
}