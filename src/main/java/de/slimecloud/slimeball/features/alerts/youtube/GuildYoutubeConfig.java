package de.slimecloud.slimeball.features.alerts.youtube;

import de.slimecloud.slimeball.config.ConfigCategory;
import de.slimecloud.slimeball.config.engine.ConfigField;
import de.slimecloud.slimeball.config.engine.ConfigFieldType;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

@Getter
public class GuildYoutubeConfig extends ConfigCategory {
	@ConfigField(name = "Kanal", command = "channel", description = "Der Text Kanal in den neue Videos gesendet werden", type = ConfigFieldType.MESSAGE_CHANNEL)
	private Long channel;

	@ConfigField(name = "Rolle", command = "role", description = "Die Rolle, die bei neuen Videos gepingt wird", type = ConfigFieldType.ROLE)
	private Long role;

	@ConfigField(name = "Live Nachricht", command = "live-msg", description = "Die Nachricht, die bei livestreams gesendet wird", type = ConfigFieldType.STRING)
	private String liveMessage;

	@ConfigField(name = "Video Nachricht", command = "video-msg", description = "Die Nachricht, die bei neuen Videos gesendet wird", type = ConfigFieldType.STRING)
	private String videoMessage;

	public TextChannel getChannel() {
		return bot.getJda().getTextChannelById(channel);
	}

	public Role getRole() {
		return bot.getJda().getRoleById(role);
	}
}