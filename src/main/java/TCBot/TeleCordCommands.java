package TCBot;

import TCBot.model.ChannelObj;
import TCBot.model.MessageModel;
import TCBot.model.UserModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

class TeleCordCommands {

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private DatabaseHandler db;

    TeleCordCommands(DiscordBot discordBot, TelegramBot telegramBot, DatabaseHandler db) {
        this.discordBot = discordBot;
        this.telegramBot = telegramBot;
        this.db = db;
    }

    void link(MessageModel messageModel) {
        ChannelObj parentChannel = db.getChannelObj(messageModel.getChannel());
        String parameter = "";

        //Check if the user has permissions needed to use /link command
        if(!getUserPermissions(messageModel)){
            sendMessage(messageModel.getChannel(), "You do not have permission to link");
            return;
        }

        if (messageModel.hasParameter()) {
            parameter = messageModel.getParameter();
        }

        //If the parentChannel is not yet in the DB, generate a password for it and insert into DB
        if (parentChannel == null) {
            parentChannel = messageModel.getChannel();
            parentChannel.setPassword(generatePassword());
            db.addChannelToDB(parentChannel);
        }
        //Else, if it is in the DB, fetch its password and set it to the parentChannel obj
        else {
            parentChannel.setPassword(db.getChannelObj(parentChannel).getPassword());
        }
        //If there is no parameter, get the parentChannels password and send a message with it
        if (parameter.equals("")) {
            sendMessage(parentChannel, "Type the following into the channel to link");
            sendMessage(parentChannel, "/link " + parentChannel.getPassword());
        }
        //Else, if there is a parameter, check if it is in the DB,
        //and add the parentChannel's Id to the target channel's linkRequestList
        else {
            try{
                ChannelObj childChannel = db.getChannelFromPassword(parameter);
                childChannel.addRequestLink(parentChannel.getId());

                db.addChannelToDB(childChannel);
                db.addChannelToDB(parentChannel);

                sendMessage(parentChannel, "Link request sent, confirm on linked channel by typing:");
                sendMessage(parentChannel, "/confirm " + parentChannel.getPassword());
            }catch (Exception e){
                e.printStackTrace();
                sendMessage(parentChannel, "There is no channel with the given password");
            }

        }
    }

    void confirm(MessageModel messageModel){
        ChannelObj parentChannel = db.getChannelObj(messageModel.getChannel());
        String parameter = "";

        if (messageModel.hasParameter()) {
            parameter = messageModel.getParameter();
        }

        if(!getUserPermissions(messageModel)){
            sendMessage(messageModel.getChannel(), "You do not have permission to confirm a link");
            return;
        }

        try{
            if(!parameter.equals("")){
                ChannelObj childChannel = db.getChannelFromPassword(parameter);
                parentChannel.linkChannel(childChannel.getId());
                childChannel.linkChannel(parentChannel.getId());
                parentChannel.getRequestLinkChannels().remove(childChannel.getId());

                db.addChannelToDB(parentChannel);
                db.addChannelToDB(childChannel);

                sendMessage(parentChannel, "The channels have been linked successfully");
            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    void delink(MessageModel messageModel) {
        ChannelObj parentChannel = db.getChannelObj(messageModel.getChannel());
        String parameter = "";

        if (messageModel.hasParameter()) {
            parameter = messageModel.getParameter();
        }

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

        if(parentChannel != null){
            Set<ObjectId> channels = parentChannel.getLinkedChannels();
            for (ObjectId channel : channels) {
                ChannelObj channelObj = db.getChannelObj(channel);
                parentMessage.addChildMessage(sendMessage(channelObj, parentMessage));
            }
            db.addMessageToDB(parentMessage);
        }
    }

    void showLinks(MessageModel messageModel){
        ChannelObj channel = db.getChannelObj(messageModel.getChannel());

        if(channel.getLinkedChannels() != null){
            StringBuilder linkedChannels = new StringBuilder();
            linkedChannels.append("The following channels are linked: \n");

            for(ObjectId linkedChannelsId : channel.getLinkedChannels()){
                linkedChannels.append(db.getChannelObj(linkedChannelsId).getChannelName()).append("\n");
            }
            sendMessage(channel, linkedChannels.toString());
        }else{
            sendMessage(channel, "There are no linked channels");
        }

    }

    void delete(MessageModel messageModel){
        List<MessageModel> allMessagesFromUser = db.getAllMessages(messageModel);
        allMessagesFromUser.sort(Comparator.comparing(MessageModel::getDate).reversed());

        if(messageModel.hasParameter()){
            String parameter = messageModel.getParameter();
            System.out.println(parameter);

            if(parameter.matches("([Ll][Aa][Ss][Tt])([1-9]|[1-9][0-9])")){
                 parameter = parameter.replaceAll("[^0-9]", "");

                 for(int i = 0; i < Integer.parseInt(parameter); i++){
                     MessageModel messageToDelete = allMessagesFromUser.get(i);
                     deleteMessage(messageToDelete);
                }

            }else if(parameter.matches("([1-9]|[1-4][0-9])|50")){
                MessageModel messageToDelete = allMessagesFromUser.get(Integer.parseInt(parameter) - 1);
                deleteMessage(messageToDelete);
            }else if(parameter.matches("([Ll][Aa][Ss][Tt])")){
                MessageModel messageToDelete = allMessagesFromUser.get(0);
                deleteMessage(messageToDelete);
            }
        }else{
            String message = "**Usage:**\n" +
                    "/delete last - Deletes the last sent message.\n" +
                    "/delete # - Deletes the message that matches #, with the most recent message being 1.\n" +
                    "/delete last # - Deletes the last # messages, # being the amount.\n\n" +
                    "(Can only delete the users most recent 50 messages, commands are ignored. " +
                    "TeleCord will automatically remove messages deleted through Discord, but Telegram messages " +
                    "must be removed manually with the /delete command.)";
            sendMessage(messageModel.getChannel(), message);
        }
    }
    //------------------------------------------------------------------------------------------------------------------
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
        messageModel.setUser(new UserModel());
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

    private boolean getUserPermissions(MessageModel messageModel){
        if(messageModel.getChannel().getSource().equalsIgnoreCase("telegram")){
            return telegramBot.isAdmin(messageModel);
        }
        else if(messageModel.getChannel().getSource().equalsIgnoreCase("discord")){
            return discordBot.isAdmin(messageModel);

        }
        return false;
    }

    void deleteMessage(MessageModel messageModel) {
        MessageModel deletedMessage = db.getMessage(messageModel);
        String source = messageModel.getChannel().getSource();

        if(deletedMessage != null && deletedMessage.getUser() != null) {
            for (MessageModel childMessage : deletedMessage.getChildMessages()) {
                if (childMessage.getChannel().getSource().equalsIgnoreCase("telegram")) {
                    telegramBot.deleteMessage(childMessage);
                } else if (childMessage.getChannel().getSource().equalsIgnoreCase("discord")) {
                    discordBot.deleteMessage(childMessage);
                }
            }

            if (source.equalsIgnoreCase("telegram")) {
                telegramBot.deleteMessage(deletedMessage);
            } else if (source.equalsIgnoreCase("discord")) {
                discordBot.deleteMessage(deletedMessage);
            }
            db.removeMessageFromDB(deletedMessage);
        }
    }
}
