package main.java.TCBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.impl.MessageEmbedImpl;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.login.LoginException;

import static javafx.scene.input.KeyCode.M;

/**
 * Created by Evan on 5/3/2017.
 */
public class DiscordBot extends ListenerAdapter {

    private static final String DISCORD_KEY = "MzUxNzI0NDkzNzY2NjU2MDAy.DJwjKQ.8xqivVFqzKeA06X0TytVTinUTZY";
    private final AtomicReference<JDA> jda;
    private DiscordMessageListener listener;
    Message message;
    String content;
    TextChannel channel;
    String userName;
    private String fileName;
    private File file = null;

    void sendMessageToChannel(MessageChannel messageChannel, String message, File file) throws IOException {
        System.out.println("Displaying message from Telegram on Discord, message and channel" + message + messageChannel.toString());
        Message msg = new MessageBuilder().append(message).build();

        if (file != null) {
            messageChannel.sendFile(file, msg).queue();
            
        } else {
            messageChannel.sendMessage(msg).queue();
        }
    }

    DiscordBot(DiscordMessageListener listener) throws LoginException, InterruptedException, RateLimitedException {
        this.listener = listener;

        jda = new AtomicReference<>();
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(DISCORD_KEY);
        jda.set(builder.buildBlocking());
        jda.get().addEventListener(this);

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //Don't respond to bots
        if (event.getAuthor().isBot()) return;

        //TODO Add support for editing messages
        //This will probably require using a database that keeps the unique id's for both messages
        if (event.getMessage().isEdited()) {
            event.getMessage().getId();
        }

        //Store message content to pass to Telegram
        message = event.getMessage();
        content = (message.getContent());
        channel = event.getTextChannel();
        userName = event.getAuthor().getName();

        if (!message.getAttachments().isEmpty()) {
            fileName = message.getAttachments().get(0).getFileName();
            file = new File(fileName);

            try {
                message.getAttachments().get(0).download(file);
            } catch (Exception e) {
                file.delete();
                file = null;
                e.printStackTrace();
            }
        }

        //Pass the Discord message over to the TeleCordBot main class to decide how message will be handled.
        //Contains the message text, the user who sent it, the channel it was from, and any attachments.
        try {
            listener.onDiscordMessageReceived(content, channel, userName, file);
            if (file != null) {
                file.delete();
                file = null;
            }
        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
        }
    }

    TextChannel getChannelFromID(String channel) {
        //System.out.println("GET TEXT CHANNEL BY ID RETURNS: " + getJda().getTextChannelById(channel).toString());
        return getJda().getTextChannelById(channel);
    }

    public interface DiscordMessageListener {
        void onDiscordMessageReceived(String message, TextChannel channel, String author, File attachment) throws IOException, TelegramApiException;
    }

    private JDA getJda() {
        return jda.get();
    }
}