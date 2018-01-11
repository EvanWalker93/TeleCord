package main.java.TCBot;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import main.java.TCBot.model.ChannelObj;
import main.java.TCBot.model.MessageModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class DatabaseHandler {

    private MongoClient mongoClient;
    private MongoDatabase db;
    private String host = "45.55.214.63";
    private int port = 27017;
    private MongoCollection<Document> messages;
    private MongoCollection<Document> discordChannelsCollection;
    private MongoCollection<Document> telegramChannelsCollection;
    private FindIterable<Document> findIterable;
    private List<String> list = new ArrayList<>();
    private Morphia morphia = new Morphia();
    private Datastore datastore = null;

    //Stops MongoDb from producing an excessive amount of logs
    private static Logger root = (Logger) LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME);

    static {
        root.setLevel(Level.INFO);
    }

    void init() {
        mongoClient = new MongoClient(host, port);
        db = mongoClient.getDatabase("MessagesDatabase");
        messages = db.getCollection("messages");
        discordChannelsCollection = db.getCollection("discordChannelsCollection");
        telegramChannelsCollection = db.getCollection("telegramChannelsCollection");

        morphia.mapPackage("main.java.TCBot.model");
        datastore = morphia.createDatastore(mongoClient, db.getName());
        datastore.getDB();
        datastore.ensureIndexes();
    }

    @Deprecated
    void addMessage(String username, String messageContent, String date, String channel, String messageOrigin, FileHandler file) {
        Document message = new Document("username", username)
                .append("message_content", messageContent)
                .append("has_file", !file.getFileName().isEmpty())
                .append("channel", channel)
                .append("message_origin", messageOrigin)
                .append("date", date);
        messages.insertOne((message));

        System.out.println("Added Message to Database");
    }

    void addMessageToDB(MessageModel message) {
        datastore.save(message);
    }

    void addChannelToDB(ChannelObj channelObj) {
        datastore.save(channelObj);
    }

    void removeChannelFromDB(ChannelObj channelObj) {
        datastore.delete(channelObj);
    }

    ChannelObj getChannelObj(ChannelObj channelObj) {
        return datastore.find(ChannelObj.class)
                .filter("channelId =", channelObj.getChannelId())
                .filter("source =", channelObj.getSource())
                .get();
    }

    ChannelObj getChannelObj(ObjectId id) {
        return datastore.find(ChannelObj.class)
                .filter("_id =", id)
                .get();
    }

    ChannelObj getChannelFromPassword(String password) {
        return datastore.find(ChannelObj.class)
                .filter("password =", password)
                .get();
    }

    boolean channelExists(ChannelObj channelObj) {
        return datastore.find(ChannelObj.class)
                .filter("channelId =", channelObj.getChannelId())
                .filter("source =", channelObj.getSource())
                .asList() != null;
    }

    boolean uniquePassword(String password) {
        ChannelObj channel = datastore.find(ChannelObj.class)
                .filter("password =", password)
                .get();

        if (channel == null) {
            System.out.println("DB: " + password + " is unique");
            return true;
        }
        System.out.println("DB: " + password + " already exists");
        return false;
    }
}

