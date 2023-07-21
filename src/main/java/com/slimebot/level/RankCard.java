package com.slimebot.level;

import com.slimebot.graphic.CustomFont;
import com.slimebot.graphic.Graphic;
import com.slimebot.graphic.ImageUtil;
import com.slimebot.main.Main;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class RankCard extends Graphic {

    private final User user;
    private final int level;
    private final double xp;
    private final Font font;

    public RankCard(Level level) {
        super(1200, 200);
        this.user = Main.jdaInstance.getUserById(level.userId());
        this.level = level.level();
        this.xp = level.xp();
        Font font_;
        try {
            font_ = CustomFont.getFont("Ubuntu", Font.BOLD, 40);
        } catch(IOException | FontFormatException e) {
            e.printStackTrace();
            font_ = new Font("Arial", Font.BOLD, 40);
        }
        this.font = font_;
        constructorEnd();
    }

    @Override
    public void drawGraphic(Graphics2D graphics2D) throws Exception {
        int avatarWidth = height - 40;
        BufferedImage avatar = ImageIO.read(new URL(user.getAvatarUrl()));
        avatar = ImageUtil.resize(avatar, avatarWidth, avatarWidth);
        avatar = ImageUtil.circle(avatar);
        graphics2D.drawImage(avatar, 0, 40, null);

        double xpRequired = Level.calculateRequiredXP(level + 1);
        double percentage = xp / xpRequired;
        int maxBarSize = width - 80 - avatarWidth;
        int barSize = (int) (maxBarSize * percentage);

        graphics2D.setColor(new Color(66, 155, 46, 200));
        graphics2D.drawRoundRect(avatarWidth + 40, height - 60, maxBarSize, height - 180, height - 180, height - 180);
        graphics2D.setColor(new Color(105, 227, 73, 200));
        graphics2D.fillRoundRect(avatarWidth + 40, height - 60, barSize, height - 180, height - 180, height - 180);

        graphics2D.setColor(Color.WHITE);
        graphics2D.setFont(new Font("Arial", Font.BOLD, 52));
        graphics2D.drawString(user.getName(), avatarWidth + 50, height - 80);

        graphics2D.setFont(font);
        String s = xp + "/" + xpRequired + " xp";
        graphics2D.drawString(s, width - graphics2D.getFontMetrics().stringWidth(s) - 40, height - 80);
        s = "Level: " + level;
        graphics2D.drawString(s, width - graphics2D.getFontMetrics().stringWidth(s) - 40, height - 160);
    }
}