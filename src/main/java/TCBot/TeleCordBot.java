package main.java.TCBot;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class TeleCordBot implements DiscordBot.DiscordMessageListener, TelegramBot.TelegramMessageListener, Serializable {

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private DatabaseHandler db = new DatabaseHandler();
    private BiMap<String, String> pairedChannels = HashBiMap.create();
    private Map<String, String> tempMap = new HashMap();
    private XStream xStream = new XStream();
    NamedMapConverter namedMapConverter = new NamedMapConverter(xStream.getMapper(), "Pair", "Discord", String.class, "Telegram", String.class);

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
        xStream.registerConverter(namedMapConverter);

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
        System.out.print("Loading Discord bot...");

        try {
            discordBot = new DiscordBot(this);
            System.out.println(" Discord bot loaded!");
        } catch (Exception e) {
            System.out.println(" Discord bot failed to load!");
            e.printStackTrace();
        }
    }

    @Override
    public void onTelegramMessageReceived(Update update, FileHandler file) throws TelegramApiException, IOException {

        String channel = update.getMessage().getChatId().toString();
        SendMessage message = new SendMessage().setChatId(update.getMessage().getChatId()).setText(update.getMessage().getText());
        String author = update.getMessage().getFrom().getUserName();
        String fileName = "";
        if (update.getMessage().hasDocument()) {
            fileName = update.getMessage().getDocument().getFileName();
        } else if (update.getMessage().hasPhoto()) {
            fileName = UUID.randomUUID().toString() + ".jpg";
        }


        System.out.println("TeleCord bot: Received message from Telegram bot: " + message.getText());
        System.out.println("TeleCord bot: Telegram Chat ID: " + message.getChatId());

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
        if (message.getText() == null) {
            message.setText("");
        }

        switch (message.getText().toLowerCase()) {
            case "/link":
                password = Integer.toString(channel.hashCode());
                pairedChannels.inverse().remove(channel);
                pairedChannels.put(password, channel);
                telegramReply(channel, "Type the following password into the Telegram channel you wish to link:");
                telegramReply(channel, "'link " + password + "'");
                saveFileToXML();

                db.addTelegramChannelToDB(channel, null, null);
                break;

            case "/delink":
                telegramReply(channel, "The link with the Discord channel has been removed.");
                pairedChannels.remove(channel);
                saveFileToXML();
                break;

            default:
                if (pairedChannels.get(channel) != null) {
                    discordBot.sendMessageToChannel(discordChannel, (author + ": " + message.getText()), file, fileName);
                    db.addMessage(author, message.getText(), ZonedDateTime.now().toString(), channel, 1);
                }
        }
    }

    @Override
    public void onDiscordMessageReceived(String message, TextChannel channel, String author, FileHandler file) throws IOException, TelegramApiException {
        System.out.println("TeleCord bot: Received message from Discord bot");
        System.out.println("TeleCord bot: Channel ID: " + channel.getId());
        discordID = channel.getId();
        String telegramChannel = pairedChannels.inverse().get(discordID);

        //Check if the message is linking with a password in the message
        if (discordHandshake(message, channel)) {
            return;
        }

        //Switch to decide what to do based on received message content
        switch (message.toLowerCase()) {

            case "/link":
                password = Integer.toString(channel.toString().hashCode());
                pairedChannels.inverse().remove(discordID);
                pairedChannels.put(password, discordID);
                channel.sendMessage("Type the following password into the Telegram channel you wish to link:").queue();
                channel.sendMessage("'link " + password + "'").queue();

                db.addDiscordChannelToDB(channel.getId(), null, null);
                break;

            case "/delink":
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

    private String getFileType(FileHandler file) {
        String extension = file.getFileExtension();

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

    private void telegramSendMessage(String telegramChannel, String message, String author, FileHandler file) {
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

        if (message.replace("'", "").matches("/([Ll][Ii][Nn][Kk])\\s[^\\s\\\\].*")) {
            password = message.replace("'", "").substring(6);
            System.out.println(password);
            String telegramChannel = pairedChannels.get(password);

            if (telegramChannel != null) {
                pairedChannels.put(telegramChannel, discordID);
                pairedChannels.remove(password);
                System.out.println("TeleCord bot: Channel has been linked to Telegram channel");
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


        if (message.getText() != null && message.getText().replace("'", "")
                .matches("/([Ll][Ii][Nn][Kk])\\s[^\\s\\\\].*")) {

            password = message.getText().replace("'", "").substring(5);
            discordID = pairedChannels.get(password);
            System.out.println("TeleCord bot: Received password = " + password);
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

