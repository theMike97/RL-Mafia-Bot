import java.util.Random;

public class MafiaGame {

    private UsersQueue q;
    private Random rand;
    private int gameStatus;

    public static int IN_QUEUE = 0;
    public static int IN_GAME = 1;
    public static int BETWEEN_GAMES = 2;
    public static int CALCULATING_POINTS = 3;

    public MafiaGame() {
        q = null;
        rand = new Random();
    }

    public void startSeries(int length, UsersQueue q){
        startGame(q);
    }

    public void startGame(UsersQueue q) {
        this.q = q;
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
    }
}
