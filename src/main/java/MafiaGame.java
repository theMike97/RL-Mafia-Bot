import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MafiaGame {

    private UsersQueue q;
    private Random rand;
    private int gameStatus;
    private ArrayList<Player> draft;
    private ArrayList<Player> team1;
    private ArrayList<Player> team2;

    public static int IN_QUEUE = 0;
    public static int IN_GAME = 1;
    public static int BETWEEN_GAMES = 2;
    public static int CALCULATING_POINTS = 3;

    public static int RANDOM_TEAMS = 0;
    public static int CAPTAIN_TEAMS = 1;

    public MafiaGame(UsersQueue q) {
        this.q = q;
        draft = new ArrayList<>();
        team1 = new ArrayList<>();
        team2 = new ArrayList<>();
        rand = new Random();
    }

    public void startSeries(int length, UsersQueue q){
        startGame();
    }

    public void startGame() {
        gameStatus = IN_GAME;
        System.out.println(gameStatus);
    }

    public void endGame() {
        gameStatus = BETWEEN_GAMES;
        System.out.println(gameStatus);
    }

    public void endSeries() {
        gameStatus = IN_QUEUE;
        System.out.println(gameStatus);
    }

    public int getGameStatus() {
        return gameStatus;
    }

    public ArrayList<Player>[] chooseTeams(int random) {
        for(Player player : q)
            draft.add(player);

        if (random == RANDOM_TEAMS) {
            int index = 0;
            int playerIndex;
            while (draft.size() > 0) {
                playerIndex = rand.nextInt(draft.size());
                team1.add(draft.get(playerIndex));
                System.out.println(team1.get(index).getUserInfo().getAsTag());
                draft.remove(playerIndex);

                playerIndex = rand.nextInt(draft.size());
                team2.add(draft.get(playerIndex));
                System.out.println(team2.get(index).getUserInfo().getAsTag());
                draft.remove(playerIndex);

                index++;
            }
        }
        ArrayList<Player>[] teams = new ArrayList[2];
        teams[0] = team1;
        teams[1] = team2;
        System.out.println(teams[0] + ", " + teams[1]);
        return teams;
    }

    public void chooseMafia() {
        if (q != null) {
            int index = rand.nextInt(q.getLobbySize());
            for (int i = 0; i < q.getLobbySize(); i++) {
                if (i != index) q.get(i).setMafia(false);
                else q.get(i).setMafia(true);
            }
            System.out.println("Index was: " + index);
            System.out.println("Mafia is: " + q.get(index).getUserInfo().getAsTag());
        } else {
            throw new NullPointerException("Queue is null!");
        }
    }

    public void calculatePoints() {
        gameStatus = CALCULATING_POINTS;
        System.out.println(gameStatus);
        // do math here
        // updated series points:
        gameStatus = BETWEEN_GAMES;
//        chooseMafia();
//        gameStatus = IN_GAME;

    }
}
