import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import exceptions.LobbyTooBigException;
import exceptions.LobbyTooSmallException;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import org.jetbrains.annotations.Nullable;
import net.dv8tion.jda.core.EmbedBuilder;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Hex;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;


public class ListenerMain extends ListenerAdapter {

    private MessageReactionListener mrl;
    private UsersQueue qdUsers;
    private MafiaGame game;
    private String[] voteList;
    private DefaultEmbedBuilder eb;

    private GoogleCredentials credentials;
    private FirebaseOptions options;
    private Guild guild;
    private Firestore firedb;
    private String serverID;

    public ListenerMain(MessageReactionListener mrl) throws IOException {
        super();
        this.mrl = mrl;
        qdUsers = new UsersQueue(6, 5);
        game = new MafiaGame(qdUsers);
        voteList = new String[]{
                ":regional_indicator_a:",
                ":regional_indicator_b:",
                ":regional_indicator_c:",
                ":regional_indicator_d:",
                ":regional_indicator_e:",
                ":regional_indicator_f:",
                ":regional_indicator_b:"
        };
        eb = new DefaultEmbedBuilder();
//        initFirebase("rl-mafia-bot");
//        serverID = "";
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (!eb.isEmpty()) eb.clear();

        MessageChannel curChannel = event.getChannel();
        Message inMessage = event.getMessage();
        Player inAuthor = new Player(event.getAuthor());
        String emessage = "";
        String field = "";

        if (inAuthor.getUser().isBot()) return;


        if (inMessage.getContentRaw().equals("!help")) {
            emessage = "\t!q - this adds you to the mafia queue.\n" +
                    "\t!1 - this removes you from the mafia queue.\n" +
                    "\t!getq - this shows the users currently in the queue.\n" +
                    "\t!setsize [#] - this sets lobby capacity (set to 6 by default).\n" +
                    "\t!size - this shows lobby size.\n" +
                    "\t!series - this shows the current series length.\n" +
                    "\t!setseries [#] - this sets the series length.\n" +
                    "\t!clear - this clears the queue.\n" +
                    "\t!reset - this clears the queue and stops any active series.\n" +
                    "\t!serverlb - this shows the top 10 mafia players by series won in the server.*\n" +
                    "\t!lb - this shows the top 20 mafia players by series won worldwide.*\n" +
                    "\t!stats - this shows your personal stats.*\n" +
                    "\t!rules - this shows a list of the rules for earning points and winning games.*\n" +
                    "\t!help - I wonder how you got here????";
            eb.addField("Here is a list of available commands:",emessage,false);
            curChannel.sendMessage(eb.build()).queue();
//            curChannel.sendMessage(":" + voteList[2] + ":").queue();
        }
        // !q
        else if (inMessage.getContentRaw().equals("!q")) {
            if (qdUsers.contains(inAuthor)) {
                eb.setDescription("You are already in the queue!");
//                curChannel.sendMessage("You are already in the queue!").queue();
                curChannel.sendMessage(eb.build()).queue();
                return;
            }
            if (qdUsers.size() > qdUsers.getLobbySize() - 1) {
                curChannel.sendMessage("Lobby is full!").queue();
                return;
            }
            qdUsers.add(inAuthor);
//            curChannel.sendMessage("Added " + inAuthor.getUser().getName()).queue();
            field = "**Added " + inAuthor.getUser().getName() + "**";
            emessage = "";
            if (qdUsers.size() == qdUsers.getLobbySize()) {
                qdUsers.setPartyFull(true);
//                curChannel.sendMessage("Queue is now full!  Choosing mafia...").queue();
//                curChannel.sendMessage("Queue is now full!  Randomly assigning teams.\n").queue();
                emessage += "\nQueue is now full!  Randomly assigning teams.";
                eb.addField(field, emessage, false);
                if (qdUsers.getLobbySize() > 1) { // testing only

                    sendVoteMessage(curChannel).queue(
                            message -> {
                                mrl.retrieveQdUsers(qdUsers);
                            }
                    );
                    game.startSeries(qdUsers.getSeriesLength(), qdUsers);
                }
                game.chooseMafia();

                //voting reactions

                return;
            }

//            curChannel.sendMessage("Queue now contains: " + qdUsers + "\n" +
//                    "Waiting on " + qdUsers.getFreeSpaces() + " more " + ((qdUsers.getFreeSpaces() == 1) ? "person." : "people.")).queue();
            emessage += "\nQueue now contains: " + qdUsers + "\n" +
                    "Waiting on " + qdUsers.getFreeSpaces() + " more " + ((qdUsers.getFreeSpaces() == 1) ? "person." : "people.");
                    eb.addField(field, emessage, false);
                    curChannel.sendMessage(eb.build()).queue();
        }
        // !l
        else if (inMessage.getContentRaw().equals("!l")) {
            if (!qdUsers.contains(inAuthor)) {
//                curChannel.sendMessage("You are not in the queue!").queue();
                curChannel.sendMessage(eb.setDescription("You are not in the queue!").build()).queue();
                return;
            }
            if (game.getGameStatus() != game.IN_QUEUE) {
//                curChannel.sendMessage("Cannot leave mid-series").queue();
                curChannel.sendMessage(eb.setDescription("Cannot leave mid-series").build()).queue();
                return;
            }

            boolean isRemoved = qdUsers.remove(inAuthor);
            if (isRemoved)
//                curChannel.sendMessage("Removed " + inAuthor.getUser().getAsMention()).queue();
                field = "Removed " + inAuthor.getUser().getName();
            else
//                curChannel.sendMessage("Error removing " + inAuthor.getUser().getAsMention() + " from the queue D:").queue();
                field = "Error removing \" + inAuthor.getUser().getAsMention() + \" from the queue D:";

//            curChannel.sendMessage("The queue is currently " + (qdUsers.isEmpty() ? "Empty" : qdUsers)).queue();
            emessage += "\nThe queue is currently " + (qdUsers.isEmpty() ? "Empty" : qdUsers);
            if (!qdUsers.isEmpty())
//                curChannel.sendMessage("Waiting on " + qdUsers.getFreeSpaces() + " more people.").queue();
                emessage += "\nWaiting on " + qdUsers.getFreeSpaces() + " more people.";

            eb.addField(field, emessage, false);
            curChannel.sendMessage(eb.build()).queue();
        }
        //!getq
        else if (inMessage.getContentRaw().equals("!getq")) {
            field = "Current queue";
            emessage = (qdUsers.isEmpty() ? "Empty" : qdUsers.toString());
            eb.addField(field, emessage, false);
            curChannel.sendMessage(eb.build()).queue();
        }
        //!setsize [#]
        else if (inMessage.getContentRaw().contains("!setsize")) {
            try {
                int size = Integer.parseInt(inMessage.getContentRaw().split("\\s+")[1]);
                if (size % 2 == 1) { // odd lobby size (unbalanced teams)
                    curChannel.sendMessage(
                            eb.addField("Lobby size unchanged", "Lobbies must have an even number of slots to avoid unbalanced teams.", false).build()
                    ).queue();
                    return;
                }
                qdUsers.setLobbySize(size);
                qdUsers.setPartyFull(qdUsers.getLobbySize() == qdUsers.size());
                if (qdUsers.isPartyFull()) {
                    field = "Lobby size is now " + qdUsers.getLobbySize() + "!";
                    emessage = "Queue is now full! Randomly assigning teams.";
                    eb.addField(field, emessage, false);
                    sendVoteMessage(curChannel).queue(
                            message -> {
                                mrl.retrieveQdUsers(qdUsers);
                                mrl.retrieveLobbySize(qdUsers.getLobbySize());
                            }
                    );
                    game.startSeries(qdUsers.getSeriesLength(), qdUsers);
                    game.chooseMafia();
                } else {
                    field = "Lobby size is now " + qdUsers.getLobbySize() + "!";
//                    curChannel.sendMessage("Lobby size is now " + qdUsers.getLobbySize() + "!  The mafia will be chosen once " + qdUsers.getFreeSpaces() + " more people have queued.").queue();
                    emessage = "The mafia will be chosen once " + qdUsers.getFreeSpaces() + " more people have queued.";
                    eb.addField(field, emessage, false);
                    curChannel.sendMessage(eb.build()).queue(
                            message -> {
                                mrl.retrieveLobbySize(qdUsers.getLobbySize());
                            }
                    );
                }
            } catch (NumberFormatException e) {
                curChannel.sendMessage("Uh oh!  There's something wrong with this command!").queue();
//                e.printStackTrace();
            } catch (LobbyTooBigException e) {
                curChannel.sendMessage(e.getMessage()).queue();
            } catch (LobbyTooSmallException e) {
                curChannel.sendMessage(e.getMessage()).queue();
            }
        }
        // !size
        else if (inMessage.getContentRaw().equals("!size")) {
//            curChannel.sendMessage("Lobby size is " + qdUsers.getLobbySize()).queue();
            eb.addField("Lobby size", "" + qdUsers.getLobbySize(), false);
            curChannel.sendMessage(eb.build()).queue();
//            curChannel.sendMessage(eb.build()).queue();
        }
        // !clear
        else if (inMessage.getContentRaw().equals("!clear")) {
            qdUsers.clear();
            curChannel.sendMessage("The queue is now empty!").queue();
        }
        // !reset
        else if (inMessage.getContentRaw().equals("!reset")) {
            game.endGame();
            game.endSeries();
            qdUsers.clear();
            curChannel.sendMessage("Series reset!  All points discarded!").queue();
        }
        // !series
        else if (inMessage.getContentRaw().equals("!series")) {
            curChannel.sendMessage("Series length is a best of " + qdUsers.getSeriesLength()).queue();
        }
        // !setseries [#]
        else if (inMessage.getContentRaw().startsWith("!setseries")) {
            if (game.getGameStatus() == game.IN_QUEUE) {
                try {
                    int length = Integer.parseInt(inMessage.getContentRaw().split(" ")[1]);
                    if (length > 1) {
                        qdUsers.setSeriesLength(length);
                        curChannel.sendMessage("Series length is now " + qdUsers.getSeriesLength()).queue();
                    } else {
                        curChannel.sendMessage("Series length must be larger than 1").queue();
                    }
                } catch (NumberFormatException e) {
                    curChannel.sendMessage("Uh oh!  There's something wrong with this command!").queue();
                }
            } else {
                curChannel.sendMessage("You must finish the series before changing the series length!").queue();
            }
        }
        else if (inMessage.getContentRaw().equals("!serverlb")) {
            curChannel.sendMessage("There are no stats for this server yet!  This is also a hard-coded response!").queue();
        } else if (inMessage.getContentRaw().equals("!lb")) {
            curChannel.sendMessage("There are no stats anywhere yet!  This is also a hard-coded response!").queue();
        } else if (inMessage.getContentRaw().equals("!stats")) {
            curChannel.sendMessage("You have no stats on record yet!  This is also a hard-coded response!").queue();
        } else if (inMessage.getContentRaw().equals("!rules")) {
            curChannel.sendMessage("Mafia points (+3 possible): \n" +
                    "\t-1 for winning the game\n" +
                    "\t+1 for losing the game \n" +
                    "\t+2 for not getting voted by majority \n" +
                    "\n" +
                    "Town points (+3 possible): \n" +
                    "\t+1 point for winning the game \n" +
                    "\t+1 for winning on Mafia's team\n" +
                    "\t+1 point for correctly voting the mafia member").queue();
        }
        // no base case
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        guild = event.getGuild();
        String serverID = createServerID(guild.getName(), guild.getOwner().getUser().getAsTag());
//        guild.getOwner().getUser().openPrivateChannel().queue((channel) ->
//                channel.sendMessage("This is RL Mafia Bot!\nYou are " + guild.getOwner().getUser().getAsTag() +
//                        "\nYour server is " + guild.getName() +
//                        "\nYour unique server ID is " + createServerID(guild.getName(), guild.getOwner().getUser().getAsTag())).queue());
        ApiFuture future = firedb.collection("Servers").add(new ServerCollection(serverID));
    }

    private String createServerID(String guildName, String ownerTag) {
        // first 10 characers of server name + owner tag
        String decodedID;
        if (guildName.replaceAll("\\s", "").length() > 10) decodedID = guildName.substring(0, 10) + ownerTag;
        else decodedID = guildName.replaceAll("\\s", "") + ownerTag;
        System.out.println(decodedID);
        String encodedID = Hex.encodeHexString(decodedID.getBytes());
        System.out.println(encodedID);
        return encodedID;
    }

    private MessageAction sendVoteMessage(MessageChannel curChannel) {
        String field = "";
        ArrayList<Player>[] teams = game.chooseTeams(game.RANDOM_TEAMS);

        // string for team 1
        String team1 = "";
        int voteIndex = 0;
        for (int i = 0; i < teams[0].size(); i++) {
            team1 += voteList[voteIndex] + ": " + teams[0].get(i).getUser().getName() + "\n";
            voteIndex++;
        }

        field = "**Team 1:**";
        eb.addField(field, team1, false);

        // string for team 2
        String team2 = "";
        for (int i = 0; i < teams[1].size(); i++) {
            team2 += voteList[voteIndex] + ": " + teams[1].get(i).getUser().getName() + "\n";
            voteIndex++;
        }

        field = "**Team2:**";
        eb.addField(field, team2, false);
        eb.addField("", "React with respective emotes to vote and with :white_check_mark: or :negative_squared_cross_mark: if you won or lost.", false);
        return curChannel.sendMessage(eb.build());
    }

    private void initFirebase(String projectId) throws IOException {
        credentials = GoogleCredentials.getApplicationDefault();
        options = new FirebaseOptions.Builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();
        FirebaseApp.initializeApp(options);
        firedb = FirestoreClient.getFirestore();
    }
}
