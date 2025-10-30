package org.drinkless.tdlib;

import com.github.mfathi91.time.PersianDateTime;

public class MessageDTO {
    public long id;
    public TdApi.FormattedText content;
    public PersianDateTime date;

    @Override
    public String toString() {
        String[] ts = date.toString().split("T");
        return "\n{" +
                "\n\tid : " + id +
                " ,\n\tcontent : " + (content != null ? (content.text.replace("\n", "\n\t")) : "") +
//                (content.linkPreview == null ? "" : " ,\n\tlink =" + content.linkPreview.title)) : "") +
                " ,\n\tdate=" + ts[0] + "-" + ts[1] +
                "\n}";
    }
}
