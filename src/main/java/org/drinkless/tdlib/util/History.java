package org.drinkless.tdlib.util;

import lombok.AllArgsConstructor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class History {
    private final List<String> historyList = new ArrayList<>();
    private final JTextField input;
    private final int[] histIndex = {-1};

    public void setInputText() {
        input.setText(historyList.get(histIndex[0]));
        input.setCaretPosition(input.getText().length());
    }

    public void setHI_VK_UP() {
        if (histIndex[0] < 0)
            histIndex[0] = historyList.size() - 1;
        else
            histIndex[0] = Math.max(0, histIndex[0] - 1);
        setInputText();
    }

    public void setHI_VK_DOWN() {
        if (histIndex[0] >= historyList.size() - 1) {
            histIndex[0] = -1;
            input.setText("");
        } else {
            histIndex[0]++;
            setInputText();
        }
    }

    public void add(String trimmed) {
        historyList.add(trimmed);
        histIndex[0] = -1;
    }

    public boolean isEmpty() {
        return historyList.isEmpty();
    }
}
