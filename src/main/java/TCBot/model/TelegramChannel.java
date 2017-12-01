package main.java.TCBot.model;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.List;

@Entity("telegramChannels")
public class TelegramChannel {

    @Id
    private String channelId;
    private String password;
    private List<String> telegramChannels;
    private List<String> discordChannels;

    public TelegramChannel() {
    }

    public TelegramChannel(String channelId) {
        super();
        this.channelId = channelId;
        this.password = password;
        this.telegramChannels = telegramChannels;
        this.discordChannels = discordChannels;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getTelegramChannels() {
        return telegramChannels;
    }

    public void setTelegramChannels(List<String> telegramChannels) {
        this.telegramChannels = telegramChannels;
    }

    public List<String> getDiscordChannels() {
        return discordChannels;
    }

    public void setDiscordChannels(List<String> discordChannels) {
        this.discordChannels = discordChannels;
    }
}
