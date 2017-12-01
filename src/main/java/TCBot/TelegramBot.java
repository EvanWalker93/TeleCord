package main.java.TCBot;

import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.send.SendVideo;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.Objects;

public class TelegramBot extends TelegramLongPollingBot {

    private TelegramMessageListener listener;
    private TokenReader tokenReader = new TokenReader();
    private String token = tokenReader.getTokens("telegramToken");
    private String botUserName = tokenReader.getUserName();
    private FileHandler fileHandler = null;

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Telegram bot: Received an update from Telegram");
        Message message = update.getMessage();
        if (message.hasPhoto() || message.hasDocument() || message.getSticker() != null) {
            try {
                fileHandler = new FileHandler(update);
            } catch (IOException | TelegramApiException e) {
                e.printStackTrace();
            }
        }


        //Message is sent over to the TeleCordBot for message handling logic
        try {
            listener.onTelegramMessageReceived(update, fileHandler);
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        }
    }

    TelegramBot(TelegramMessageListener listener) {
        this.listener = listener;
    }

    void sendMessageToChannel(String channel, String messageText, String author) throws TelegramApiException {
        System.out.println("Telegram bot: Sending message to channel: " + channel);
        SendMessage message = new SendMessage().setChatId(channel).setText(author + ": " + messageText);
        execute(message);
    }

    void sendVideo(String channel, String messageText, String author, FileHandler file) {
        SendVideo video = new SendVideo();
        video.setNewVideo(file.getFileName(), file.getFis());
        video.setChatId(channel);
        if (messageText == null || Objects.equals(messageText, "")) {
            video.setCaption("File from " + author);
        } else {
            video.setCaption(author + ": " + messageText);
        }
        try {
            sendVideo(video);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //Receives a photo from Discord and sends it to Telegram
    void sendPhoto(String channel, String messageText, String author, FileHandler file) {
        SendPhoto photoMsg = new SendPhoto();
        photoMsg.setNewPhoto(file.getFileName(), file.getFis());
        System.out.println(file.getFis());
        photoMsg.setChatId(channel);

        if (messageText == null || Objects.equals(messageText, "")) {
            photoMsg.setCaption("File from " + author);
        } else {
            photoMsg.setCaption(author + ": " + messageText);
        }
        try {
            sendPhoto(photoMsg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void sendDocument(String channel, String messageText, String author, FileHandler file) {
        SendDocument documentMsg = new SendDocument();
        documentMsg.setNewDocument(file.getFileName(), file.getFis());
        documentMsg.setChatId(channel);

        if (messageText == null || Objects.equals(messageText, "")) {
            documentMsg.setCaption("File from " + author);
        } else {
            documentMsg.setCaption(author + ": " + messageText);
        }
        try {
            sendDocument(documentMsg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public interface TelegramMessageListener {
        void onTelegramMessageReceived(Update update, FileHandler fileInputStream) throws TelegramApiException, IOException;
    }
}