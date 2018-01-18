package main.java.TCBot;

import main.java.TCBot.model.channel.AbstractChannel;
import main.java.TCBot.model.MessageModel;
import main.java.TCBot.model.channel.DiscordChannel;
import main.java.TCBot.model.channel.TelegramChannel;
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
        AbstractChannel parentChannel = db.getChannelObj(messageModel.getChannel());

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
            AbstractChannel childChannel = db.getChannelFromPassword(parameter);
            childChannel.linkChannel(parentChannel.getId());
            parentChannel.linkChannel(childChannel.getId());

            db.addChannelToDB(childChannel);
            db.addChannelToDB(parentChannel);

            sendMessage(parentChannel, "The channels have been linked");
        }
    }

    void delink(MessageModel messageModel) {
        AbstractChannel parentChannel = db.getChannelObj(messageModel.getChannel());

        if (parameter.equals("")) {
            sendMessage(parentChannel, "Please include the password of the channel you wish to remove.");
        } else {
            //Check if the parameter is a password in the DB
            if (!db.uniquePassword(parameter)) {
                AbstractChannel childChannel = db.getChannelFromPassword(parameter);
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
        AbstractChannel parentChannel = db.getChannelObj(messageModel.getChannel());
        sendMessage(parentChannel, "All channel pairings have been removed");
        db.removeChannelFromDB(parentChannel);
    }

    void password(MessageModel messageModel) {
        AbstractChannel parentChannel = db.getChannelObj(messageModel.getChannel());

        if (parentChannel.getPassword() != null) {
            sendMessage(parentChannel, parentChannel.getPassword());
        } else {
            sendMessage(parentChannel, "This channel has no password yet, use /link to create a password.");
        }
    }

    void sendToLinks(MessageModel parentMessage) {
        AbstractChannel parentChannel = db.getChannelObj(parentMessage.getChannel());
        Set<ObjectId> channelObjectIds = parentChannel.getLinkedChannels();

        for (ObjectId channelObjectId : channelObjectIds) {
            AbstractChannel abstractChannel = db.getChannelObj(channelObjectId);
            parentMessage.addChildMessage(sendMessage(abstractChannel, parentMessage));
        }
        db.addMessageToDB(parentMessage);
    }

    private MessageModel sendMessage(AbstractChannel targetChannel, MessageModel messageModel) {
        if (targetChannel instanceof DiscordChannel) {
            return discordBot.sendMessageToChannel(targetChannel, messageModel);
        } else if (targetChannel instanceof TelegramChannel) {
            return telegramBot.sendMessageToChannel(targetChannel, messageModel);
        }
        return null;
    }

    private void sendMessage(AbstractChannel targetChannel, String messageText) {
        MessageModel messageModel = new MessageModel();
        messageModel.setMessageText(messageText);

        if (targetChannel instanceof DiscordChannel) {
            discordBot.sendMessageToChannel(targetChannel, messageModel);
        } else if (targetChannel instanceof TelegramChannel) {
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
