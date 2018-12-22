package TCBot;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import TCBot.model.ChannelObj;
import TCBot.model.MessageModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class DatabaseHandler {

    private String host = TeleCordProps.getInstance().getProperty("dbAddress");
    private int port = Integer.parseInt(TeleCordProps.getInstance().getProperty("dbPort"));
    private String adminDB = TeleCordProps.getInstance().getProperty("adminDB");
    private String collection = TeleCordProps.getInstance().getProperty("dbCollection");
    private String activeDB = TeleCordProps.getInstance().getProperty("activeDBName");
    private String userName = TeleCordProps.getInstance().getProperty("dbUserName");
    private String password = TeleCordProps.getInstance().getProperty("dbPassword");

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
        ServerAddress address = new ServerAddress(host, port);
        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
        MongoCredential credential = MongoCredential.createCredential(userName, adminDB, password.toCharArray());
        credentials.add(credential);
        MongoClient mongoClient = new MongoClient(address, credentials);


        MongoDatabase db = mongoClient.getDatabase(activeDB);
        messages = db.getCollection(collection);
        morphia.mapPackage("main.java.TCBot.model");
        datastore = morphia.createDatastore(mongoClient, db.getName());
        datastore.getDB();
        //datastore.ensureIndexes();
    }

    void addMessageToDB(MessageModel message) {
        datastore.save(message);
    }

    void addChannelToDB(ChannelObj channelObj) {
        datastore.save(channelObj);
    }

    MessageModel getMessage(MessageModel messageModel){
        return datastore.find(MessageModel.class)
                .filter("messageId =", messageModel.getMessageId())
                .filter("channel.channelId =", messageModel.getChannel().getChannelId())
                .filter("channel.source =", messageModel.getChannel().getSource())
                .get();
    }

    void removeChannelFromDB(ChannelObj channelObj) {
        //Remove the channel from all children channels
        for (ObjectId objectId : channelObj.getLinkedChannels()) {
            ChannelObj childChannel = getChannelObj(objectId);
            childChannel.removeChannelFromList(channelObj.getId());
            addChannelToDB(childChannel);
        }

        datastore.delete(channelObj);
    }

    void removeMessageFromDB(MessageModel messageModel){
        messageModel = getMessage(messageModel);
        datastore.delete(messageModel);
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

    List<MessageModel> getAllMessages(MessageModel messageModel){
        return datastore.find(MessageModel.class)
                .filter("channel =", messageModel.getChannel())
                .filter("user =", messageModel.getUser())
                .asList();
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

