package org.drinkless.tdlib.util;

import lombok.AllArgsConstructor;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class History {
    private final List<String> historyList = new ArrayList<>();
    private final JTextField input;
    private final int[] histIndex = {-1};

    public History(JTextField input) {
        this.input = input;
        FileHistoryManager fileHistoryManager = new FileHistoryManager(this);
        fileHistoryManager.loadHistory();
        Runtime.getRuntime().addShutdownHook(new Thread(fileHistoryManager::saveHistory));
    }

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

    @AllArgsConstructor
    static class FileHistoryManager {
        private static final String fileName = "hist";
        private History history;

        void loadHistory() {
            try {
                Path path = Path.of(fileName);
                if (path.toFile().exists()) {
                    List<String> list = Files.readAllLines(path);
                    for (String s : list)
                        history.add(s);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void saveHistory() {
            try {
                Files.write(Path.of(fileName),
                        history.historyList.reversed(),
                        TRUNCATE_EXISTING,
                        CREATE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
