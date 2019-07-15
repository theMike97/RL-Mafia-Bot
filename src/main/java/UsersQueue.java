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

    @Override
    public String toString() {
        String list = "";
        for (Player player : this)
            list += ", " + player.getUserInfo().getAsTag();

        list = list.substring(2);

        return list;
    }
}
