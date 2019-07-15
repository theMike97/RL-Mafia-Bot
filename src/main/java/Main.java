import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.AccountType;

import javax.security.auth.login.LoginException;

public class Main {

    public static void main(String[] args) throws LoginException {
        JDABuilder builder = new JDABuilder(AccountType.BOT);
        String token = "NTk5ODUzMzM3MzY1NzA4ODIx.XSwfCA.Iu9NPOkkbH94XJKoyofxiTbQKD8";
        builder.setToken(token);
        builder.addEventListener(new ListenerMain());
        builder.build();
    }

}
