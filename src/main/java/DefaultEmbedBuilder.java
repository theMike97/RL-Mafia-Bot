import net.dv8tion.jda.core.EmbedBuilder;

import java.awt.*;

public class DefaultEmbedBuilder extends EmbedBuilder {

    private Color defaultColor;

    public DefaultEmbedBuilder() {
        super();
        defaultColor = new Color(228,180,0);
        init();
    }

    @Override
    public EmbedBuilder clear() {
        super.clear();
        init();

        return this;
    }

    private void init() {
        setColor(defaultColor);
        setFooter("\nBot created by theMike97_", null);
    }


}
