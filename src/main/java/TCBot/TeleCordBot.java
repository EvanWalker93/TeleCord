package main.java.TCBot;


import com.google.common.collect.HashBiMap;
import com.thoughtworks.xstream.XStream;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import com.google.common.collect.BiMap;



import java.io.*;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;


public class TeleCordBot implements DiscordBot.DiscordMessageListener, TelegramBot.TelegramMessageListener, Serializable {

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private DatabaseHandler db = new DatabaseHandler();
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

        //Start the Telegram and Discord bots, and connect to the MongoDb
        telecordBot.readPairedChannelsXml();
        telecordBot.startDiscordBot();
        telecordBot.startTelegramBot();
        telecordBot.db.init();
    }


    private void readPairedChannelsXml() {
        if (fileChecker.exists()) {
            //Read in previous channel pairs from file
            try {
                readFile();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void startTelegramBot() {
        System.out.print("Loading Telegram bot...");


        ApiContextInitializer.init();
        TelegramBotsApi telegramApi = new TelegramBotsApi();
        telegramBot = new TelegramBot(this);
        try {
            telegramApi.registerBot(telegramBot);
            System.out.println(" Telegram bot loaded!");
        } catch (TelegramApiException e) {
            System.out.println(" Telegram bot failed to load!");
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
    public void onTelegramMessageReceived(SendMessage message, String channel, String author, File file) throws TelegramApiException, IOException {
        System.out.println("Telegram message received! " + message.getText());
        System.out.println("Chat ID: " + message.getChatId());
        System.out.println("Chat Channel: " + channel);

        TextChannel discordChannel = null;
        discordID = pairedChannels.get(channel);

        if (discordID != null) {
            discordChannel = discordBot.getChannelFromID(discordID);

        }

        //Checks if there is a channel linked to the message origin channel
        if (telegramHandshake(message, channel)) {
            return;
        }

        //Check if message containing 'link' also includes a password
        //Get the channel with the key that equals the password
        //Change the key to the current telegram channel
        switch (message.getText().toLowerCase()) {
            case "link":
                password = RandomStringUtils.random(8, characters);
                pairedChannels.inverse().remove(channel);
                pairedChannels.put(password, channel);
                telegramReply(channel, "Type the following password into the Telegram channel you wish to link:");
                telegramReply(channel, "'link " + password + "'");
                break;

            case "delink":
                telegramReply(channel, "The link with the Discord channel has been removed.");
                pairedChannels.remove(channel);
                saveFileToXML();
                break;

            default:
                if (pairedChannels.get(channel) != null) {
                    discordBot.sendMessageToChannel(discordChannel, (author + ": " + message.getText()), file);
                    db.addMessage(author, message.getText(), ZonedDateTime.now().toString(), channel, 1);
                }
        }
    }

    @Override
    public void onDiscordMessageReceived(String message, TextChannel channel, String author, File file) throws IOException, TelegramApiException {
        System.out.println("Discord message received!");
        discordID = channel.getId();
        String telegramChannel = pairedChannels.inverse().get(discordID);

        //Check if the message is linking with a password in the message
        if (discordHandshake(message, channel)) {
            return;
        }

        //Switch to decide what to do based on received message content
        switch (message.toLowerCase()) {

            case "link":
                password = RandomStringUtils.random(8, characters);
                pairedChannels.inverse().remove(discordID);
                pairedChannels.put(password, discordID);
                channel.sendMessage("Type the following password into the Telegram channel you wish to link:").queue();
                channel.sendMessage("'link " + password + "'").queue();
                break;

            case "delink":
                channel.sendMessage("Channel has been delinked from Telegram channel ").queue();
                pairedChannels.inverse().remove(discordID);
                saveFileToXML();
                break;

            default:
                if (pairedChannels.inverse().get(discordID) != null) {
                    telegramSendMessage(telegramChannel, message, author, file);
                    db.addMessage(author, message, ZonedDateTime.now().toString(), channel.getName(), 0);
                }

        }

    }


    //---------------------------------------------------------------------------------------------------
    private void telegramReply(String channel, String text) throws TelegramApiException {
        SendMessage reply = new SendMessage().setChatId(channel).setText(text);
        telegramBot.sendMessage(reply);
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

    private String getFileType(File file) {
        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        String fileType;
        switch (extension) {
            case "jpg":
            case "png":
                fileType = "image";
                break;
            case "mp4":
            case "webm":
            case "gif":
                fileType = "video";
                break;
            default:
                fileType = "other";
                break;
        }

        return fileType;
    }

    private void telegramSendMessage(String telegramChannel, String message, String author, File file) {
        //Checks file extension if there is one, sends file based on extension.
        // If no file, send message with no attachment.
        if (file != null) {
            switch (getFileType(file)) {
                case "image":
                    telegramBot.sendPhoto(telegramChannel, message, author, file);
                    break;
                case "video":
                    telegramBot.sendVideo(telegramChannel, message, author, file);
                    break;
                case "other":
                    telegramBot.sendDocument(telegramChannel, message, author, file);
                    break;
            }
        } else {
            try {
                telegramBot.sendMessageToChannel(telegramChannel, message, author);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean discordHandshake(String message, TextChannel channel) throws IOException {

        if (message.replace("'", "").matches("([Ll][Ii][Nn][Kk]) ........")) {
            password = message.replace("'", "").substring(5);
            String telegramChannel = pairedChannels.get(password);

            if (telegramChannel != null) {
                pairedChannels.put(telegramChannel, discordID);
                pairedChannels.remove(password);
                System.out.println("Channel has been linked to Telegram channel");
                channel.sendMessage("Channel has been linked to Telegram channel ").queue();
                saveFileToXML();

            } else {
                channel.sendMessage("No channel with password found").queue();
            }

        } else {
            return false;
        }

        return true;
    }

    private boolean telegramHandshake(SendMessage message, String channel) throws TelegramApiException, IOException {


        if (message.getText() != null && message.getText().replace("'", "").matches("([Ll][Ii][Nn][Kk]) ........")) {
            password = message.getText().replace("'", "").substring(5);
            discordID = pairedChannels.get(password);
            System.out.println("Received password = " + password);
            MessageChannel discordChannel = discordBot.getChannelFromID(discordID);

            if (discordChannel != null) {
                pairedChannels.remove(password);
                pairedChannels.put(channel, discordID);
                telegramReply(channel, "Telegram channel has been linked");
                saveFileToXML();


            } else {
                telegramReply(channel, "No channel with password found");
            }

        } else {
            return false;
        }
        return true;
    }
}

