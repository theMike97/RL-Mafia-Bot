import exceptions.LobbyTooBigException;
import exceptions.LobbyTooSmallException;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;

public class ListenerMain extends ListenerAdapter {

    private UsersQueue qdUsers;
    private MafiaGame game;

    public ListenerMain() {
        super();
        qdUsers = new UsersQueue(6, 5);
        game = new MafiaGame();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel curChannel = event.getChannel();
        Message inMessage = event.getMessage();
        Player inAuthor = new Player(event.getAuthor());

        if (inAuthor.getUserInfo().isBot()) return;

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
                    "\t!g [@user] - this logs your guess for who the mafia is.*\n" +
                    "\t!reveal - this reveals the mafia and totals points for the game/series.*\n" +
                    "\t!serverlb - this shows the top 10 mafia players by series won in the server.*\n" +
                    "\t!lb - this shows the top 20 mafia players by series won worldwide.*\n" +
                    "\t!stats - this shows your personal stats.*\n" +
                    "\t!rules - this shows a list of the rules for earning points and winning games.*\n" +
                    "\t!help - I wonder how you got here????").queue();
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
            curChannel.sendMessage("Added " + inAuthor.getUserInfo().getName()).queue();
            if (qdUsers.size() == qdUsers.getLobbySize()) {
                qdUsers.setPartyFull(true);
                curChannel.sendMessage("Queue is now full!  Choosing mafia...").queue();
                game.startSeries(qdUsers.getSeriesLength(), qdUsers);
                game.chooseMafia();
                return;
            }

            curChannel.sendMessage("Queue now contains: " + qdUsers + "\n" +
                    "Waiting on " + qdUsers.getFreeSpaces() + " more people.").queue();
        }
        // !r
        else if (inMessage.getContentRaw().equals("!r")) {
            if (!qdUsers.contains(inAuthor)) {
                curChannel.sendMessage("You are not in the queue!").queue();
                return;
            } else if (game.getGameStatus() != game.IN_QUEUE) {
                curChannel.sendMessage("Cannot leave mid-series").queue();
            }
            qdUsers.remove(inAuthor);
            curChannel.sendMessage("Removed " + inAuthor.getUserInfo().getName()).queue();

            curChannel.sendMessage("The queue is currently " + (qdUsers.isEmpty() ? "Empty" : qdUsers)).queue();
            if (!qdUsers.isEmpty())
                curChannel.sendMessage("Waiting on " + qdUsers.getFreeSpaces() + " more people.").queue();
        }
        //!getq
        else if (inMessage.getContentRaw().equals("!getq")) {
            curChannel.sendMessage("The queue is currently " + (qdUsers.isEmpty() ? "Empty" : qdUsers)).queue();
        }
        //!setsize
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
                    curChannel.sendMessage("Lobby size is now " + qdUsers.getLobbySize() + "!  The mafia will be chosen once " + qdUsers.getFreeSpaces() + " more people have queued.").queue();
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
            curChannel.sendMessage("Lobby size is "  + qdUsers.getLobbySize()).queue();
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
        // !g [@user]
        else if (inMessage.getContentRaw().startsWith("!g")) {
            if (game.getGameStatus() == game.IN_QUEUE) {
                curChannel.sendMessage("Cannot guess until the mafia is chosen!");
                return;
            }
            if (qdUsers.containsUser(inAuthor.getUserInfo()) < 1) {
                curChannel.sendMessage("Must be in the queue to guess a player!").queue();
                return;
            }
            List<User> playersList = inMessage.getMentionedUsers();
            if (playersList.size() > 1) {
                curChannel.sendMessage("Guess only one person!");
                return;
            }
            if (qdUsers.containsUser(playersList.get(0)) < 1) {
                curChannel.sendMessage("Must send message to person in the queue.").queue();
                return;
            }
            Player guessedPlayer = user2Player(playersList.get(0));
            inAuthor.setGuess(guessedPlayer);
            curChannel.sendMessage(inAuthor.getUserInfo().getAsTag() + " guessed " + inAuthor.getGuess().getUserInfo().getAsTag() + " as mafia!").queue();
        }
        else if (inMessage.getContentRaw().equals("!reveal")) {
            for (Player player : qdUsers) {
                if (player.isMafia())
                    curChannel.sendMessage(player.getUserInfo().getAsTag() + " was the Mafia!\n").queue();
            }
            game.calculatePoints();
            curChannel.sendMessage("Points updated!").queue();
        }
        else if (inMessage.getContentRaw().equals("!serverlb")) {
            curChannel.sendMessage("There are no stats for this server yet!  This is also a hard-coded response!").queue();
        }
        else if (inMessage.getContentRaw().equals("!lb")) {
            curChannel.sendMessage("There are no stats anywhere yet!  This is also a hard-coded response!").queue();
        }
        else if (inMessage.getContentRaw().equals("!stats")) {
            curChannel.sendMessage("You have no stats on record yet!  This is also a hard-coded response!").queue();
        }
        else if (inMessage.getContentRaw().equals("!rules")) {
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

    private Player user2Player(User user) {
        for (Player player : qdUsers) {
            if (player.getUserInfo().equals(user))
                return player;
        }
        return null;
    }
}
