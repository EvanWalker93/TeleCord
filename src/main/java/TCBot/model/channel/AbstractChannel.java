package main.java.TCBot.model.channel;

import main.java.TCBot.model.MessageModel;
import net.dv8tion.jda.core.entities.Message;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.HashSet;
import java.util.Set;

@Entity("Channel")
@Indexes(@Index(fields = {@Field("channelId"), @Field("source")}, options = @IndexOptions(unique = true)))
public abstract class AbstractChannel {

    @Id
    private ObjectId id;
    protected String channelId;
    protected String channelName;
    private String password;
    private String source;
    private Set<ObjectId> linkedChannels;


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
