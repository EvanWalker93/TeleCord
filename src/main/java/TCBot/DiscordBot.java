package main.java.TCBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.login.LoginException;

/**
 * Created by Evan on 5/3/2017.
 *
 */
public class DiscordBot extends ListenerAdapter {

    private static final String DISCORD_KEY = "MzA3OTU4OTE3MzExNDk2MTky.C-ahYw.hC1QdNoVFppzuueCTU3G-1o4FMY";
    private final AtomicReference<JDA> jda;
    private DiscordMessageListener listener;


    public interface DiscordMessageListener{
        void onDiscordMessageReceived(String message, TextChannel channel, String author) throws IOException, TelegramApiException;
    }

    DiscordBot(DiscordMessageListener listener) throws LoginException, InterruptedException, RateLimitedException {
        this.listener = listener;

        jda = new AtomicReference<>();
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(DISCORD_KEY);
        jda.set(builder.buildBlocking());
        jda.get().addEventListener(this);

    }

    void sendMessageToChannelWithText(MessageChannel messageChannel, String message){
        System.out.println("Displaying message from Telegram on Discord, message and channel" + message + messageChannel.toString());
        messageChannel.sendMessage(message).queue();



        }

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        //Don't respond to bots
        if(event.getAuthor().isBot()) return;;

        //Store message content in String content
        Message message = event.getMessage();
        String content = (message.getRawContent());
        event.getTextChannel().getId();
        //TODO Handle attachments from Discord
        event.getMessage().getAttachments();

        try {
            listener.onDiscordMessageReceived(content, event.getTextChannel(), event.getAuthor().getName());
        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
        }
    }

    TextChannel getChannelFromID(String channel){
        System.out.println("GET TEXT CHANNEL BY ID RETURNS: " + getJda().getTextChannelById(channel).toString());
        return getJda().getTextChannelById(channel);
    }

    private JDA getJda() {
        return jda.get();
    }


}