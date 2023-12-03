package com.slimebot.games;

import com.slimebot.main.Main;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


public abstract class Game<T extends GamePlayer<? extends G, ? extends T>, G extends Game<? extends T, G>> extends ListenerAdapter {
	public final UUID uuid = UUID.randomUUID();
	public final Class<? extends T> playerClass;

	public final long channel;
	public final long guild;

	public GameStatus status;

	/**
	 * @param guild id of the guild
	 */
	protected Game(Class<? extends T> playerClass, long guild, long channel) {
		this.playerClass = playerClass;

		this.guild = guild;
		this.channel = channel;

		status = GameStatus.WAITING;
		Main.jdaInstance.addEventListener(this);

		// End the game if its 15min WAITING
		Main.executor.schedule(() -> {
			if (status == GameStatus.WAITING) end();
		}, 15, TimeUnit.MINUTES);
	}

	public void start() {
		status = GameStatus.PLAYING;
	}

	/**
	 * Important to call in the end of the game!
	 * Kills all players and removes eventListener
	 */
	public void end() {
		status = GameStatus.ENDED;
		Main.jdaInstance.removeEventListener(this);

		if (getChannel() != null && getChannel() instanceof ThreadChannel tc) tc.delete().queueAfter(3, TimeUnit.MINUTES);
	}

	public MessageChannel getChannel() {
		return Main.jdaInstance.getChannelById(MessageChannel.class, channel);
	}

	protected abstract void handleMessageEvent(T player, MessageReceivedEvent event);

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (status != GameStatus.PLAYING) return;
		if (event.getChannel().getIdLong() != channel) return;

		GamePlayer.getFromId(playerClass, event.getAuthor().getIdLong()).ifPresentOrElse(
				p -> {
					if (p.game == this) handleMessageEvent(p, event);
					else event.getMessage().delete().queue();
				},
				() -> event.getMessage().delete().queue()
		);
	}

	public enum GameStatus {
		WAITING,
		PLAYING,
		ENDED
	}
}