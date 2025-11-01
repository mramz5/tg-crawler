package org.drinkless.tdlib;

import com.github.mfathi91.time.PersianDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageMapper {

    public static MessageDTO toDTO(TdApi.Message message, String channelName) {
        long realId = message.id >> 20;
        MessageDTO dto = new MessageDTO();
        dto.id = realId;
        dto.content = message.content instanceof TdApi.MessageText ? ((TdApi.MessageText) message.content).text :
                message.content instanceof TdApi.MessagePhoto ? ((TdApi.MessagePhoto) message.content).caption :
                        message.content instanceof TdApi.MessageVideo ? ((TdApi.MessageVideo) message.content).caption :
                                message.content instanceof TdApi.MessageDocument ? ((TdApi.MessageDocument) message.content).caption :
                                        message.content instanceof TdApi.MessageVoiceNote ? ((TdApi.MessageVoiceNote) message.content).caption :
                                                message.content instanceof TdApi.MessageAnimation ? ((TdApi.MessageAnimation) message.content).caption :
                                                        message.content instanceof TdApi.MessagePremiumGiftCode ? ((TdApi.MessagePremiumGiftCode) message.content).text :
                                                                message.content instanceof TdApi.MessageGiftedPremium ? ((TdApi.MessageGiftedPremium) message.content).text :
                                                                        message.content instanceof TdApi.MessageAudio ? ((TdApi.MessageAudio) message.content).caption :
                                                                                message.content instanceof TdApi.MessagePaidMedia ? ((TdApi.MessagePaidMedia) message.content).caption :
                                                                                        null;
        dto.date = PersianDateTime.fromGregorian(new Timestamp(message.date * 1000L).toLocalDateTime());
        dto.messageLink = "https://t.me/" + channelName + "/" + realId;
        return dto;
    }
}
