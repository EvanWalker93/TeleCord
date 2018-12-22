package TCBot.model;

import net.dv8tion.jda.core.entities.Message;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.HashSet;
import java.util.Set;

@Entity("Channel")
@Indexes(@Index(fields = {@Field("channelId"), @Field("source")}, options = @IndexOptions(unique = true)))
public class ChannelObj {

    @Id
    private ObjectId id;
    private String channelId;
    private String channelName;
    private String password;
    private String source;
    private Set<ObjectId> linkedChannels;
    private Set<ObjectId> requestLinkChannels;


    public ChannelObj() {
        super();
    }

    public ChannelObj(String channelId) {
        this.channelId = channelId;
    }

    ChannelObj(Message message) {
        this.channelId = message.getTextChannel().getId();
        this.channelName = message.getTextChannel().getName();
        this.source = "Discord";
    }

    ChannelObj(org.telegram.telegrambots.api.objects.Message message) {
        this.channelId = message.getChatId().toString();
        this.channelName = message.getChat().getTitle();
        this.source = "Telegram";
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Set<ObjectId> getLinkedChannels() {
        return linkedChannels;
    }

    public void setLinkedChannels(Set<ObjectId> linkedChannels) {
        this.linkedChannels = linkedChannels;
    }

    public Set<ObjectId> getRequestLinkChannels() {
        return requestLinkChannels;
    }

    public void setRequestLinkChannels(Set<ObjectId> requestLinkChannels) {
        this.requestLinkChannels = requestLinkChannels;
    }

    public void addRequestLink(ObjectId id){
        if(requestLinkChannels == null){
            this.requestLinkChannels = new HashSet<>();
        }
        requestLinkChannels.add(id);
    }

    public void linkChannel(ObjectId id) {
        if (linkedChannels == null) {
            this.linkedChannels = new HashSet<>();
        }
        linkedChannels.add(id);
    }

    public boolean removeChannelFromList(ObjectId id) {
        return linkedChannels.remove(id);
    }
}
