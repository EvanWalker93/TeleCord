package TCBot;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.Requester;
import org.apache.commons.io.IOUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class FileHandler extends TelegramLongPollingBot {
    private FileInputStream fis;
    private byte[] bytes;
    private String fileName;
    private String token = TeleCordProps.getInstance().getProperty("telegramBotToken");
    private String botUserName = TeleCordProps.getInstance().getProperty("telegramBotUsername");

    public FileHandler() {
    }

    //Discord Constructor
    FileHandler(Message message) {
        this.bytes = toByteArray(message);
        this.fileName = fileName(message);
    }

    //Telegram Constructor
    FileHandler(Update update) {
        try {
            this.bytes = toByteArray(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileName = fileName(update);
    }

    String getFileName() {
        return fileName;
    }

    String getFileExtension() {
        //This may throw a null pointer exception if there is no extension
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }


    //METHODS-----------------------------------------------------------------------------------------------------------

    //Discord
    private byte[] toByteArray(Message message) {
        try {
            URL url = new URL(message.getAttachments().get(0).getUrl());

            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("user-agent", Requester.USER_AGENT);
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Telegram
    private byte[] toByteArray(Update update) throws IOException {
        org.telegram.telegrambots.meta.api.objects.Message message = update.getMessage();
        try {
            if (message.hasPhoto()) {
                PhotoSize photo = getPhoto(update);
                fis = new FileInputStream(downloadFile(getFilePath(photo)));

            } else if (message.hasDocument()) {
                Document doc = message.getDocument();
                fis = new FileInputStream(downloadFile(getFilePath(doc)));

            } else if (message.hasSticker()) {
                Sticker sticker = message.getSticker();
                fis = new FileInputStream(downloadFile(getFilePath(sticker.getFileId())));

            } else if (message.getVoice() != null) {
                Voice voice = message.getVoice();
                fis = new FileInputStream(downloadFile(getFilePath(voice.getFileId())));

            } else if (message.hasVideo()) {
                Video video = message.getVideo();
                fis = new FileInputStream(downloadFile(getFilePath(video.getFileId())));

            } else if (message.hasVideoNote()) {
                VideoNote videoNote = message.getVideoNote();
                fis = new FileInputStream(downloadFile(getFilePath(videoNote.getFileId())));

            } else {
                return null;
            }
        } catch (FileNotFoundException | TelegramApiException e) {
            e.printStackTrace();
        }

        return IOUtils.toByteArray(fis);
    }

    private String fileName(Message message) {
        return message.getAttachments().get(0).getFileName();
    }

    private String fileName(Update update) {
        org.telegram.telegrambots.meta.api.objects.Message message = update.getMessage();

        if (message.hasDocument()) {
            return message.getDocument().getFileName();
        } else if (message.hasPhoto()) {
            return UUID.randomUUID().toString() + ".jpg";
        } else if (message.hasSticker()) {
            return message.getSticker().getFileId() + ".jpg";
        } else if (message.getVoice() != null) {
            return UUID.randomUUID().toString() + ".ogg";
        } else if (message.hasVideo() || message.hasVideoNote()) {
            return UUID.randomUUID().toString() + ".mp4";
        }
        return null;
    }

    private String getFilePath(PhotoSize photo) {

        if (photo.hasFilePath()) { // If the file_path is already present, we are done!
            System.out.println(photo.getFileId());
            return photo.getFilePath();
        } else { // If not, find it
            // We create a GetFile method and set the file_id from the photo
            GetFile getFileMethod = new GetFile();
            getFileMethod.setFileId(photo.getFileId());
            try {
                // We execute the method using AbsSender::getDocument method.
                File file = execute(getFileMethod);
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
            File file = execute(getFileMethod);
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
            File file = execute(getFileMethod);
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
            return photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);
        }
        // Return null if not found
        return null;
    }

    InputStream getFile() {
        return new ByteArrayInputStream(bytes);
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
