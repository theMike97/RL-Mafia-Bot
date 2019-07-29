import java.util.ArrayList;


public class ServerCollection extends ArrayList<Player> {

    private String serverID;

    public ServerCollection() {
        // need public empty constructor
        // cant really think of anything else to put here
    }

    public ServerCollection(String serverID) {
        this();
        this.serverID = serverID;
    }
}
