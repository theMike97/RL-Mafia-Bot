import net.dv8tion.jda.core.entities.User;

public class Player {

    private User user;
    private boolean isMafia;
    private Player guess;
    private int points;

    public Player(User user) {
        this.user = user;
        isMafia = false;
        guess = null;
        points = 0;
    }

    public User getUser() {
        return user;
    }

    public void setMafia(boolean mafia) {
        isMafia = mafia;
    }

    public boolean isMafia() {
        return isMafia;
    }

    public void setGuess(Player player) {
        guess = player;
    }

    public Player getGuess() {
        return guess;
    }

    public boolean hasGuessed() {
        return guess == null;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null)
            if (o instanceof User) return user.equals(o);
        return false;
    }
}
