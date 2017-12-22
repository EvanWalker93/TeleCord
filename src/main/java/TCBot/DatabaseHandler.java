package main.java.TCBot;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import main.java.TCBot.model.DiscordChannel;
import main.java.TCBot.model.MessageModel;
import main.java.TCBot.model.TelegramChannel;
import org.bson.Document;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
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

    void addChannelToDB(DiscordChannel discordChannel) {
        datastore.save(discordChannel);
    }

    void addChannelToDB(TelegramChannel telegramChannel) {
        datastore.save(telegramChannel);
    }

    void removeChannelFromDB(DiscordChannel discordChannel) {
        datastore.delete(discordChannel);
    }

    void removeChannelFromDb(TelegramChannel telegramChannel) {
        datastore.delete(telegramChannel);
    }

    void removeChannelFromList(DiscordChannel discordChannel, String telegramChannelId) {
        UpdateOperations<DiscordChannel> ops = datastore.createUpdateOperations(DiscordChannel.class).removeAll("telegramChannels", telegramChannelId);
        datastore.update(discordChannel, ops);
    }

    void removeChannelFromList(TelegramChannel telegramChannel, String discordChannelId) {
        UpdateOperations<TelegramChannel> ops = datastore.createUpdateOperations(TelegramChannel.class).removeAll("discordChannels", discordChannelId);
        datastore.update(telegramChannel, ops);
    }

    Query<DiscordChannel> getDiscordChannelObj(String channelId) {
        return datastore.find(DiscordChannel.class).filter("channelId =", channelId);
    }

    Query<TelegramChannel> getTelegramChannelObj(String channelId) {
        return datastore.find(TelegramChannel.class).filter("channelId =", channelId);
    }

    TelegramChannel getTelegramChannelFromPassword(String password) {
        return datastore.find(TelegramChannel.class).filter("password =", password).get();
    }

    DiscordChannel getDiscordChannelFromPassword(String password) {
        return datastore.find(DiscordChannel.class).filter("password =", password).get();
    }

    void addToDiscordList(DiscordChannel query, String addChannelId) {
        UpdateOperations<DiscordChannel> ops = datastore.createUpdateOperations(DiscordChannel.class).add("telegramChannels", addChannelId);
        datastore.update(query, ops);
    }

    void addToTelegramList(TelegramChannel query, String addChannelId) {
        UpdateOperations<TelegramChannel> ops = datastore.createUpdateOperations(TelegramChannel.class).add("discordChannels", addChannelId);
        datastore.update(query, ops);
    }

    boolean uniquePassword(String password) {
        DiscordChannel discordChannel = datastore.find(DiscordChannel.class).filter("password =", password).get();
        TelegramChannel telegramChannel = datastore.find(TelegramChannel.class).filter("password =", password).get();

        if (discordChannel == null && telegramChannel == null) {
            System.out.println("DB: " + password + " is unique");
            return true;
        }
        System.out.println("DB: " + password + " already exists");
        return false;
    }
}

