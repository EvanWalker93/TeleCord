package TCBot;

import TCBot.model.ChannelObj;
import TCBot.model.MessageModel;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


public class DiscordBot extends ListenerAdapter {

    private final AtomicReference<JDA> jda;
    private DiscordMessageListener listener;

    DiscordBot(DiscordMessageListener listener) throws LoginException {
        this.listener = listener;

        jda = new AtomicReference<>();
        String token = Objects.requireNonNull(TeleCordProps.getInstance()).getProperty("discordBotToken");
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(token);
        jda.set(builder.build());
        jda.get().addEventListener(this);

    }

    MessageModel sendMessageToChannel(ChannelObj channelObj, MessageModel messageModel) {
        System.out.println("Discord bot: Sending message to Discord");
        TextChannel messageChannel = getChannelFromID(channelObj.getChannelId());
        Message msg = new MessageBuilder().append(messageModel.getFormattedMessageText()).build();

        if (messageModel.hasFile()) {
            Message returnMessage = messageChannel.sendFile(messageModel.getFileHandler().getFile(), messageModel.getFileHandler().getFileName(), msg).complete();
            return new MessageModel(returnMessage);
        } else if (messageModel.getMessageText() != null) {
            Message returnMessage = messageChannel.sendMessage(msg).complete();
            return new MessageModel(returnMessage);
        }
        return null;
    }

    void updateMessage(MessageModel editedMessage) {
        TextChannel textChannel = getChannelFromID(editedMessage.getChannel().getChannelId());
        textChannel.editMessageById(editedMessage.getMessageId(), editedMessage.getMessageText()).queue();
    }

    void deleteMessage(MessageModel deleteMessage){
        TextChannel textChannel = getChannelFromID(deleteMessage.getChannel().getChannelId());
        textChannel.deleteMessageById(deleteMessage.getMessageId()).queue();
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event){
        if(!event.getAuthor().isBot()){
            Message message = event.getMessage();
            MessageModel messageModel = new MessageModel(message);
            listener.updateMessage(messageModel);
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        System.out.println("Discord message Deleted event");
        MessageModel message = new MessageModel(event);
        listener.deleteMessage(message);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        MessageModel messageModel = new MessageModel(message);

        if (!message.getAttachments().isEmpty()) {
            FileHandler fileHandler = new FileHandler(message);
            messageModel.setFileHandler(fileHandler);
        }
            listener.processMessage(messageModel);
    }

    private TextChannel getChannelFromID(String channel) {
        return getJda().getTextChannelById(channel);

    }

    boolean isAdmin(MessageModel message){
        TextChannel channel = getChannelFromID(message.getChannel().getChannelId());
        User user = getJda().getUserById(message.getUser().getUserId());
        Guild guild = channel.getGuild();
        Member member = guild.getMember(user);
        List<Permission> permissions = member.getPermissions(channel);

        for(Permission permission : permissions){
            if(permission == Permission.MANAGE_CHANNEL){
                return true;
            }
        }
        return false;
    }

    public interface DiscordMessageListener {
        void processMessage(MessageModel messageModel);
        void updateMessage(MessageModel messageModel);
        void deleteMessage(MessageModel messageModel);
    }

    private JDA getJda() {
        return jda.get();
    }
}