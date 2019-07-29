import exceptions.LobbyTooBigException;
import exceptions.LobbyTooSmallException;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;


public class UsersQueue extends ArrayList<Player> {

    private int lobbySize;
    private boolean isPartyFull;
    private int seriesLength;

    public UsersQueue(int lobbySize, int seriesLength) {
        this.lobbySize = lobbySize;
        this.seriesLength = seriesLength;
        isPartyFull = false;
    }

    public void setLobbySize(int lobbySize) throws LobbyTooBigException, LobbyTooSmallException {
        if (lobbySize > 8) throw new LobbyTooBigException("Lobby cannot be more than 8 people.");
        if (lobbySize < 2) throw new LobbyTooSmallException("Lobby must be at least 2 people.");
        this.lobbySize = lobbySize;
    }

    public int getLobbySize() {
        return lobbySize;
    }

    public int getFreeSpaces() {
        return lobbySize - size();
    }

    public void setPartyFull(boolean isPartyFull) {
        this.isPartyFull = isPartyFull;
    }

    public boolean isPartyFull() {
        return isPartyFull;
    }

    public void setSeriesLength(int seriesLength) {
        this.seriesLength = seriesLength;
    }

    public int getSeriesLength() {
        return seriesLength;
    }

    private boolean containsUser(User user) {

        for (Player player : this) {
//                System.out.println(player.getUserInfo());
            if (player.getUser().equals(user)) return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o != null) {
            int pos = indexOf(o);
            if (pos != -1)
                return super.remove(pos) != null;
        }
        return false;
    }

    @Override
    public int indexOf(Object o) {
        if (o != null) {
            User user;
            if (o instanceof Player) user = ((Player) o).getUser();
            else if (o instanceof User) user = (User) o;
            else return -1;

            for (int i = 0; i < size(); i++)
                if (get(i).getUser().equals(user)) return i;
        }
        return -1;
    }

    @Override
    public boolean contains(Object o) {
        if (o != null) {
            if (o instanceof Player) return containsUser(((Player) o).getUser());
            if (o instanceof User) return containsUser((User) o);
        }
        return false;
    }

    @Override
    public String toString() {
        String list = "";
        for (Player player : this)
            list += ", " + player.getUser().getAsTag();

        list = list.substring(2);

        return list;
    }
}
