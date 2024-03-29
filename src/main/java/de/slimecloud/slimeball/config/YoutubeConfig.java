package de.slimecloud.slimeball.config;

import de.slimecloud.slimeball.config.engine.Required;
import lombok.Getter;

@Getter
public class YoutubeConfig {
	@Required
	private String youtubeChannelId;

	@Required
	private int updateRate; // seconds
}
