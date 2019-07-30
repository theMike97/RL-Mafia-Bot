import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.MiscUtil;

import java.util.ArrayList;
import java.util.List;

public class MessageReactionListener extends ListenerAdapter {

    private int lobbysize = 6;
    private UsersQueue q = null;
    private static String[] EMOTE_LIST = new String[]{
            "\uD83C\uDDE6",
            "\uD83C\uDDE7",
            "\uD83C\uDDE8",
            "\uD83C\uDDE9",
            "\uD83C\uDDEA",
            "\uD83C\uDDEB",
            "\uD83C\uDDEC"
    };


    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        int reactions = 0;
        /* Determine if the reaction is to a message from the bot */

        // get the reacted text channel
        TextChannel tc = event.getChannel();
        MessageHistory history = new MessageHistory(tc);
        // get reacted message
        Message rmessage = tc.getMessageById(event.getMessageIdLong()).complete();
        // get reacted message author
        User author = rmessage.getAuthor();
        // check if message was sent by mafia bot
        if (author.getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
            List<Message> messages = history.retrievePast(20).complete();

            for (Message message : messages) {
                if (rmessage.getContentRaw().endsWith("React to vote.")) { // if the message is votable
                    if (message.getContentRaw().equals(rmessage.getContentRaw())) { // if the votable message matches the reacted message
                        // we have the right message
                        // get author of reaction
                        User reactor = event.getUser();
                        System.out.println("qdUsers: " + q);
                        if (q.contains(reactor)) {
                            // delete any reactions other than those in EMOTE_LIST
                            for (int i = 0; i < EMOTE_LIST.length; i++) {
                                if (MiscUtil.encodeUTF8(emote.getName()).equals(MiscUtil.encodeUTF8(EMOTE_LIST[i]))) {
                                    if (!(i > q.size()-1)) {
                                        q.get(q.indexOf(reactor)).setGuess(q.get(i)); //record guess
                                        System.out.println(reactor.getAsTag() + " voted " + q.get(i).getUser().getAsTag() + " as mafia.");
                                        break;
                                    } else {
                                        event.getReaction().removeReaction(reactor).queue();
                                        System.err.println("Issue recording guess.");
                                        break;
                                    }
                                } else{
                                    event.getReaction().removeReaction(reactor).queue();
                                    System.err.println("Not in approved emote list");
                                    break;
                                }
                            }
                        } else {
                            event.getReaction().removeReaction(reactor).queue();
                            tc.sendMessage(reactor.getAsMention() + " Only players in the current game can vote.").queue();
                        }
                        reactions = rmessage.getReactions().size();
                        System.out.println(reactions);
                        break;
                    }
                }
            }
        }
        if (reactions == lobbysize) { // everyone has voted
            String guessedMafia = "";
            int i = 0;
            do {
                if (q.get(i).isMafia())
                    tc.sendMessage("The mafia was " + q.get(i).getUser().getAsTag() + "!");
                i++;
            } while(!q.get(i).isMafia());

            for (Player player : q) {
                if (player.getGuess().isMafia()) guessedMafia += ", " + player.getUser().getAsMention();
            }
            guessedMafia = guessedMafia.substring(2);
            tc.sendMessage(guessedMafia + " guessed correctly!");

            lobbysize = 0;
            q = null;
        }
    }

    public void retrieveLobbySize(int size) { // might have to change this for multithreading
        lobbysize = size;
//        System.out.println(lobbysize);
    }

    public void retrieveQdUsers(UsersQueue q) {
        this.q = q;
    }
}
