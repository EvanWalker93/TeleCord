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
    private Map<String, String> telegramChannels = new HashMap<>();
    private Map<String, MessageChannel> discordChannels = new HashMap<>();
    private BiMap<String, MessageChannel> pairedChannels = HashBiMap.create();

    private String password;
    private String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static void main(String[] args) {
        TeleCordBot telecordBot = new TeleCordBot();


        // String telegramKey = botPairings.get("MY DISCORD ID"); // MY TELEGRAM ID

        telecordBot.startDiscordBot();
        telecordBot.startTelegramBot();
    }

    private void startTelegramBot() {
        //pairedChannels.clear();
        System.out.println(getClasspathString());
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
            discordChannels.remove(password);
            pairedChannels.put(channel, discordChannel);


            //Begins the linking process, creates a password and then puts the password and channel pair into a Map
            //to later be linked to a discord channel
        } else if (message.getText().equalsIgnoreCase("link")) {
            password = RandomStringUtils.random(8, characters);
            telegramChannels.put(password, channel);
            SendMessage reply = new SendMessage().setChatId(channel).setText
                    ("Typed 'link " + password + "' into the Discord Channel to link.");

            //Send the password to the telegram channel
            try {
                telegramBot.sendMessage(reply);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            //Remove the link between the current Telegram channel and its paired Discord channel if 'delink' is entered, inform the user
        } else if (message.getText().equalsIgnoreCase("delink")) {

            SendMessage reply = new SendMessage().setChatId(channel).setText("The link with the Discord channel " +
                    pairedChannels.get(channel).getName() + " has been removed.");
            pairedChannels.remove(channel);

            try {
                telegramBot.sendMessage(reply);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }


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

    public String getClasspathString() {
        StringBuffer classpath = new StringBuffer();
        ClassLoader applicationClassLoader = this.getClass().getClassLoader();
        if (applicationClassLoader == null) {
            applicationClassLoader = ClassLoader.getSystemClassLoader();
        }
        URL[] urls = ((URLClassLoader)applicationClassLoader).getURLs();
        for(int i=0; i < urls.length; i++) {
            classpath.append(urls[i].getFile()).append("\r\n");
        }

        return classpath.toString();
    }









    @Override
    public void onDiscordMessageReceived(String message, MessageChannel channel, String author) {
        System.out.println("Discord message received! " + message);

        if (message.matches("link ........")) {
            password = message.substring(5);
            String telegramChannel = telegramChannels.get(password);
            pairedChannels.put(telegramChannel, channel);
            telegramChannels.remove(password);


        } else if (message.equals("link")) {
            password = RandomStringUtils.random(8, characters);
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
}
