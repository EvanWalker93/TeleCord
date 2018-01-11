package main.java.TCBot;

import main.java.TCBot.model.ChannelObj;
import main.java.TCBot.model.MessageModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.List;


public class TeleCordBot implements DiscordBot.DiscordMessageListener, TelegramBot.TelegramMessageListener, Serializable {

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private DatabaseHandler db = new DatabaseHandler();

    private String password;
    private String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static void main(String[] args) {
        TeleCordBot telecordBot = new TeleCordBot();

        //Start the Telegram and Discord bots, and connect to the MongoDb
        telecordBot.startTelegramBot();
        telecordBot.startDiscordBot();
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

    public void processMessage(MessageModel messageModel) {
        String command = "";
        String parameter = "";
        ChannelObj parentChannel = db.getChannelObj(messageModel.getChannel());

        if (messageModel.isCommand()) {
            command = messageModel.getCommand().toLowerCase();
            if (messageModel.hasParameter()) {
                parameter = messageModel.getParameter();
            }
        }

        switch (command) {
            case "/link":
                //If the channel is not in the DB, give the channel a password and add to DB
                if (parentChannel == null) {
                    parentChannel = messageModel.getChannel();
                    parentChannel.setPassword(generatePassword());
                    db.addChannelToDB(parentChannel);
                } else {
                    parentChannel.setPassword(db.getChannelObj(parentChannel).getPassword());
                }
                if (parameter.equals("")) {
                    sendMessage(parentChannel, "Type the following into the channel to link");
                    sendMessage(parentChannel, "/link " + parentChannel.getPassword());
                } else {
                    ChannelObj childChannel = db.getChannelFromPassword(parameter);
                    childChannel.linkChannel(parentChannel.getId());
                    parentChannel.linkChannel(childChannel.getId());
                    db.addChannelToDB(childChannel);
                    db.addChannelToDB(parentChannel);
                    sendMessage(parentChannel, "The channels have been linked");
                }
                break;
            case "/delink":
                break;
            case "/remove":
                break;
            case "/password":
                sendMessage(parentChannel, generatePassword());
                break;
            default:
                List<ObjectId> channels = parentChannel.getLinkedChannels();
                for (ObjectId channel : channels) {
                    ChannelObj channelObj = db.getChannelObj(channel);
                    sendMessage(channelObj, messageModel);
                }
                break;
        }
    }

    private void sendMessage(ChannelObj targetChannel, MessageModel messageModel) {
        if (targetChannel.getSource().equalsIgnoreCase("discord")) {
            discordBot.sendMessageToChannel(targetChannel, messageModel);
        } else if (targetChannel.getSource().equalsIgnoreCase("telegram")) {
            telegramBot.sendMessageToChannel(targetChannel, messageModel);
        }
    }

    private void sendMessage(ChannelObj targetChannel, String messageText) {
        MessageModel messageModel = new MessageModel();
        messageModel.setMessageText(messageText);

        if (targetChannel.getSource().equalsIgnoreCase("discord")) {
            discordBot.sendMessageToChannel(targetChannel, messageModel);
        } else if (targetChannel.getSource().equalsIgnoreCase("telegram")) {
            telegramBot.sendMessageToChannel(targetChannel, messageModel);
        }
    }

    private String generatePassword() {
        password = RandomStringUtils.random(8, characters);
        while (!db.uniquePassword(password)) {
            password = RandomStringUtils.random(8, characters);
        }
        return password;
    }
}

