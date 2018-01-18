package main.java.TCBot.model.channel;

import net.dv8tion.jda.core.entities.Message;

public class DiscordChannel extends AbstractChannel {

    public DiscordChannel(Message message) {
        this.channelId = message.getTextChannel().getId();
        this.channelName = message.getTextChannel().getName();
    }

}
