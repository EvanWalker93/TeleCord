package main.java.TCBot.model;

import main.java.TCBot.FileHandler;
import net.dv8tion.jda.core.entities.Message;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import java.util.Date;
import java.util.Objects;

@Entity("messages")
public class MessageModel {

    @Id
    private String id;
    private String username;
    private String messageText = "";
    //private String channelId;
    private Date date;
    @Transient
    private ChannelObj channel;
    @Transient
    private FileHandler file = new FileHandler();

    public MessageModel() {
        super();
    }

    public MessageModel(Message message) {
        this.username = message.getAuthor().getName();
        this.messageText = message.getContent();
        this.date = new Date();

        this.channel = new ChannelObj(message);
    }

    public MessageModel(org.telegram.telegrambots.api.objects.Message message) {
        this.username = message.getFrom().getUserName();
        if (this.username.equalsIgnoreCase("null") || this.username == null) {
            this.username = message.getFrom().getFirstName() + " " + message.getFrom().getLastName();
        }
        this.messageText = message.getText();
        this.date = new Date();
        this.channel = new ChannelObj(message);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public ChannelObj getChannel() {
        return channel;
    }

    public void setChannel(ChannelObj channel) {
        this.channel = channel;
    }

    public boolean isCommand() {
        return messageText.replaceAll("\\s+", " ").substring(0, 1).equals("/");
    }

    public FileHandler getFile() {
        return file;
    }

    public void setFile(FileHandler file) {
        this.file = file;
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
        return file.hasFile();
    }

    public String getFormattedMessageText() {
        String messageText = this.messageText;
        String username = this.username;

        if (username == null) {
            return messageText;
        } else if (this.file != null) {
            if (messageText == null || Objects.equals(messageText, "")) {
                return ("File from " + username);
            } else {
                return (username + ": " + messageText);
            }
        }
        return null;
    }
}
