package main.java.TCBot;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import main.java.TCBot.model.channel.AbstractChannel;
import main.java.TCBot.model.MessageModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;

class DatabaseHandler {

    private String host = "45.55.214.63";
    private int port = 27017;
    private MongoCollection<Document> messages;
    private Morphia morphia = new Morphia();
    private Datastore datastore = null;

    //Stops MongoDb from producing an excessive amount of logs
    private static Logger root = (Logger) LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME);

    static {
        root.setLevel(Level.INFO);
    }

    void init() {
        MongoClient mongoClient = new MongoClient(host, port);
        MongoDatabase db = mongoClient.getDatabase("MessagesDatabase");
        messages = db.getCollection("messages");

        morphia.mapPackage("main.java.TCBot.model");
        datastore = morphia.createDatastore(mongoClient, db.getName());
        datastore.getDB();
        datastore.ensureIndexes();
    }


    void addMessageToDB(MessageModel message) {
        datastore.save(message);
    }

    void addChannelToDB(AbstractChannel abstractChannel) {
        datastore.save(abstractChannel);
    }

    void removeChannelFromDB(AbstractChannel abstractChannel) {
        for (ObjectId objectId : abstractChannel.getLinkedChannels()) {
            AbstractChannel childChannel = getChannelObj(objectId);
            childChannel.removeChannelFromList(abstractChannel.getId());
            addChannelToDB(childChannel);
        }

        datastore.delete(abstractChannel);
    }

    AbstractChannel getChannelObj(AbstractChannel abstractChannel) {
        return datastore.find(AbstractChannel.class)
                .filter("channelId =", abstractChannel.getChannelId())
                .filter("source =", abstractChannel.getSource())
                .get();
    }

    AbstractChannel getChannelObj(ObjectId id) {
        return datastore.find(AbstractChannel.class)
                .filter("_id =", id)
                .get();
    }

    AbstractChannel getChannelFromPassword(String password) {
        return datastore.find(AbstractChannel.class)
                .filter("password =", password)
                .get();
    }

    boolean channelExists(AbstractChannel abstractChannel) {
        return datastore.find(AbstractChannel.class)
                .filter("channelId =", abstractChannel.getChannelId())
                .filter("source =", abstractChannel.getSource())
                .asList() != null;
    }

    boolean uniquePassword(String password) {
        AbstractChannel channel = datastore.find(AbstractChannel.class)
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

