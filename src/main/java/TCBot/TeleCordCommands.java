package main.java.TCBot;

import main.java.TCBot.model.ChannelObj;
import main.java.TCBot.model.MessageModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;

import java.util.Set;

class TeleCordCommands {

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private DatabaseHandler db;
    private String parameter = "";

    TeleCordCommands(DiscordBot discordBot, TelegramBot telegramBot, DatabaseHandler db) {
        this.discordBot = discordBot;
        this.telegramBot = telegramBot;
        this.db = db;
    }

    void link(MessageModel messageModel) {
        ChannelObj parentChannel = db.getChannelObj(messageModel.getChannel());

        if (messageModel.hasParameter()) {
            parameter = messageModel.getParameter();
        }

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
    }

    void delink(MessageModel messageModel) {
        ChannelObj parentChannel = db.getChannelObj(messageModel.getChannel());

        if (parameter.equals("")) {
            sendMessage(parentChannel, "Please include the password of the channel you wish to remove.");
        } else {
            //Check if the parameter is a password in the DB
            if (!db.uniquePassword(parameter)) {
                ChannelObj childChannel = db.getChannelFromPassword(parameter);
                childChannel.removeChannelFromList(parentChannel.getId());
                parentChannel.removeChannelFromList(childChannel.getId());

                db.addChannelToDB(parentChannel);
                db.addChannelToDB(childChannel);

                sendMessage(parentChannel, "The link to " + childChannel.getChannelName() + " has been removed.");
                sendMessage(childChannel, "The link to " + parentChannel.getChannelName() + " has been removed.");
            } else {
                sendMessage(parentChannel, "No channel with given password found.");
            }
        }
    }

    void remove(MessageModel messageModel) {
        ChannelObj parentChannel = db.getChannelObj(messageModel.getChannel());
        sendMessage(parentChannel, "All channel pairings have been removed");
        db.removeChannelFromDB(parentChannel);
    }

    void password(MessageModel messageModel) {
        ChannelObj parentChannel = db.getChannelObj(messageModel.getChannel());

        if (parentChannel.getPassword() != null) {
            sendMessage(parentChannel, parentChannel.getPassword());
        } else {
            sendMessage(parentChannel, "This channel has no password yet, use /link to create a password.");
        }
    }

    void sendToLinks(MessageModel parentMessage) {
        ChannelObj parentChannel = db.getChannelObj(parentMessage.getChannel());
        Set<ObjectId> channels = parentChannel.getLinkedChannels();

        for (ObjectId channel : channels) {
            ChannelObj channelObj = db.getChannelObj(channel);
            parentMessage.addChildMessage(sendMessage(channelObj, parentMessage));
        }
        db.addMessageToDB(parentMessage);
    }

    private MessageModel sendMessage(ChannelObj targetChannel, MessageModel messageModel) {
        if (targetChannel.getSource().equalsIgnoreCase("discord")) {
            return discordBot.sendMessageToChannel(targetChannel, messageModel);
        } else if (targetChannel.getSource().equalsIgnoreCase("telegram")) {
            return telegramBot.sendMessageToChannel(targetChannel, messageModel);
        }
        return null;
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
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String password = RandomStringUtils.random(8, characters);
        while (!db.uniquePassword(password)) {
            password = RandomStringUtils.random(8, characters);
        }
        return password;
    }

}
