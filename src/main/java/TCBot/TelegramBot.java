package main.java.TCBot;

import main.java.TCBot.model.channel.AbstractChannel;
import main.java.TCBot.model.MessageModel;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.send.SendVideo;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
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
        Message message = null;
        MessageModel messageModel = null;

        if (update.hasEditedMessage()) {
            MessageModel editedMessage = new MessageModel(update.getEditedMessage());
            listener.updateMessage(editedMessage);

        }
        else{
            message = update.getMessage();
            messageModel = new MessageModel(message);

            if (message.hasPhoto() || message.hasDocument() || message.getSticker() != null) {
                FileHandler fileHandler = new FileHandler(update);
                messageModel.setFileHandler(fileHandler);
            }
            listener.processMessage(messageModel);
        }

        //messageModel.setUserIsAdmin(isAdmin(message));


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

    TelegramBot(TelegramMessageListener listener) {
        this.listener = listener;
    }

    MessageModel sendMessageToChannel(AbstractChannel abstractChannel, MessageModel messageModel) {
        String channel = abstractChannel.getChannelId();
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
        String extension = fileHandler.getFileExtension().toLowerCase();
        String messageText = messageModel.getFormattedMessageText();

        switch (extension) {
            case "jpg":
            case "jpeg":
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
        void updateMessage(MessageModel messageModel);
    }
}