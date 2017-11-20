package main.java.TCBot;

import java.util.List;

public class DiscordChannel {

    private String channel;
    private String password;
    private List telegramChannels;
    private List discordChannels;

    public DiscordChannel(String channel, String password, List telegramChannels, List discordChannels) {
        this.channel = channel;
        this.password = password;
        this.telegramChannels = telegramChannels;
        this.discordChannels = discordChannels;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List getTelegramChannels() {
        return telegramChannels;
    }

    public void setTelegramChannels(List telegramChannels) {
        this.telegramChannels = telegramChannels;
    }

    public List getDiscordChannels() {
        return discordChannels;
    }

    public void setDiscordChannels(List discordChannels) {
        this.discordChannels = discordChannels;
    }
}
