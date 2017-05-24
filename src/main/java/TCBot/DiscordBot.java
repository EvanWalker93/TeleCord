package TCBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

/**
 * Created by Evan on 5/3/2017.
 *
 */
public class DiscordBot extends ListenerAdapter {

    private static final String DISCORD_KEY = "MzA3OTU4OTE3MzExNDk2MTky.C-ahYw.hC1QdNoVFppzuueCTU3G-1o4FMY";
    private DiscordMessageListener listener;


    public interface DiscordMessageListener{
        void onDiscordMessageReceived(String message, MessageChannel channel, String author);
    }

    DiscordBot(DiscordMessageListener listener) throws LoginException, InterruptedException, RateLimitedException {
        this.listener = listener;

        JDA discordAPI = new JDABuilder(AccountType.BOT)
                .setToken(DISCORD_KEY)
                .buildBlocking();
        discordAPI.addEventListener(this);
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

        listener.onDiscordMessageReceived(content, event.getChannel(), event.getAuthor().getName());
    }
}