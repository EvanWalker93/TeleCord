package TCBot.model;

import TCBot.FileHandler;
import net.dv8tion.jda.core.entities.Message;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
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
    private UserModel user;
    private String messageText;
    private Date date;
    @Indexed(unique = false)
    private ChannelObj channel;
    @Transient
    private FileHandler fileHandler = new FileHandler();
    private Set<MessageModel> childMessages;

    public MessageModel() {
        super();
    }

    public MessageModel(Message message) {
        this.user = new UserModel(message);
        this.messageText = message.getContent();
        this.date = new Date();
        this.channel = new ChannelObj(message);
        this.messageId = message.getId();
    }

    public MessageModel(org.telegram.telegrambots.api.objects.Message message) {
        this.user = new UserModel(message);

        if(message.getCaption() != null){
            this.messageText = message.getCaption();
        }
        else{
            this.messageText = message.getText();
        }

        this.date = new Date();
        this.channel = new ChannelObj(message);
        this.messageId = message.getMessageId().toString();
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getMessageText() {
        return messageText;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
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

    public ChannelObj getChannel() {
        return channel;
    }

    public void setChannel(ChannelObj channel) {
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

    public boolean isCommand() {
        return messageText != null && !messageText.equals("") && messageText.replaceAll("\\s+|_", " ").substring(0, 1).equals("/");
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
        StringBuilder parameter = new StringBuilder();
        for(int i = 1; i < split.length; i++){
            parameter.append(split[i]);
        }
        return parameter.toString();
    }

    private String[] splitCommand(String messageText) {
        return messageText.replace("\\s+", " ").split(" ");
    }

    public boolean hasFile() {
        return fileHandler.hasFile();
    }

    public String getFormattedMessageText() {
        String messageText = this.messageText;
        String username = this.user.getUsername();

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

    @Override
    public String toString() {
        return "MessageModel{" +
                "id=" + id +
                ", messageId='" + messageId + '\'' +
                ", user=" + user +
                ", messageText='" + messageText + '\'' +
                ", date=" + date +
                ", channel=" + channel +
                ", fileHandler=" + fileHandler +
                ", childMessages=" + childMessages +
                '}';
    }
}
