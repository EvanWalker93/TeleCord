package main.java.TCBot;


import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;


public class DatabaseHandler {

    private MongoClient mongoClient;
    private MongoDatabase db ;
    private String host = "45.55.214.63";
    private int port = 27017;
    private String databaseName = "MessagesDatabase";
    private String collectionName = "messages";
    private MongoCollection<Document> messages;

   public void init(){
       mongoClient = new MongoClient(host, port);
       db = mongoClient.getDatabase(databaseName);
       messages = db.getCollection(collectionName);
    }


    public void addMessage(String username, String messageContent, String date, String channel, int messageOrigin){
      Document message = new Document("username", username)
                       .append("message_content", messageContent)
                       .append("channel", channel)
                       .append("message_origin", messageOrigin)
                       .append("date", date);
      messages.insertOne((message));

       System.out.println("Added Message to Database");
    }
}

