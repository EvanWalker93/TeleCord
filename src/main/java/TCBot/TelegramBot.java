package main.java.TCBot;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.send.SendVideo;
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
    private FileReader fileReader = new FileReader();
    private String token = fileReader.getTokens("telegramToken");


    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Telegram update received...");

        SendMessage newMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText(update.getMessage().getText());
        String channel = update.getMessage().getChatId().toString();
        String username =  update.getMessage().getFrom().getUserName();
        java.io.File file = checkForFile(update, newMessage);


        //Message is sent over to the TeleCordBot for message handling logic
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

    void sendMessageToChannel(String channel, String messageText, String author) throws TelegramApiException {
        System.out.println("SendMessageToChannelWithText, channel: " + channel);
        SendMessage message = new SendMessage().setChatId(channel).setText(author + ": " + messageText);
        sendMessage(message);
    }

    void sendVideo(String channel, String messageText, String author, java.io.File file) {
        SendVideo video = new SendVideo();
        video.setNewVideo(file);
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
    void sendPhoto(String channel, String messageText, String author, java.io.File file) {
        SendPhoto photoMsg = new SendPhoto();
        photoMsg.setNewPhoto(file);
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

    void sendDocument(String channel, String messageText, String author, java.io.File file) {
        SendDocument documentMsg = new SendDocument();
        documentMsg.setNewDocument(file);
        documentMsg.setChatId(channel);

        if (messageText == null || Objects.equals(messageText, "")) {
            documentMsg.setCaption("File from " + author);
        } else {
            documentMsg.setCaption(author + ": " + messageText);
        }
        try {
            sendDocument(documentMsg);
            file.delete();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "TeleCord_dev";
    }

    @Override
    public String getBotToken() {
        return token;
    }

    private java.io.File checkForFile(Update update, SendMessage newMessage) {

        java.io.File file = null;
        java.io.File tmpFile;

        //Checks if the Telegram message contains a photo and sends it to Discord
        //I'm not sure how this works and should be cleaned up
        if (getPhoto(update) != null) {
            file = downloadPhotoByFilePath(getFilePath(getPhoto(update)));
            tmpFile = new java.io.File("photo.jpg");
            file.renameTo(tmpFile);
            file = tmpFile;

            //Puts in blank text if message text was null to avoid null pointer exception
            //Occurs at the TeleCordBot switch that turns message text to lowercase
            if (newMessage.getText() == null) {
                newMessage.setText("");
            }
        }


        //Checks if the Telegram message contains a document and sends it to Discord
        if (getDocument(update) != null) {
            file = downloadPhotoByFilePath(getFilePath(getDocument(update)));
            String fileName = getDocumentName(update);

            if (fileName.substring(fileName.length() - 8).equals(".gif.mp4")) {
                fileName = StringUtils.removeEnd(fileName, ".mp4");
                tmpFile = new java.io.File(fileName);
            } else {
                tmpFile = new java.io.File(getDocumentName(update));
            }

            file.renameTo(tmpFile);
            file = tmpFile;

            //Puts in blank text if message text was null to avoid null pointer exception
            //Occurs at the TeleCordBot switch that turns message text to lowercase
            if (newMessage.getText() == null) {
                newMessage.setText("");
            }
        }
        return file;
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

    private Document getDocument(Update update) {
        if (update.hasMessage() && update.getMessage().hasDocument()) {
            return update.getMessage().getDocument();
        }
        return null;
    }

    private String getDocumentName(Update update) {
        return update.getMessage().getDocument().getFileName();
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
                // We execute the method using AbsSender::getDocument method.
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
            // We execute the method using AbsSender::getDocument method.
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