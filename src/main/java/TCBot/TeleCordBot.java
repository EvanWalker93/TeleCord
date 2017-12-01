package main.java.TCBot;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;
import main.java.TCBot.model.DiscordChannel;
import main.java.TCBot.model.MessageModel;
import main.java.TCBot.model.TelegramChannel;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;


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

        String telegramChannelId = update.getMessage().getChatId().toString();
        SendMessage message = new SendMessage().setChatId(update.getMessage().getChatId()).setText(update.getMessage().getText());
        String messageText = message.getText();
        String author = update.getMessage().getFrom().getUserName();
        String fileName = "";

        if (update.getMessage().hasDocument()) {
            fileName = file.getFileName();
        } else if (update.getMessage().hasPhoto()) {
            fileName = UUID.randomUUID().toString() + ".jpg";
        }


        System.out.println("TeleCord bot: Received message from Telegram bot: " + message.getText());
        System.out.println("TeleCord bot: Telegram Chat ID: " + message.getChatId());

        TextChannel discordChannel = null;
        String discordChannelId = pairedChannels.get(telegramChannelId);

        if (discordChannelId != null) {
            discordChannel = discordBot.getChannelFromID(discordChannelId);

        }

        //Checks if there is a channel linked to the message origin channel
        if (telegramHandshake(message, telegramChannelId)) {
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
                //If the Discord channel is already in the db, fetch its password
                if (db.getTelegramChannelObj(telegramChannelId).get() != null) {
                    password = db.getTelegramChannelObj(telegramChannelId).get().getPassword();
                    telegramReply(telegramChannelId, "Type the following password into the Discord channel you wish to link:");
                    telegramReply(telegramChannelId, "/link " + password);
                } else {
                    password = generatePassword();
                    TelegramChannel telegramChannel = new TelegramChannel(telegramChannelId);
                    telegramChannel.setPassword(password);
                    db.addChannelToDB(telegramChannel);
                    telegramReply(telegramChannelId, "Type the following password into the Telegram channel you wish to link:");
                    telegramReply(telegramChannelId, "/link " + password);
                }
                break;

            case "/delink":
                telegramReply(telegramChannelId, "The link with the Discord channel has been removed.");
                pairedChannels.remove(telegramChannelId);
                saveFileToXML();
                break;

            default:
                if (pairedChannels.get(telegramChannelId) != null) {
                    discordBot.sendMessageToChannel(discordChannel, (author + ": " + messageText), file, fileName);
                    Date date = new Date();
                    MessageModel messageModel = new MessageModel(author, messageText, telegramChannelId, "Telegram", date);
                    db.addMessageToDB(messageModel);
                    //db.addMessage(author, message.getText(), ZonedDateTime.now().toString(), telegramChannelId, "Telegram", file);
                }
        }
    }

    @Override
    public void onDiscordMessageReceived(String message, TextChannel channel, String author, FileHandler file) throws IOException, TelegramApiException {
        System.out.println("TeleCord bot: Received message from Discord bot");
        System.out.println("TeleCord bot: Channel ID: " + channel.getId());
        String discordChannelId = channel.getId();
        String telegramChannel = pairedChannels.inverse().get(discordChannelId);

        //Check if the message is linking with a password in the message
        if (discordHandshake(message, channel)) {
            return;
        }

        //Switch to decide what to do based on received message content
        switch (message.toLowerCase()) {

            case "/link":
                //If the Discord channel is already in the db, fetch its password
                if (db.getDiscordChannelObj(discordChannelId).get() != null) {
                    password = db.getDiscordChannelObj(discordChannelId).get().getPassword();
                    channel.sendMessage("Type the following password into the Telegram channel you wish to link:").queue();
                    channel.sendMessage("/link " + password).queue();
                } else {
                    password = generatePassword();
                    DiscordChannel discordChannel = new DiscordChannel(discordChannelId);
                    discordChannel.setPassword(password);
                    db.addChannelToDB(discordChannel);
                    channel.sendMessage("Type the following password into the Telegram channel you wish to link:").queue();
                    channel.sendMessage("/link " + password).queue();
                }
                break;

            case "/delink":
                channel.sendMessage("Channel has been delinked from Telegram channel ").queue();
                pairedChannels.inverse().remove(discordChannelId);
                saveFileToXML();
                break;

            default:
                if (pairedChannels.inverse().get(discordChannelId) != null) {
                    telegramSendMessage(telegramChannel, message, author, file);
                    //db.addMessage(author, message, ZonedDateTime.now().toString(), channel.getName(), "Discord", file);
                    Date date = new Date();
                    MessageModel messageModel = new MessageModel(author, message, discordChannelId, "Discord", date);
                    db.addMessageToDB(messageModel);
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

    private boolean discordHandshake(String message, TextChannel discordChannelId) throws IOException {
        message = message.replace("'", "");

        if (message.matches("/([Ll][Ii][Nn][Kk])\\s[^\\s\\\\].*")) {
            password = message.substring(6);


            TelegramChannel telegramChannel = db.getTelegramChannelFromPassword(password);
            DiscordChannel discordChannel = db.getDiscordChannelObj(discordChannelId.getId()).get();

            if (telegramChannel.getChannelId() != null) {
                //If the Discord channel is not already in the DB, create a new channel obj,
                //set the parameters, and insert it into the DB.
                //Else if the Discord channel is already in the DB, add the Telegram server
                //to the Discord channel's Telegram list.
                //Else send a message stating that the password is incorrect.
                if (db.getDiscordChannelObj(discordChannelId.getId()).get() == null) {
                    DiscordChannel channel = new DiscordChannel();
                    channel.setChannelId(discordChannelId.getId());
                    channel.setPassword(generatePassword());
                    List<String> list = new ArrayList<>();
                    list.add(telegramChannel.getChannelId());
                    channel.setTelegramChannels(list);

                    db.addChannelToDB(channel);
                    db.addToTelegramList(telegramChannel, discordChannelId.getId());
                    discordChannelId.sendMessage("Channel has been linked to Telegram channel ").queue();
                } else if (discordChannel.getChannelId() != null) {
                    db.addToDiscordList(discordChannel, telegramChannel.getChannelId());
                    db.addToTelegramList(telegramChannel, discordChannelId.getId());
                    discordChannelId.sendMessage("Channel has been linked to Telegram channel ").queue();
                }

            } else {
                discordChannelId.sendMessage("No channel with password found").queue();
            }

        } else {
            return false;
        }

        return true;
    }

    private boolean telegramHandshake(SendMessage sendMessage, String telegramChannelId) throws TelegramApiException, IOException {
        String discordChannelId;
        String message = sendMessage.getText().replace("'", "");

        if (message.matches("/([Ll][Ii][Nn][Kk])\\s[^\\s\\\\].*")) {

            password = message.substring(6);
            discordChannelId = pairedChannels.get(password);
            System.out.println("TeleCord bot: Received password = " + password);
            MessageChannel discordChannel = discordBot.getChannelFromID(discordChannelId);

            if (discordChannel != null) {
                pairedChannels.remove(password);
                pairedChannels.put(telegramChannelId, discordChannelId);
                telegramReply(telegramChannelId, "Telegram channel has been linked");
                saveFileToXML();

                System.out.println("DB: " + db.getDiscordChannelFromPassword(password));
            } else {
                telegramReply(telegramChannelId, "No channel with password found");
            }

        } else {
            return false;
        }
        return true;
    }

    private String generatePassword() {
        password = RandomStringUtils.random(8, characters);
        while (!db.uniquePassword(password)) {
            password = RandomStringUtils.random(8, characters);
        }
        return password;
    }
}

