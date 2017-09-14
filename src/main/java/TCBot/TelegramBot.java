package main.java.TCBot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.List;

public class TelegramBot extends TelegramLongPollingBot {

    private TelegramMessageListener listener;


    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Telegram update received...");

        SendMessage newMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText(update.getMessage().getText());
        String channel = update.getMessage().getChatId().toString();
        String username =  update.getMessage().getFrom().getUserName();
        List<File> attachment = (List<File>) update.getMessage().getDocument();

        try {
            listener.onTelegramMessageReceived(newMessage, channel, username, attachment);
            System.out.println("Sending message to TeleCordBot");
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        }
    }

    TelegramBot(TelegramMessageListener listener) {
        this.listener = listener;
    }


    @Override
    public String getBotUsername() {
        return "TeleCord_dev";
    }

    @Override
    public String getBotToken() {
        return "316767133:AAF-Mvb0OrAtHejI5pA18VeJe-JeyhP_Mag";
    }


    public interface TelegramMessageListener {
        void onTelegramMessageReceived(SendMessage message, String channel, String author, List<File> attachment) throws TelegramApiException, IOException;
    }




    void sendMessageToChannelWithText(String channel, String messageText, String author) throws TelegramApiException {
        System.out.println("SendMessageToChannelWithText, channel: " + channel);
        SendMessage message = new SendMessage().setChatId(channel).setText(author + ": " + messageText);
        sendMessage(message);
    }

}