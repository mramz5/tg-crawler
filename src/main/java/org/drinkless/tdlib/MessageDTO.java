package org.drinkless.tdlib;

import com.github.mfathi91.time.PersianDateTime;

public class MessageDTO {
    public long id;
    public TdApi.FormattedText content;
    public PersianDateTime date;
    public String messageLink;

    @Override
    public String toString() {
        return firstPartToString() + secondPartToString();
    }

    public String firstPartToString() {
        String[] ts = date.toString().split("T");
        return "\n" +
                "\nid : " + id +
                " ,\ncontent : " + (content != null ? (content.text.replace("\n", "\n\t")) : "") +
                " ,\ndate=" + ts[0] + "-" + ts[1];
    }

    public String secondPartToString() {
        return " ,\nlink=" + messageLink +
                "\n";
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        MessageDTO that = (MessageDTO) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
