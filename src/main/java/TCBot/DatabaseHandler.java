package main.java.TCBot;


import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

class DatabaseHandler {

    private MongoClient mongoClient;
    private MongoDatabase db ;
    private String host = "45.55.214.63";
    private int port = 27017;
    private String databaseName = "MessagesDatabase";
    private String collectionName = "messages";
    private MongoCollection<Document> messages;

    //Stops MongoDb from producing an excessive amount of logs
    private static Logger root = (Logger) LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME);

    static {
        root.setLevel(Level.INFO);
    }

    void init() {
       mongoClient = new MongoClient(host, port);
       db = mongoClient.getDatabase(databaseName);
       messages = db.getCollection(collectionName);
    }

    void addMessage(String username, String messageContent, String date, String channel, int messageOrigin) {
      Document message = new Document("username", username)
                       .append("message_content", messageContent)
                       .append("channel", channel)
                       .append("message_origin", messageOrigin)
                       .append("date", date);
      messages.insertOne((message));

       System.out.println("Added Message to Database");
    }
}

