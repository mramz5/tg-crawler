package org.drinkless.tdlib;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TPage {
    public long fromMessageId;
    public long numberOfPages;
    public int pageSize;
    public int offset;

    public static TPage of(long fromMessageId,int numberOfPages, int pageSize,int offset) {
        return new TPage(fromMessageId,numberOfPages, pageSize,offset);
    }
}
