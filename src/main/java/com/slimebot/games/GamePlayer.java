package com.slimebot.games;

import net.dv8tion.jda.api.entities.UserSnowflake;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class GamePlayer<G extends Game<T, G>, T extends GamePlayer<G, T>> {
	public final static List<GamePlayer<?, ?>> players = new ArrayList<>();

	public final long id;
	public final G game;

	protected GamePlayer(long id,G game) {
		this.id = id;
		this.game = game;

		players.add(this);
	}

	public UserSnowflake getUser() {
		return UserSnowflake.fromId(id);
	}

	public String getAsMention() {
		return "<@" + id + ">";
	}

	/**
	 * Removes the player from the game
	 */
	public void kill() {
		if (game instanceof MultiPlayerGame<?, ?> mg) mg.players.remove(this);
		players.remove(this);
	}

	public static <T extends GamePlayer<?, ?>> Optional<T> getFromId(Class<T> playerClass, long id) {
		return players.stream()
				.filter(p -> p.id == id)
				.map(playerClass::cast)
				.findAny();
	}

	public static boolean isInGame(long id) {
		return getFromId(GamePlayer.class, id).isPresent();
	}
}
