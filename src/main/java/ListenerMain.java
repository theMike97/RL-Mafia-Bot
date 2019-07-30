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
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
//        initFirebase("rl-mafia-bot");
//        serverID = "";
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel curChannel = event.getChannel();
        Message inMessage = event.getMessage();
        Player inAuthor = new Player(event.getAuthor());

        if (inAuthor.getUser().isBot()) return;


        if (inMessage.getContentRaw().equals("!help")) {
            curChannel.sendMessage("Here is a list of available commands:\n" +
                    "\t!q - this adds you to the mafia queue.\n" +
                    "\t!r - this removes you from the mafia queue.\n" +
                    "\t!getq - this shows the users currently in the queue.\n" +
                    "\t!setsize [#] - this sets lobby capacity (set to 6 by default).\n" +
                    "\t!size - this shows lobby size.\n" +
                    "\t!series - this shows the current series length.*\n" +
                    "\t!setseries [#] - this sets the series length.*\n" +
                    "\t!clear - this clears the queue.\n" +
                    "\t!reset - this clears the queue and stops any active series.\n" +
                    "\t~~!g [@user] - this logs your guess for who the mafia is.*~~\n" +
                    "\t~~!reveal - this reveals the mafia and totals points for the game/series.*~~\n" +
                    "\t!serverlb - this shows the top 10 mafia players by series won in the server.*\n" +
                    "\t!lb - this shows the top 20 mafia players by series won worldwide.*\n" +
                    "\t!stats - this shows your personal stats.*\n" +
                    "\t!rules - this shows a list of the rules for earning points and winning games.*\n" +
                    "\t!help - I wonder how you got here????").queue();
//            curChannel.sendMessage(":" + voteList[2] + ":").queue();
        }
        // !q
        else if (inMessage.getContentRaw().equals("!q")) {
            if (qdUsers.contains(inAuthor)) {
                curChannel.sendMessage("You are already in the queue!").queue();
                return;
            }
            if (qdUsers.size() > qdUsers.getLobbySize() - 1) {
                curChannel.sendMessage("Lobby is full!").queue();
                return;
            }
            qdUsers.add(inAuthor);
            curChannel.sendMessage("Added " + inAuthor.getUser().getName()).queue();
            if (qdUsers.size() == qdUsers.getLobbySize()) {
                qdUsers.setPartyFull(true);
//                curChannel.sendMessage("Queue is now full!  Choosing mafia...").queue();
                curChannel.sendMessage("Queue is now full!  Randomly assigning teams.\n").queue();
                if (qdUsers.getLobbySize() > 1) { // testing only
                    ArrayList<Player>[] teams = game.chooseTeams(game.RANDOM_TEAMS);

                    // string for team 1
                    String team1 = "";
                    int voteIndex = 0;
                    for (int i = 0; i < teams[0].size(); i++) {
                        team1 += voteList[voteIndex] + " " + teams[0].get(i).getUser().getAsTag() + "\n";
                        voteIndex++;
                    }
//                    team1.substring(2);
                    curChannel.sendMessage("**Team 1:**\n" + team1).queue();

                    // string for team 2
                    String team2 = "";
                    for (int i = 0; i < teams[1].size(); i++) {
                        team2 += voteList[voteIndex] + " " + teams[1].get(i).getUser().getAsTag() + "\n";
                        voteIndex++;
                    }
//                    team2.substring(2);
                    curChannel.sendMessage("**Team 2:**\n" + team2).queue();
                    curChannel.sendMessage("React to vote.").queue(
                            message -> {
                                mrl.retrieveQdUsers(game.sortQueueByTeam());
                            }
                    );

                    game.startSeries(qdUsers.getSeriesLength(), qdUsers);
                }
                game.chooseMafia();

                //voting reactions


                return;
            }

            curChannel.sendMessage("Queue now contains: " + qdUsers + "\n" +
                    "Waiting on " + qdUsers.getFreeSpaces() + " more " + ((qdUsers.getFreeSpaces() == 1) ? "person." : "people.")).queue();
        }
        // !r
        else if (inMessage.getContentRaw().equals("!r")) {
            if (!qdUsers.contains(inAuthor)) {
                curChannel.sendMessage("You are not in the queue!").queue();
                return;
            }
            if (game.getGameStatus() != game.IN_QUEUE) {
                curChannel.sendMessage("Cannot leave mid-series").queue();
                return;
            }

            boolean isRemoved = qdUsers.remove(inAuthor);
            if (isRemoved)
                curChannel.sendMessage("Removed " + inAuthor.getUser().getAsTag()).queue();
            else
                curChannel.sendMessage("Error removing " + inAuthor.getUser().getAsTag() + " from the queue D:").queue();

            curChannel.sendMessage("The queue is currently " + (qdUsers.isEmpty() ? "Empty" : qdUsers)).queue();
            if (!qdUsers.isEmpty())
                curChannel.sendMessage("Waiting on " + qdUsers.getFreeSpaces() + " more people.").queue();
        }
        //!getq
        else if (inMessage.getContentRaw().equals("!getq")) {
            curChannel.sendMessage("The queue is currently " + (qdUsers.isEmpty() ? "Empty" : qdUsers)).queue();
        }
        //!setsize [#]
        else if (inMessage.getContentRaw().contains("!setsize")) {
            try {
                qdUsers.setLobbySize(Integer.parseInt(inMessage.getContentRaw().split(" ")[1]));
                qdUsers.setPartyFull(qdUsers.getLobbySize() == qdUsers.size());
                if (qdUsers.isPartyFull()) {
                    curChannel.sendMessage("Lobby size is now " + qdUsers.getLobbySize() + "!\n" +
                            "Choosing mafia...").queue();
                    game.startSeries(qdUsers.getSeriesLength(), qdUsers);
                    game.chooseMafia();
                } else {
                    curChannel.sendMessage("Lobby size is now " + qdUsers.getLobbySize() + "!  The mafia will be chosen once " + qdUsers.getFreeSpaces() + " more people have queued.").queue(
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
            curChannel.sendMessage("Lobby size is " + qdUsers.getLobbySize()).queue();
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
        // !g [@user] - deprecated
//        else if (inMessage.getContentRaw().startsWith("!g")) {
//            if (game.getGameStatus() == game.IN_QUEUE) {
//                curChannel.sendMessage("Cannot guess until the mafia is chosen!");
//                return;
//            }
//            if (qdUsers.containsUser(inAuthor.getUser()) < 1) {
//                curChannel.sendMessage("Must be in the queue to guess a player!").queue();
//                return;
//            }
//            List<User> playersList = inMessage.getMentionedUsers();
//            if (playersList.size() > 1) {
//                curChannel.sendMessage("Guess only one person!");
//                return;
//            }
//            if (qdUsers.containsUser(playersList.get(0)) < 1) {
//                curChannel.sendMessage("Must send message to person in the queue.").queue();
//                return;
//            }
//            Player guessedPlayer = user2Player(playersList.get(0));
//            inAuthor.setGuess(guessedPlayer);
//            curChannel.sendMessage(inAuthor.getUser().getAsTag() + " guessed " + inAuthor.getGuess().getUser().getAsTag() + " as mafia!").queue();
//        }
        // !reveal - deprecated
//        else if (inMessage.getContentRaw().equals("!reveal")) {
//            for (Player player : qdUsers) {
//                if (player.isMafia())
//                    curChannel.sendMessage(player.getUser().getAsTag() + " was the Mafia!\n").queue();
//            }
//            game.calculatePoints();
//            curChannel.sendMessage("Points updated!").queue();
//        }
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

    @Nullable
    private Player user2Player(User user) {
        for (Player player : qdUsers) {
            if (player.getUser().equals(user))
                return player;
        }
        return null;
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
