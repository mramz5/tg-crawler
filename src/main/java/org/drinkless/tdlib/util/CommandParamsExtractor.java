package org.drinkless.tdlib.util;

import java.util.Arrays;
import java.util.List;

import static java.lang.Character.isWhitespace;
import static org.drinkless.tdlib.util.ChatUtil.getChatId;

public class CommandParamsExtractor {

    public static void extractSMParams(String unRefinedCommand, long[] limit, String[] message) {
        String[] splitted = unRefinedCommand.split("\\s+");
        limit[0] = getChatId(splitted[0]);
        message[0] = splitted[1];
    }

    public static void extractGCSParams(String unRefinedCommand, int[] limit) {
        String trimmed = unRefinedCommand.trim();
        if (!trimmed.isEmpty())
            limit[0] = Integer.parseInt(trimmed);
    }

    public static void extractSCWParams(String unRefinedCommand,
                                        List<String> chatNameList,
                                        List<String> keywords,
                                        int[] numberOfPages,
                                        int[] size) {

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
                stopOfChannels = (i == startOfWords ? i - 7 : i == unRefinedCommand.length() - 1 ? i : i - 6); // i-6 is for both page and size
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


        Arrays.stream(unRefinedCommand
                        .substring(startOfChannels, stopOfChannels)
                        .split(","))
                .map(String::trim)
                .collect(() -> chatNameList, List::add, List::addAll);

        Arrays.stream(unRefinedCommand
                        .substring(startOfWords, stopOfWords)
                        .trim()
                        .split(","))
                .map(String::trim)
                .collect(() -> keywords, List::add, List::addAll);

        numberOfPages[0] = startOfPage == 0 ? 1 : Integer.parseInt(unRefinedCommand
                .substring(startOfPage, stopOfPage)
                .trim());

        size[0] = stopOfSize == 0 ? 10 : Integer.parseInt(unRefinedCommand
                .substring(startOfSize, stopOfSize)
                .trim());

    }
}