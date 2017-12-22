package main.java.TCBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;


public class DiscordBot extends ListenerAdapter {

    private final AtomicReference<JDA> jda;
    private DiscordMessageListener listener;
    private FileHandler file = null;
    private String source = "Discord";

    DiscordBot(DiscordMessageListener listener) throws LoginException, InterruptedException, RateLimitedException {
        this.listener = listener;

        jda = new AtomicReference<>();
        TokenReader tokenReader = new TokenReader();
        String token = tokenReader.getTokens("discordToken");
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(token);
        jda.set(builder.buildBlocking());
        jda.get().addEventListener(this);

    }

    void sendMessageToChannel(MessageChannel messageChannel, String message, FileHandler fis) throws IOException {
        System.out.println("Discord bot: Sending message to Discord");
        Message msg = new MessageBuilder().append(message).build();

        if (fis != null) {
            messageChannel.sendFile(fis.getFis(), fis.getFileName(), msg).queue();

        } else {
            messageChannel.sendMessage(msg).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        System.out.println("Discord bot: Message received from Discord");
        //Don't respond to bots
        if (event.getAuthor().isBot()) return;

        //TODO Add support for editing messages
        //This will probably require using a database that keeps the unique id's for both messages
        if (message.isEdited()) {
            message.getId();
        }

        try {
            file = new FileHandler(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Pass the Discord message over to the TeleCordBot main class to decide how message will be handled.
        //Contains the message text, the user who sent it, the channel it was from, and any attachments.
        try {
            listener.onDiscordMessageReceived(message, file);
        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
        }
    }


    TextChannel getChannelFromID(String channel) {
        return getJda().getTextChannelById(channel);
    }

    public String getSource() {
        return source;
    }

    public interface DiscordMessageListener {
        void onDiscordMessageReceived(Message message, FileHandler attachment) throws IOException, TelegramApiException;
    }

    private JDA getJda() {
        return jda.get();
    }
}