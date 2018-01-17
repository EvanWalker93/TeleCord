package main.java.TCBot;

import main.java.TCBot.model.ChannelObj;
import main.java.TCBot.model.MessageModel;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.concurrent.atomic.AtomicReference;


public class DiscordBot extends ListenerAdapter {

    private final AtomicReference<JDA> jda;
    private DiscordMessageListener listener;

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

    MessageModel sendMessageToChannel(ChannelObj channelObj, MessageModel messageModel) {
        System.out.println("Discord bot: Sending message to Discord");
        TextChannel messageChannel = getChannelFromID(channelObj.getChannelId());
        Message msg = new MessageBuilder().append(messageModel.getFormattedMessageText()).build();

        if (messageModel.hasFile()) {
            messageChannel.sendFile(messageModel.getFileHandler().getFile(), messageModel.getFileHandler().getFileName(), msg).queue();

        } else {
            //TODO use .complete to sendToLinks and return the message to get the id for editing
            Message returnMessage = messageChannel.sendMessage(msg).complete();
            return new MessageModel(returnMessage);
        }
        return null;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        System.out.println(event.getTextChannel().getId());
        Message message = event.getMessage();
        MessageModel messageModel = new MessageModel(message);

        if (!message.getAttachments().isEmpty()) {
            FileHandler fileHandler = new FileHandler(message);
            messageModel.setFileHandler(fileHandler);
        }
        listener.processMessage(messageModel);

        //----------------------------------------

        System.out.println("Discord bot: Message received from Discord");
        //Don't respond to bots


        //TODO Add support for editing messages
        //This will probably require using a database that keeps the unique id's for both messages
        if (message.isEdited()) {
            message.getId();
        }
    }

    private TextChannel getChannelFromID(String channel) {
        return getJda().getTextChannelById(channel);

    }

    public String getSource() {
        return "Discord";
    }

    public interface DiscordMessageListener {
        void processMessage(MessageModel messageModel);
    }

    private JDA getJda() {
        return jda.get();
    }
}