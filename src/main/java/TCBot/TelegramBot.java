package main.java.TCBot;

import main.java.TCBot.model.ChannelObj;
import main.java.TCBot.model.MessageModel;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.send.SendVideo;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.InputStream;

public class TelegramBot extends TelegramLongPollingBot {

    private TelegramMessageListener listener;
    private TokenReader tokenReader = new TokenReader();
    private String token = tokenReader.getTokens("telegramToken");
    private String botUserName = tokenReader.getUserName();

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Telegram bot: Received an update from Telegram");
        Message message = update.getMessage();
        MessageModel messageModel = new MessageModel(message);
        //messageModel.setUserIsAdmin(isAdmin(message));

        if (message.hasPhoto() || message.hasDocument() || message.getSticker() != null) {
            FileHandler fileHandler = new FileHandler(update);
            messageModel.setFileHandler(fileHandler);
        }

        if (update.hasEditedMessage()) {
            Message editedMessage = update.getEditedMessage();
        }
        listener.processMessage(messageModel);
    }


    TelegramBot(TelegramMessageListener listener) {
        this.listener = listener;
    }

    MessageModel sendMessageToChannel(ChannelObj channelObj, MessageModel messageModel) {
        String channel = channelObj.getChannelId();
        String messageText = messageModel.getFormattedMessageText();

        if (messageModel.hasFile()) {
            try {
                sendFile(messageModel, channel);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            SendMessage message = new SendMessage().setChatId(channel).setText(messageText);

            try {
                Message sentMessage = execute(message);
                return new MessageModel(sentMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean isAdmin(Message message) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(message.getChatId());
        getChatMember.setUserId(message.getFrom().getId());

        ChatMember chatMember = null;
        try {
            chatMember = execute(getChatMember);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        assert chatMember != null;
        return chatMember.getCanDeleteMessages();
    }

    private void sendFile(MessageModel messageModel, String channel) throws TelegramApiException {
        FileHandler fileHandler = messageModel.getFileHandler();
        InputStream fis = fileHandler.getFile();

        String fileName = fileHandler.getFileName();
        String extension = fileHandler.getFileExtension();
        String messageText = messageModel.getFormattedMessageText();

        switch (extension) {
            case "jpg":
            case "png":
                SendPhoto Photo = new SendPhoto()
                        .setChatId(channel)
                        .setNewPhoto(fileName, fis)
                        .setCaption(messageText);

                sendPhoto(Photo);
                break;
            case "mp4":
            case "webm":
            case "gif":
                SendVideo video = new SendVideo()
                        .setChatId(channel)
                        .setNewVideo(fileName, fis)
                        .setCaption(messageText);

                sendVideo(video);
                break;
            default:
                SendDocument document = new SendDocument()
                        .setChatId(channel)
                        .setNewDocument(fileName, fis)
                        .setCaption(messageText);

                sendDocument(document);
                break;
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
        void processMessage(MessageModel messageModel);
    }
}