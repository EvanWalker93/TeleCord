package main.java.TCBot;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.Requester;
import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.stickers.Sticker;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.List;

public class FileHandler extends TelegramLongPollingBot {
    private FileInputStream fis;
    private String fileName;
    private TokenReader tokenReader = new TokenReader();
    private String token = tokenReader.getTokens("telegramToken");
    private String botUserName = tokenReader.getUserName();

    FileHandler(Message message) throws IOException {
        this.fis = toFileInputStream(message);
        this.fileName = FileName(message);
    }

    FileHandler(Update update) throws IOException, TelegramApiException {
        this.fis = toFileInputStream(update);
        this.fileName = FileName(update);
    }

    String getFileName() {
        return fileName;
    }

    String getFileExtension() {
        //This may throw a null pointer exception if there is no extension
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    FileInputStream getFis() {
        return fis;
    }


    //METHODS-----------------------------------------------------------------------------------------------------------

    //Discord
    private FileInputStream toFileInputStream(Message message) throws IOException {
        String tempFileName = message.getAttachments().get(0).getFileName();
        URL url = new URL(message.getAttachments().get(0).getUrl());

        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("user-agent", Requester.USER_AGENT);
        urlConnection.connect();

        java.io.File output = java.io.File.createTempFile(tempFileName, ".tmp");
        FileUtils.copyInputStreamToFile(urlConnection.getInputStream(), output);

        return fis = new FileInputStream(output);
    }

    private String FileName(Message message) {
        return message.getAttachments().get(0).getFileName();
    }

    //Telegram
    private FileInputStream toFileInputStream(Update update) throws IOException, TelegramApiException {
        if (update.getMessage().hasPhoto()) {
            PhotoSize photo = getPhoto(update);
            fis = new FileInputStream(downloadFile(getFilePath(photo)));

        } else if (update.getMessage().hasDocument()) {
            fis = new FileInputStream(downloadFile(getFilePath(update.getMessage().getDocument())));
        } else if (update.getMessage().getSticker() != null) {
            Sticker sticker = update.getMessage().getSticker();
            fis = new FileInputStream(downloadFile(getFilePath(sticker.getFileId())));
        } else {
            return null;
        }

        return fis;
    }

    private String FileName(Update update) {
        if (update.getMessage().hasDocument()) {
            return update.getMessage().getDocument().getFileName();
        } else if (update.getMessage().hasPhoto()) {
            return update.getMessage().getPhoto().get(0).getFileId();
        } else if (update.getMessage().getSticker() != null) {
            return update.getMessage().getSticker().getFileId() + ".jpg";
        }
        return null;
    }

    private String getFilePath(PhotoSize photo) {

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

    private String getFilePath(String fileID) {
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(fileID);
        try {
            File file = getFile(getFileMethod);
            return file.getFilePath();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null; // Just in case
    }

    private String getFilePath(Document document) {

        // We create a GetFile method and set the file_id from the doc
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

    @Override
    public void onUpdateReceived(Update update) {

    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
