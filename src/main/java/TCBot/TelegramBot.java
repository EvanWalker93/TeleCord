package main.java.TCBot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;

public class TelegramBot extends TelegramLongPollingBot {

    private TelegramMessageListener listener;


    public interface TelegramMessageListener {
        void onTelegramMessageReceived(SendMessage message, String channel, String author) throws TelegramApiException, IOException;
    }

    TelegramBot(TelegramMessageListener listener) {
        this.listener = listener;
    }


    @Override
    public String getBotUsername() {
        return "TeleCord";
    }

    @Override
    public String getBotToken() {
        return "370672972:AAE-rF-G8HchhSipsxplWJ9m1yzr2tIF-v0";
    }

    @Override
    public void onUpdateReceived(Update update) {

        // Link the chats together
        String channel;
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals("link")) {


                SendMessage newMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText(update.getMessage().getText());
                channel = update.getMessage().getChatId().toString();
            try {
                listener.onTelegramMessageReceived(newMessage, channel, update.getMessage().getFrom().getUserName());
            } catch (TelegramApiException | IOException e) {
                e.printStackTrace();
            }


        }

        // If a user makes a post in Telegram, get the text from the message
        // and set it to object 'message'
        else  {
            channel = update.getMessage().getChatId().toString();
            SendMessage message = new SendMessage().setChatId(channel).setText((update.getMessage()).getText());

            try {
                listener.onTelegramMessageReceived(message, channel, update.getMessage().getFrom().getFirstName());
            } catch (TelegramApiException | IOException e) {
                e.printStackTrace();
            }
            //listener.onTelegramMessageReceived();

        }

    }

    void sendMessageToChannelWithText(String channel, String messageText, String author) throws TelegramApiException {
        System.out.println("SendMessageToChannelWithText, channel: " + channel);
        SendMessage message = new SendMessage().setChatId(channel).setText(author + ": " + messageText);
        sendMessage(message);
    }

}