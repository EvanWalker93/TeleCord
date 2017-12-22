package main.java.TCBot;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import main.java.TCBot.model.DiscordChannel;
import main.java.TCBot.model.MessageModel;
import main.java.TCBot.model.TelegramChannel;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class TeleCordBot implements DiscordBot.DiscordMessageListener, TelegramBot.TelegramMessageListener, Serializable {

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private DatabaseHandler db = new DatabaseHandler();
    private BiMap<String, String> pairedChannels = HashBiMap.create();

    private String password;
    private String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static void main(String[] args) {
        TeleCordBot telecordBot = new TeleCordBot();

        //Start the Telegram and Discord bots, and connect to the MongoDb
        telecordBot.startDiscordBot();
        telecordBot.startTelegramBot();
        telecordBot.db.init();
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
        User user = update.getMessage().getFrom();
        String author = user.getUserName();


        if (user.getUserName() == null) {
            author = user.getFirstName() + " " + user.getLastName();
        }
        TelegramChannel telegramChannel = db.getTelegramChannelObj(telegramChannelId).get();

        System.out.println("TeleCord bot: Received message from Telegram bot: " + message.getText());
        System.out.println("TeleCord bot: Telegram Chat ID: " + message.getChatId());

        //Check if message containing 'link' also includes a password
        //Get the channel with the key that equals the password
        //Change the key to the current telegram channel
        if (message.getText() == null) {
            message.setText("");
        }

        //Checks if there is a channel linked to the message origin channel
        if (telegramHandshake(message, telegramChannelId)) {
            return;
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
                    TelegramChannel newTelegramChannel = new TelegramChannel(telegramChannelId);
                    newTelegramChannel.setPassword(password);
                    db.addChannelToDB(newTelegramChannel);
                    telegramReply(telegramChannelId, "Type the following password into the Telegram channel you wish to link:");
                    telegramReply(telegramChannelId, "/link " + password);
                }
                break;

            case "/delink":
                telegramReply(telegramChannelId, "The link with the Discord channel has been removed.");
                //db.removeChannelFromList(, );
                break;
            case "/password":
                telegramReply(telegramChannelId, "The password is: " + db.getTelegramChannelObj(telegramChannelId).get().getPassword());
                break;
            default:
                if (telegramChannel.getDiscordChannels() != null) {
                    for (String discordChannels : telegramChannel.getDiscordChannels()) {
                        TextChannel discordChannel = discordBot.getChannelFromID(discordChannels);
                        discordBot.sendMessageToChannel(discordChannel, (author + ": " + messageText), file);
                    }
                }
        }
    }

    @Override
    public void onDiscordMessageReceived(Message message, FileHandler file) throws IOException, TelegramApiException {
        TextChannel textChannel = message.getTextChannel();
        String messageText = message.getContent();
        String discordChannelId = textChannel.getId();
        String author = message.getAuthor().getName();

        System.out.println("TeleCord bot: Received message from Discord bot");
        System.out.println("TeleCord bot: Channel ID: " + discordChannelId);

        DiscordChannel discordChannel = db.getDiscordChannelObj(discordChannelId).get();

        //Check if the message is linking with a password in the message
        if (discordHandshake(messageText, textChannel)) {
            return;
        }

        //Switch to decide what to do based on received message content
        switch (messageText.toLowerCase()) {
            case "/link":
                //If the Discord channel is already in the db, fetch its password
                if (discordChannel != null) {
                    password = db.getDiscordChannelObj(discordChannelId).get().getPassword();
                    textChannel.sendMessage("Type the following password into the Telegram channel you wish to link:").queue();
                    textChannel.sendMessage("/link " + password).queue();
                } else {
                    password = generatePassword();
                    DiscordChannel newDiscordChannel = new DiscordChannel(discordChannelId);
                    newDiscordChannel.setPassword(password);
                    db.addChannelToDB(newDiscordChannel);
                    textChannel.sendMessage("Type the following password into the Telegram channel you wish to link:").queue();
                    textChannel.sendMessage("/link " + password).queue();
                }
                break;
            //TODO
            case "/delink":
                textChannel.sendMessage("Channel has been delinked from Telegram channel ").queue();
                break;
            case "/password":
                textChannel.sendMessage("The password is: " + db.getDiscordChannelObj(discordChannelId).get().getPassword()).queue();
                break;
            default:
                if (discordChannel.getTelegramChannels() != null) {
                    for (String telegramChannel : discordChannel.getTelegramChannels()) {
                        telegramSendMessage(telegramChannel, messageText, author, file);
                        telegramSendMessage(telegramChannel, messageText, author, file);
                    }
                    MessageModel messageModel = new MessageModel(message);
                    db.addMessageToDB(messageModel);
                }
        }
    }


    //---------------------------------------------------------------------------------------------------
    private void telegramReply(String channel, String text) throws TelegramApiException {
        SendMessage reply = new SendMessage().setChatId(channel).setText(text);
        telegramBot.sendMessage(reply);
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
                    return true;
                } else if (discordChannel.getChannelId() != null) {
                    db.addToDiscordList(discordChannel, telegramChannel.getChannelId());
                    db.addToTelegramList(telegramChannel, discordChannelId.getId());

                    discordChannelId.sendMessage("Channel has been linked to Telegram channel ").queue();
                    return true;
                }
            } else {
                discordChannelId.sendMessage("No channel with password found").queue();
                return false;
            }
        }
        return false;
    }

    private boolean telegramHandshake(SendMessage sendMessage, String telegramChannelId) throws TelegramApiException, IOException {
        String message = sendMessage.getText();

        if (message.matches("/([Ll][Ii][Nn][Kk])\\s[^\\s\\\\].*")) {
            password = message.substring(6);
            DiscordChannel discordChannel = db.getDiscordChannelFromPassword(password);
            TelegramChannel telegramChannel = db.getTelegramChannelObj(telegramChannelId).get();

            if (discordChannel.getChannelId() != null) {
                if (db.getTelegramChannelObj(telegramChannelId).get() == null) {
                    TelegramChannel channel = new TelegramChannel();
                    channel.setChannelId(telegramChannelId);
                    channel.setPassword(generatePassword());
                    List<String> list = new ArrayList<>();
                    list.add(discordChannel.getChannelId());
                    channel.setDiscordChannels(list);

                    db.addChannelToDB(channel);
                    db.addToDiscordList(discordChannel, telegramChannelId);

                    telegramReply(telegramChannelId, "Channel been linked to Discord channel ");
                    return true;
                } else if (telegramChannel.getChannelId() != null) {
                    db.addToTelegramList(telegramChannel, discordChannel.getChannelId());
                    db.addToDiscordList(discordChannel, telegramChannelId);

                    telegramReply(telegramChannelId, "Channel been linked to Discord channel ");
                    return true;
                } else {
                    telegramReply(telegramChannelId, "No channel with password found");
                    return false;
                }
            }
        }
        return false;
    }

    private String generatePassword() {
        password = RandomStringUtils.random(8, characters);
        while (!db.uniquePassword(password)) {
            password = RandomStringUtils.random(8, characters);
        }
        return password;
    }
}

