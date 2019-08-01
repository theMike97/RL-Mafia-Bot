import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.MiscUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MessageReactionListener extends ListenerAdapter {

    private int lobbysize;
    private UsersQueue q;
    private DefaultEmbedBuilder eb;
    private boolean correctMessage;
    private int reactions = 0;

    private static String[] EMOTE_LIST = new String[]{
            "\uD83C\uDDE6", //A
            "\uD83C\uDDE7", //B
            "\uD83C\uDDE8", //C
            "\uD83C\uDDE9", //D
            "\uD83C\uDDEA", //E
            "\uD83C\uDDEB", //F
            "\uD83C\uDDEC", //G
            "\uD83C\uDDED", //H
            "\u2705",       //white check
            "\u274E"        //cross mark
    };

    public MessageReactionListener() {
        lobbysize = 6;
        q = null;
        eb = new DefaultEmbedBuilder();
        correctMessage = false;
        reactions = 0;
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        reactions++; // any automatically removed reaction will subtract from this through onGuildMessageReactionRemove(...)
        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        String emessage = "";
        String field = "";
        /* Determine if the reaction is to a message from the bot */

        Message message = getReactedMessage(event);
        TextChannel tc = event.getChannel();

        if (message != null) {
            // we have the right message
            User reactor = event.getUser();
//                                System.out.println("qdUsers: " + q);
            if (q.contains(reactor)) {
                // delete any reactions other than those in EMOTE_LIST
                boolean emoteFound = false;
                int reactedEmote = 0;
                for (int i = 0; i < EMOTE_LIST.length; i++) {
                    if (MiscUtil.encodeUTF8(emote.getName()).equals(MiscUtil.encodeUTF8(EMOTE_LIST[i]))) {
                        emoteFound = true;
                        reactedEmote = i;
                        break;
                    }
                }
                if (emoteFound) {
                    Player currentPlayer = q.get(q.indexOf(reactor));
                    if (currentPlayer.hasGuessed()) {
                        // remove old reaction
                        for (MessageReaction reaction : message.getReactions()) {
                            if (reaction.getUsers().complete().equals(currentPlayer.getUser())) {
                                reaction.removeReaction(currentPlayer.getUser());
//                                reactions--;
                                System.out.println(reactions);
                            }
                        }
                    }
                    // add new reaction
                    if (!(reactedEmote > q.size() - 1)) {
                        currentPlayer.setGuess(q.get(reactedEmote)); //record guess
                        System.out.println(reactions);
                        System.out.println(reactor.getName() + " voted " + q.get(reactedEmote).getUser().getName() + " as mafia.");
                    } else {
                        event.getReaction().removeReaction(reactor).queue();
                        System.err.println("Issue recording guess.");
                        return;
                    }
                } else {
                    event.getReaction().removeReaction(reactor).queue();
                    System.err.println("Not in approved emote list");
                    return;
                }
            } else {
                event.getReaction().removeReaction(reactor).queue();
                eb.setDescription(reactor.getAsMention() + " Only players in the current game can vote.");
                //                            tc.sendMessage(reactor.getAsMention() + " Only players in the current game can vote.").queue();
                tc.sendMessage(eb.build()).queue();
            }

            System.out.println("lobbysize = " + lobbysize);

            if (reactions == lobbysize) { // everyone has voted
                System.out.println("here");
                String guessedMafia = "";
                int i = 0;
                do {
                    if (q.get(i).isMafia()) {
                        eb.clearFields();
                        field = "The mafia was " + q.get(i).getUser().getName() + "!";
//                    tc.sendMessage("The mafia was " + q.get(i).getUser().getAsTag() + "!");
                    }
                } while (!q.get(i++).isMafia());

                for (Player player : q) {
                    if (player.getGuess().isMafia()) guessedMafia += ", " + player.getUser().getAsMention();
                }
                guessedMafia = (guessedMafia.equals("")) ? "Nobody" : guessedMafia.substring(2);
//            tc.sendMessage(guessedMafia + " guessed correctly!");
                emessage = guessedMafia + " guessed correctly!";
                eb.addField(field, emessage, false);
                tc.sendMessage(eb.build()).queue();

                lobbysize = 0;
                q = null;
                reactions = 0;
            }
        }
        for (String emoteStr : EMOTE_LIST) {
            if (MiscUtil.encodeUTF8(emote.getName()).equals(MiscUtil.encodeUTF8(emoteStr))) System.out.println("supported");
        }

    }

    @Override
    public void onGuildMessageReactionRemove(@NotNull GuildMessageReactionRemoveEvent event) {

        Message message = getReactedMessage(event);

        if (message != null) {
            reactions--;
            System.out.println(reactions);
        }
    }

    @Nullable
    private Message getReactedMessage(@NotNull GenericGuildMessageReactionEvent event) {
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
                if (message.getEmbeds().size() > 0) {
                    MessageEmbed embed = message.getEmbeds().get(0);

                    if (embed.getFields().size() > 0 // skip message without fields
                            && embed.getFields().get(embed.getFields().size() - 1).getValue().equals(
                                    "React with respective emotes to vote and with :white_check_mark: or :negative_squared_cross_mark: if you won or lost.") // is message votable?
                            && message.getIdLong() == rmessage.getIdLong()) { // confirm message ID

                        // we have the right message
                        return message;
                    }
                }
            }
        }
        return null; // returns null for all other messages
    }

    public void retrieveLobbySize(int size) { // might have to change this for multithreading
        lobbysize = size;
//        System.out.println(lobbysize);
    }

    public void retrieveQdUsers(UsersQueue q) {
        this.q = q;
    }
}
