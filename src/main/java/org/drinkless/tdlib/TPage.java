package org.drinkless.tdlib;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TPage {
    public long fromMessageId;
    public int pageNumber;
    public int pageSize;
    public int offset;

    public static TPage of(long fromMessageId,int pageNumber, int pageSize,int offset) {
        return new TPage(fromMessageId,pageNumber, pageSize,offset);
    }
}
