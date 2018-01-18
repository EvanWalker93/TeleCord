package main.java.TCBot.model.channel;

import org.telegram.telegrambots.api.objects.Message;

public class TelegramChannel extends AbstractChannel {

    public TelegramChannel(Message message) {
        this.channelId = message.getChatId().toString();
        this.channelName = message.getChat().getTitle();
    }

}
