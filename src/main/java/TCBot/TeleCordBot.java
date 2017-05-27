package main.java.TCBot;


import com.google.common.collect.HashBiMap;
import com.thoughtworks.xstream.XStream;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import com.google.common.collect.BiMap;


import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class TeleCordBot implements DiscordBot.DiscordMessageListener, TelegramBot.TelegramMessageListener, Serializable {

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private BiMap<String, String> pairedChannels = HashBiMap.create();
    private Map<String, String> tempMap = new HashMap();
    private XStream xStream = new XStream();
    private String pairedChannelsFilePath = "PairedChannels.xml";
    private File fileChecker = new File(pairedChannelsFilePath);

    private String password;
    private String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private String discordID;

    public static void main(String[] args) {


        TeleCordBot telecordBot = new TeleCordBot();

        //Start the Telegram and Discord bots
        telecordBot.startDiscordBot();
        telecordBot.startTelegramBot();
    }

    private void startTelegramBot() {


        if (fileChecker.exists()) {
            //Read in previous channel pairs from file
            try {
                readFile();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

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
            discordID = pairedChannels.get(password);
            System.out.println("Received password = " + password);
            MessageChannel discordChannel = discordBot.getChannelFromID(discordID);

            if (discordChannel != null) {
                pairedChannels.remove(password);
                pairedChannels.put(channel, discordID);
                telegramReply(channel, "Telegram channel has been linked");

                try {
                    saveFileToXML();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else {
                telegramReply(channel, "No channel with password found");
            }


            //Begins the linking process, creates a password and then puts the password and channel pair into a Map
            //to later be linked to a discord channel
        } else if (message.getText().equalsIgnoreCase("link")) {
            password = RandomStringUtils.random(8, characters);
            pairedChannels.inverse().remove(channel);
            pairedChannels.put(password, channel);

            //Send the password to the telegram channel
            telegramReply(channel, "Type 'link " + password + "' into the Discord Channel to link.");


            //Remove the link between the current Telegram channel and its paired Discord channel if 'delink' is entered, inform the user
        } else if (message.getText().equalsIgnoreCase("delink")) {
            telegramReply(channel, "The link with the Discord channel has been removed.");
            pairedChannels.remove(channel);

            try {
                saveFileToXML();
            } catch (IOException e) {
                e.printStackTrace();
            }


            //Send the content of the message to the Discord channel that is paired with the Telegram channel, if it exists
        } else {
            try {
                System.out.println("Paired Discord Channel: " + pairedChannels.get(channel));
                discordID = pairedChannels.get(channel);
                discordBot.sendMessageToChannelWithText(discordBot.getChannelFromID(discordID), (author + ": " + message.getText()));

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onDiscordMessageReceived(String message, TextChannel channel, String author) {
        System.out.println("Discord message received! " + message);

        discordID = channel.getId();

        if (message.matches("([Ll][Ii][Nn][Kk]) ........")) {
            password = message.substring(5);
            String telegramChannel = pairedChannels.get(password);
            System.out.println(telegramChannel);
            System.out.println(channel);

            if (telegramChannel != null) {
                pairedChannels.put(telegramChannel, discordID);
                pairedChannels.remove(password);
                System.out.println("Channel has been linked to Telegram channel");
                channel.sendMessage("Channel has been linked to Telegram channel ").queue();

                try {
                    saveFileToXML();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else {
                channel.sendMessage("No channel with password found").queue();
            }


        } else if (message.equalsIgnoreCase("delink")) {
            channel.sendMessage("Channel has been delinked from Telegram channel ").queue();
            pairedChannels.inverse().remove(discordID);
            try {
                saveFileToXML();
            } catch (IOException e) {
                e.printStackTrace();
            }


        } else if (message.equalsIgnoreCase("link")) {
            password = RandomStringUtils.random(8, characters);
            pairedChannels.inverse().remove(discordID);
            pairedChannels.put(password, discordID);
            channel.sendMessage("Type 'link " + password + "' into the Telegram Chat to link").queue();


        } else {
            System.out.println("Sending message to telegram");
            try {
                //Send message to the telegram chanel
                System.out.println("Telegram paired Channel: " + pairedChannels.inverse().get(discordID));

                telegramBot.sendMessageToChannelWithText(pairedChannels.inverse().get(discordID), message, author);

            } catch (TelegramApiException e) {
                System.out.println("Can't find paired Channel");
                e.printStackTrace();
            }
        }
    }

    public void telegramReply(String channel, String text) {
        SendMessage reply = new SendMessage().setChatId(channel).setText(text);

        try {
            telegramBot.sendMessage(reply);
        } catch (TelegramApiException e) {
            System.out.println("Error with telegram bot replying");
            e.printStackTrace();
        }
    }

    private void readFile() throws IOException, ClassNotFoundException {

        tempMap = (Map<String, String>) xStream.fromXML(new FileInputStream(pairedChannelsFilePath));
        pairedChannels.putAll(tempMap);
    }

    private void saveFileToXML() throws IOException {
        tempMap.clear();
        tempMap.putAll(pairedChannels);
        xStream.alias("map", java.util.HashMap.class);
        String xml = xStream.toXML(tempMap);
        FileWriter writer = new FileWriter(pairedChannelsFilePath);
        writer.write(xml);
        writer.close();


    }


}
