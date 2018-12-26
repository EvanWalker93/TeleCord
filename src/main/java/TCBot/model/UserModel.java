package TCBot.model;

import net.dv8tion.jda.core.entities.Message;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("User")
public class UserModel {

    @Id
    private ObjectId id;
    private String username;
    private String userId;

    public UserModel() {
        super();
    }

    UserModel(Message message) {
        this.username = message.getAuthor().getName();
        this.userId = message.getAuthor().getId();
    }

    UserModel(org.telegram.telegrambots.meta.api.objects.Message message) {
        this.username = message.getFrom().getUserName();
        if (this.username.equalsIgnoreCase("null")) {
            this.username = message.getFrom().getFirstName() + " " + message.getFrom().getLastName();
        }
        this.userId = message.getFrom().getId().toString();
    }

    String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
}
