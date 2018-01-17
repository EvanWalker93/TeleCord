package main.java.TCBot;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import main.java.TCBot.model.ChannelObj;
import main.java.TCBot.model.MessageModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.LoggerFactory;

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

    void addChannelToDB(ChannelObj channelObj) {
        datastore.save(channelObj);
    }

    void removeChannelFromDB(ChannelObj channelObj) {
        for (ObjectId objectId : channelObj.getLinkedChannels()) {
            ChannelObj childChannel = getChannelObj(objectId);
            childChannel.removeChannelFromList(channelObj.getId());
            addChannelToDB(childChannel);
        }

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

