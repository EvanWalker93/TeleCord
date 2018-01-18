package main.java.TCBot.model;

import main.java.TCBot.FileHandler;
import main.java.TCBot.model.channel.AbstractChannel;
import main.java.TCBot.model.channel.DiscordChannel;
import main.java.TCBot.model.channel.TelegramChannel;
import net.dv8tion.jda.core.entities.Message;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity("messages")
public class MessageModel {

    @Id
    private ObjectId id;
    private String messageId;
    private String username;
    private boolean userIsAdmin;
    private String messageText;
    private Date date;
    private AbstractChannel channel;
    @Transient
    private FileHandler fileHandler = new FileHandler();
    private Set<MessageModel> childMessages;

    public MessageModel() {
        super();
    }

    public MessageModel(Message message) {
        this.username = message.getAuthor().getName();
        this.messageText = message.getContent();
        this.date = new Date();
        this.channel = new DiscordChannel(message);
        this.messageId = message.getId();
    }

    public MessageModel(org.telegram.telegrambots.api.objects.Message message) {
        this.username = message.getFrom().getUserName();
        if (this.username.equalsIgnoreCase("null") || this.username == null) {
            this.username = message.getFrom().getFirstName() + " " + message.getFrom().getLastName();
        }
        this.messageText = message.getText();
        this.date = new Date();
        this.channel = new TelegramChannel(message);
        this.messageId = message.getMessageId().toString();
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public AbstractChannel getChannel() {
        return channel;
    }

    public void setChannel(AbstractChannel channel) {
        this.channel = channel;
    }

    public Set<MessageModel> getChildMessages() {
        return childMessages;
    }

    public void setChildMessages(Set<MessageModel> childMessages) {
        this.childMessages = childMessages;
    }

    public void addChildMessage(MessageModel messageModel) {
        if (childMessages == null) {
            childMessages = new HashSet<>();
        }
        childMessages.add(messageModel);
    }

    public boolean isUserIsAdmin() {
        return userIsAdmin;
    }

    public void setUserIsAdmin(boolean userIsAdmin) {
        this.userIsAdmin = userIsAdmin;
    }

    public boolean isCommand() {
        if (messageText == null || messageText.equals("")) {
            return false;
        } else {
            return messageText.replaceAll("\\s+", " ").substring(0, 1).equals("/");
        }
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    public void setFileHandler(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    public String getCommand() {
        String[] split = splitCommand(messageText);
        return split[0];
    }

    public boolean hasParameter() {
        String[] split = splitCommand(messageText);
        return split.length > 1;
    }

    public String getParameter() {
        String[] split = splitCommand(messageText);
        return split[1];
    }

    private String[] splitCommand(String messageText) {
        return messageText.replaceAll("\\s+", " ").split(" ");
    }

    public boolean hasFile() {
        return fileHandler.hasFile();
    }

    public String getFormattedMessageText() {
        String messageText = this.messageText;
        String username = this.username;

        if (username == null) {
            return messageText;
        } else if (this.fileHandler != null) {
            if (messageText == null || Objects.equals(messageText, "")) {
                return ("File from " + username);
            } else {
                return (username + ": " + messageText);
            }
        }
        return null;
    }
}
