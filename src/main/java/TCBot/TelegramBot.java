package TCBot;

import TCBot.model.ChannelObj;
import TCBot.model.MessageModel;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.util.Objects;

public class TelegramBot extends TelegramLongPollingBot {

    private TelegramMessageListener listener;

    private String token = Objects.requireNonNull(TeleCordProps.getInstance()).getProperty("telegramBotToken");
    private String botUserName = TeleCordProps.getInstance().getProperty("telegramBotUsername");

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasEditedMessage()) {
            MessageModel editedMessage = new MessageModel(update.getEditedMessage());
            listener.updateMessage(editedMessage);

        }
        else{
            Message message = update.getMessage();
            MessageModel messageModel = new MessageModel(message);

            if (message.hasPhoto() || message.hasDocument() || message.hasSticker()) {
                FileHandler fileHandler = new FileHandler(update);
                messageModel.setFileHandler(fileHandler);
            } else if (message.hasContact()) {
                Contact contact = message.getContact();
                String contactName = contact.getFirstName() + " " + contact.getLastName();
                String phoneNumber = contact.getPhoneNumber();
                messageModel.setMessageText(contactName + "\n" + phoneNumber);
            } else if (message.hasLocation()) {
                Location location = message.getLocation();
                String lat = location.getLatitude().toString();
                String lon = location.getLongitude().toString();
                String url = "http://www.google.com/maps/place/" + lat + "," + lon;

                messageModel.setMessageText(url);
            }
            listener.processMessage(messageModel);
        }
    }


    void updateMessage(MessageModel editedMessage) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(editedMessage.getChannel().getChannelId())
                .setMessageId(Integer.parseInt(editedMessage.getMessageId()))
                .setText(editedMessage.getMessageText());

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void deleteMessage(MessageModel messageModel){
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(messageModel.getChannel().getChannelId());
        deleteMessage.setMessageId(Integer.parseInt(messageModel.getMessageId()));

        try{
            execute(deleteMessage);
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    MessageModel sendMessageToChannel(ChannelObj channelObj, MessageModel messageModel) {
        System.out.println("Telegram Bot: Sending Message to Telegram");
        String channel = channelObj.getChannelId();
        String messageText = messageModel.getFormattedMessageText();

        if (messageModel.hasFile()) {
            try {
                return new MessageModel(sendFile(messageModel, channel));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            SendMessage message = new SendMessage()
                    .setChatId(channel)
                    .setText(messageText);
            try {
                Message sentMessage = execute(message);
                return new MessageModel(sentMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    boolean isAdmin(MessageModel message) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(message.getChannel().getChannelId());
        getChatMember.setUserId(Integer.parseInt(message.getUser().getUserId()));

        ChatMember chatMember = null;
        try {
            chatMember = execute(getChatMember);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        assert chatMember != null;

        String status = chatMember.getStatus();
        return status.equalsIgnoreCase("creator") || status.equalsIgnoreCase("administrator");
    }

    private Message sendFile(MessageModel messageModel, String channel) throws TelegramApiException {
        FileHandler fileHandler = messageModel.getFileHandler();
        InputStream fis = fileHandler.getFile();

        String fileName = fileHandler.getFileName();
        String extension = fileHandler.getFileExtension().toLowerCase();
        String messageText = messageModel.getFormattedMessageText();

        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
                SendPhoto Photo = new SendPhoto()
                        .setChatId(channel)
                        .setPhoto(fileName, fis)
                        .setCaption(messageText);

                return execute(Photo);
            case "mp4":
            case "webm":
            case "gif":
                SendVideo video = new SendVideo()
                        .setChatId(channel)
                        .setVideo(fileName, fis)
                        .setCaption(messageText);

                return execute(video);
            default:
                SendDocument document = new SendDocument()
                        .setChatId(channel)
                        .setDocument(fileName, fis)
                        .setCaption(messageText);

                return execute(document);
        }
    }

    TelegramBot(TelegramMessageListener listener) {
        this.listener = listener;
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
        void processMessage(MessageModel messageModel);
        void updateMessage(MessageModel messageModel);
    }
}