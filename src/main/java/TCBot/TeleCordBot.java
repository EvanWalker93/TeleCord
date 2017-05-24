package TCBot;


import com.google.common.collect.HashBiMap;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import com.google.common.collect.BiMap;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class TeleCordBot implements DiscordBot.DiscordMessageListener, TelegramBot.TelegramMessageListener {

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private BiMap<String, String> telegramChannels = HashBiMap.create();
    private BiMap<String, MessageChannel> discordChannels = HashBiMap.create();
    private BiMap<String, MessageChannel> pairedChannels = HashBiMap.create();

    private String password;
    private String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static void main(String[] args) {
        TeleCordBot telecordBot = new TeleCordBot();

        //Start the Telegram and Discord bots
        telecordBot.startDiscordBot();
        telecordBot.startTelegramBot();
    }

    private void startTelegramBot() {
        ApiContextInitializer.init();
        TelegramBotsApi telegramApi = new TelegramBotsApi();
        telegramBot = new TelegramBot(this);
        try {
            telegramApi.registerBot(telegramBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startDiscordBot() {
        try {
            discordBot = new DiscordBot(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //discordBot = new TCB.TCBot.DiscordBot(DISCORD_KEY);


    @Override
    public void onTelegramMessageReceived(SendMessage message, String channel, String author) {
        System.out.println("Telegram message received! " + message.getText());
        System.out.println("Chat ID: " + message.getChatId());
        System.out.println("Chat Channel: " + channel);

        //Check if message containing 'link' also includes a password
        //Get the channel with the key that equals the password
        //Change the key to the current telegram channel
        if (message.getText().matches("link ........")) {
            password = message.getText().substring(5);
            System.out.println("Received password = " + password);
            MessageChannel discordChannel = discordChannels.get(password);

            if(discordChannel != null){
                discordChannels.remove(password);
                pairedChannels.put(channel, discordChannel);        telegramChannels.put(password, channel);

                //Send the password to the telegram channel
                telegramReply(channel, "Typed 'link " + password + "' into the Discord Channel to link.");

            }


            //Begins the linking process, creates a password and then puts the password and channel pair into a Map
            //to later be linked to a discord channel
        } else if (message.getText().equalsIgnoreCase("link")) {
            password = RandomStringUtils.random(8, characters);
            telegramChannels.inverse().remove(channel);



            //Remove the link between the current Telegram channel and its paired Discord channel if 'delink' is entered, inform the user
        } else if (message.getText().equalsIgnoreCase("delink")) {
            telegramReply(channel, "The link with the Discord channel has been removed.");
            pairedChannels.remove(channel);



            //Send the content of the message to the Discord channel that is paired with the Telegram channel, if it exists
        } else {
            try {
                System.out.println("Paired Discord Channel: " + pairedChannels.get(channel));
                discordBot.sendMessageToChannelWithText(pairedChannels.get(channel), (author + ": " + message.getText()));

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onDiscordMessageReceived(String message, MessageChannel channel, String author) {
        System.out.println("Discord message received! " + message);

        if (message.matches("(L|l)ink ........")) {
            password = message.substring(5);
            String telegramChannel = telegramChannels.get(password);
            System.out.println(telegramChannel);

            if(telegramChannel != null){
                pairedChannels.put(telegramChannel, channel);
                telegramChannels.remove(password);
                System.out.println("Channel has been linked to Telegram channel");
                channel.sendMessage("Channel has been linked to Telegram channel ").queue();
            }

            else{
                channel.sendMessage("No channel with password found").queue();
            }



        }else if(message.equalsIgnoreCase("delink")){
            channel.sendMessage("Channel has been delinked from Telegram channel ").queue();
            pairedChannels.inverse().remove(channel);



        } else if (message.equalsIgnoreCase("link")) {
            password = RandomStringUtils.random(8, characters);
            discordChannels.inverse().remove(channel);
            discordChannels.put(password, channel);
            channel.sendMessage("Typed 'link " + password + "' into the Telegram Chat to link").queue();


        } else {
            System.out.println("Sending message to telegram");
            try {
                //Send message to the telegram chanel
                System.out.println("Telegram paired Channel: " + pairedChannels.inverse().get(channel));

                telegramBot.sendMessageToChannelWithText(pairedChannels.inverse().get(channel), message, author);

            } catch (TelegramApiException e) {
                System.out.println("Can't find paired Channel");
                e.printStackTrace();
            }
        }
    }

    public void telegramReply(String channel, String text){
        SendMessage reply = new SendMessage().setChatId(channel).setText(text);

        try {
            telegramBot.sendMessage(reply);
        } catch (TelegramApiException e) {
            System.out.println("Error with telegram bot replying");
            e.printStackTrace();
        }
    }


}
