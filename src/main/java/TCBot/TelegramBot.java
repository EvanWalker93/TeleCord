package main.java.TCBot;

import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.send.SendVideo;
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
        if (update.getMessage().hasPhoto() || update.getMessage().hasDocument()) {
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
        sendMessage(message);
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

    /*
    private InputStream checkForFile(Update update) {

        FileInputStream fis = null;

        //Checks if the Telegram message contains a photo and sends it to Discord
        //I'm not sure how this works and should be cleaned up
        if (getPhoto(update) != null) {
            try {
                fis = new FileInputStream(downloadFile(getFilePath(getPhoto(update))));
            } catch (FileNotFoundException | TelegramApiException e) {
                e.printStackTrace();
            }
            return fis;
        }

        //Checks if the Telegram message contains a document and sends it to Discord
        else if (getFilePath(getDocument(update)) != null) {
            try {
                fis = new FileInputStream(downloadFile(getFilePath(getDocument(update))));
            } catch (FileNotFoundException | TelegramApiException e) {
                e.printStackTrace();
            }
            return fis;
        }
        return null;
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

    private String getFilePath(PhotoSize photo) {
        Objects.requireNonNull(photo);

        if (photo.hasFilePath()) { // If the file_path is already present, we are done!
            System.out.println(photo.getFileId());
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
    */
    public interface TelegramMessageListener {
        void onTelegramMessageReceived(Update update, FileHandler fileInputStream) throws TelegramApiException, IOException;
    }
}