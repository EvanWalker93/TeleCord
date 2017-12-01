package main.java.TCBot.model;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;

@Entity("messages")
public class MessageModel {

    @Id
    private String id;
    private String username;
    private String messageText;
    private String channel;
    private String origin;
    private Date date;

    public MessageModel() {
    }

    public MessageModel(String username, String messageText, String channel, String origin, Date date) {
        super();
        this.username = username;
        this.messageText = messageText;
        this.channel = channel;
        this.origin = origin;
        this.date = date;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
