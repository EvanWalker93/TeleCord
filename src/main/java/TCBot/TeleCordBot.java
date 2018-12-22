package TCBot;

import TCBot.model.MessageModel;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.util.Properties;


public class TeleCordBot implements DiscordBot.DiscordMessageListener, TelegramBot.TelegramMessageListener, Serializable {

    private DatabaseHandler db = new DatabaseHandler();
    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private TeleCordCommands teleCordCommands;
    private static Properties properties = new Properties();

    public static void main(String[] args) {
        TeleCordBot telecordBot = new TeleCordBot();

        //Start the Telegram and Discord bots, and connect to the MongoDb
        telecordBot.startTelegramBot();
        telecordBot.startDiscordBot();
        telecordBot.init();
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

    private void init() {
        db.init();
        teleCordCommands = new TeleCordCommands(discordBot, telegramBot, db);
    }

    public void processMessage(MessageModel messageModel) {
        String messageText = "";

        if (messageModel.isCommand()) {
            messageText = messageModel.getCommand().toLowerCase();
        }

        switch (messageText) {
            //Pairs two channels
            case "/link":
                teleCordCommands.link(messageModel);
                break;

            //Removes a pairing
            case "/delink":
                teleCordCommands.delink(messageModel);
                break;

            //Removes channel from db and its links
            case "/remove":
                teleCordCommands.remove(messageModel);
                break;

            //Sends the user the password of the channel (if it has one).
            case "/password":
                teleCordCommands.password(messageModel);
                break;

            //Confirm a channel link request
            case "/confirm":
                teleCordCommands.confirm(messageModel);
                break;

            //Shows the names of all linked channels
            case "/showlinks":
                teleCordCommands.showLinks(messageModel);
                break;

            case "/delete":
                teleCordCommands.delete(messageModel);
                break;

            //Send the message to all paired channels
            default:
                teleCordCommands.sendToLinks(messageModel);
                break;
        }
    }

    public void updateMessage(MessageModel messageModel){
        MessageModel editedMessage = db.getMessage(messageModel);

        for(MessageModel newMessage : editedMessage.getChildMessages()){
            newMessage.setMessageText(messageModel.getFormattedMessageText());

            if(newMessage.getChannel().getSource().equalsIgnoreCase("telegram")){
                telegramBot.updateMessage(newMessage);
            }
            else if(newMessage.getChannel().getSource().equalsIgnoreCase("discord")){
                discordBot.updateMessage(newMessage);
            }
        }
    }

    //Only works with discord
    public void deleteMessage(MessageModel messageModel) {
        teleCordCommands.deleteMessage(messageModel);
    }

    public Properties getProperties() {
        return properties;
    }
}

