package main.java.TCBot;

import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class TelegramBot extends TelegramLongPollingBot {

    private TelegramMessageListener listener;


    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Telegram update received...");

        SendMessage newMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText(update.getMessage().getText());
        String channel = update.getMessage().getChatId().toString();
        String username =  update.getMessage().getFrom().getUserName();
        java.io.File file = null;
        java.io.File photoFile;


        //Checks if the message from Telegram has/is a photo and sends it to Discord
        //I'm not sure how this works and should be cleaned up
        if (getPhoto(update) != null) {
            file = downloadPhotoByFilePath(getFilePath(getPhoto(update)));
            photoFile = new java.io.File("photo.jpg");
            file.renameTo(photoFile);
            file = new java.io.File("photo.jpg");

            if (newMessage.getText() == null) {
                newMessage.setText("");
            }
        }


        try {
            listener.onTelegramMessageReceived(newMessage, channel, username, file);
            System.out.println("Sending message to TeleCordBot");

            //Delete the file after use
            if (file != null) {
                file.delete();
            }
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

    //This method was copied off of an example program, might want to rework
    private PhotoSize getPhoto(Update update) {
        // Check that the update contains a message and the message has a photo
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            // When receiving a photo, you usually get different sizes of it
            List<PhotoSize> photos = update.getMessage().getPhoto();

            // We fetch the bigger photo
            return photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null);
        }

        // Return null if not found
        return null;
    }

    private Document getFile(Update update) {
        if (update.hasMessage() && update.getMessage().hasDocument()) {
            return update.getMessage().getDocument();
        }
        return null;
    }

    void sendMessageToChannel(String channel, String messageText, String author) throws TelegramApiException {
        System.out.println("SendMessageToChannelWithText, channel: " + channel);
        SendMessage message = new SendMessage().setChatId(channel).setText(author + ": " + messageText);
        sendMessage(message);
    }

    private String getFilePath(PhotoSize photo) {
        Objects.requireNonNull(photo);

        if (photo.hasFilePath()) { // If the file_path is already present, we are done!
            return photo.getFilePath();
        } else { // If not, let find it
            // We create a GetFile method and set the file_id from the photo
            GetFile getFileMethod = new GetFile();
            getFileMethod.setFileId(photo.getFileId());
            try {
                // We execute the method using AbsSender::getFile method.
                File file = getFile(getFileMethod);
                // We now have the file_path
                return file.getFilePath();
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return null; // Just in case
    }

    private String getFilePath(Document document) {
        Objects.requireNonNull(document);

        // We create a GetFile method and set the file_id from the photo
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(document.getFileId());
        try {
            // We execute the method using AbsSender::getFile method.
            File file = getFile(getFileMethod);
            // We now have the file_path
            return file.getFilePath();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        return null; // Just in case
    }

    private java.io.File downloadPhotoByFilePath(String filePath) {
        try {
            // Download the file calling AbsSender::downloadFile method
            return downloadFile(filePath);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }


    public interface TelegramMessageListener {
        void onTelegramMessageReceived(SendMessage message, String channel, String author, java.io.File attachment) throws TelegramApiException, IOException;
    }
}