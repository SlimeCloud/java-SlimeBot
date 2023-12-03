package com.slimebot.games.wordchain;

import com.slimebot.games.GamePlayer;

import java.util.function.Consumer;

public class WordchainPlayer extends GamePlayer<WordchainGame, WordchainPlayer> {
	public short lives;

	public WordchainPlayer(long id, short lives, WordchainGame game) {
		super(id, game);
		this.lives = lives;
	}

	public void damage(Consumer<Short> messageConsumer) {
		lives--;

		if (lives <= 0) {
			this.game.getChannel().sendMessage(":x: " + getAsMention() + " ist Ausgeschieden!").queue();
			kill();

			this.game.nextTurn();
			return;
		}

		messageConsumer.accept(lives);

		this.game.nextTurn();
	}
}
