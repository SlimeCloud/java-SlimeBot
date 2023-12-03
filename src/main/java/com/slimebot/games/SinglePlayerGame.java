package com.slimebot.games;

public abstract class SinglePlayerGame<T extends GamePlayer<G, T>, G extends SinglePlayerGame<T, G>> extends Game<T, G> {
	public final T player;

	@SuppressWarnings("unchecked")
	protected SinglePlayerGame(long guild, long channel, T player) {
		super((Class<? extends T>) player.getClass(), guild, channel);
		this.player = player;

		start();
	}

	@Override
	public void end() {
		super.end();
		player.kill();
	}
}
