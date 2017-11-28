package main.java.TCBot;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;

class DatabaseHandler {

    private MongoClient mongoClient;
    private MongoDatabase db ;
    private String host = "45.55.214.63";
    private int port = 27017;
    private MongoCollection<Document> messages;
    private MongoCollection<Document> discordChannels;
    private MongoCollection<Document> telegramChannels;

    //Stops MongoDb from producing an excessive amount of logs
    private static Logger root = (Logger) LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME);

    static {
        root.setLevel(Level.INFO);
    }

    void init() {
       mongoClient = new MongoClient(host, port);
        String databaseName = "MessagesDatabase";
        db = mongoClient.getDatabase(databaseName);
        String collectionName = "messages";
        messages = db.getCollection(collectionName);
        discordChannels = db.getCollection("discordChannels");
        telegramChannels = db.getCollection("telegramChannels");
    }

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

    void addDiscordChannelToDB(String channelID, String telegramChannel, String discordChannel) {

        //If there is no Discord channel with the given id in the database, create a new entry.
        if (discordChannels.count(new Document("_id", channelID)) == 0) {
            Document channel = new Document("_id", channelID)
                    .append("password", Integer.toString(channelID.hashCode()))
                    .append("Telegram_Channels", null)
                    .append("Discord_Channels", null);
            discordChannels.insertOne(channel);
        }

        if (telegramChannel != null) {
            discordChannels.updateOne(eq("_id", channelID),
                    new Document("$push", new Document("Telegram_Channels", telegramChannel)));
        }
        if (discordChannel != null) {
            discordChannels.updateOne(eq("_id", channelID),
                    new Document("$push", new Document("Discord_Channels", discordChannel)));
        }

    }

    void addTelegramChannelToDB(String channelID, String telegramChannel, String discordChannel) {

        if (telegramChannels.count(new Document("_id", channelID)) == 0) {
            System.out.println("!!!CHANNEL ID LOG: " + channelID);
            Document channel = new Document("_id", channelID)
                    .append("password", Integer.toString(channelID.hashCode()))
                    .append("Telegram_Channels", null)
                    .append("Discord_Channels", null);
            telegramChannels.insertOne(channel);

        }

        if (telegramChannel != null) {
            telegramChannels.updateOne(eq("_id", channelID),
                    new Document("$push", new Document("Telegram_Channels", telegramChannel)));
        }
        if (discordChannel != null) {

            telegramChannels.updateOne(eq("_id", channelID),
                    new Document("$push", new Document("Discord_Channels", discordChannel)));
        }
    }

    List getPairedDiscordChannels(String source, String channelID) {
        Document document = telegramChannels.find(eq("_id", channelID)).first();
        System.out.println(document.toString());
        return null;
    }
}

