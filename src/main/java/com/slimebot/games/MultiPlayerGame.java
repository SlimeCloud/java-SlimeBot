package com.slimebot.games;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class MultiPlayerGame<T extends GamePlayer<G, T>, G extends MultiPlayerGame<T, G>> extends Game<T, G> {
	public final long gameMaster;
	public final boolean playersCanJoin;

	public final short minPlayers;
	public final short maxPlayers;

	private final BiFunction<MultiPlayerGame<T, G>, Long, T> gamePlayerFunction;
	public final List<T> players = new ArrayList<>();

	/**
	 * @param gameMaster         memberId of the gameMaster
	 * @param guild            id of the guild
	 * @param channel          id of the thread Channel
	 * @param playersCanJoin     disables or enables the join button
	 * @param gamePlayerFunction BiFunction that calls the constructor of your player class
	 */
	protected MultiPlayerGame(Class<T> type, BiFunction<MultiPlayerGame<T, G>, Long, T> gamePlayerFunction, long guild, long channel, long gameMaster, boolean playersCanJoin, short maxPlayers, short minPlayers) {
		super(type, guild, channel);

		this.gameMaster = gameMaster;
		this.playersCanJoin = playersCanJoin;
		this.gamePlayerFunction = gamePlayerFunction;

		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;

		players.add(this.gamePlayerFunction.apply(this, gameMaster));
	}

	public void sendJoinMessage() {
		getChannel().sendMessage(buildJoinMessage()).queue();
	}

	private MessageCreateData buildJoinMessage() {
		return new MessageCreateBuilder()
				.setEmbeds(buildJoinEmbed()
						.addField(
								":alarm_clock: Automatisch Löschen",
								TimeFormat.RELATIVE.after(15 * 60 * 1000).toString(),
								true
						)
						.build()
				)
				.setActionRow(
						Button.primary(uuid + ":join", "Join").withDisabled(!playersCanJoin || players.size() > maxPlayers),
						Button.danger(uuid + ":leave", "Leave"),
						Button.primary(uuid + ":start", "Start")
				).build();
	}

	public Optional<T> getPlayerFromId(long id) {
		return players.stream()
				.filter(p -> p.id == id)
				.findAny();
	}

	public boolean join(long id) {
		if (status != GameStatus.WAITING) return false;
		if (GamePlayer.isInGame(id)) return false;

		this.players.add(gamePlayerFunction.apply(this, id));
		return true;
	}

	@Override
	public void end() {
		super.end();
		players.forEach(GamePlayer::kill);
	}

	protected abstract EmbedBuilder buildJoinEmbed();

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String[] id = event.getComponentId().split(":");

		if (!id[0].equals(uuid.toString())) return;

		switch (id[1]) {
			case "join" -> {
				if (players.size() >= maxPlayers) {
					event.reply(":x: Das Spiel ist bereits voll!").setEphemeral(true).queue();
					return;
				}

				if (!join(event.getUser().getIdLong())) {
					event.reply(":x: Du bist schon in einem Game!").setEphemeral(true).queue();
					return;
				}

				event.editMessageEmbeds(buildJoinMessage().getEmbeds()).queue();

				getChannel().sendMessage(event.getUser().getAsMention() + " ist dem Spiel beigetreten!").queue();
			}

			case "leave" -> {
				if (getPlayerFromId(event.getUser().getIdLong()).isEmpty()) {
					event.reply(":x: Du bist nicht im Game!").setEphemeral(true).queue();
					return;
				}

				if (event.getUser().getIdLong() == gameMaster) {
					event.reply(":x: Du bist Spielleiter:in du kannst nicht verlassen!").setEphemeral(true).queue();
					return;
				}

				getPlayerFromId(event.getUser().getIdLong()).ifPresent(GamePlayer::kill);

				event.editMessageEmbeds(buildJoinMessage().getEmbeds()).queue();
				event.reply(":x: " + event.getUser().getAsMention() + " hat das Spiel verlassen!").queue();
			}
		}

		if (event.getUser().getIdLong() != gameMaster) {
			event.reply(":x: Du bist nicht Spielleiter:in!").queue();
			return;
		}

		if (id[1].equals("start")) {
			if (players.size() < minPlayers) {
				event.reply(":x: Es müssen mindestens " + minPlayers + " Spieler:innen mit machen!").setEphemeral(true).queue();
				return;
			}

			event.editComponents(event.getMessage().getComponents().stream().map(LayoutComponent::asDisabled).toList()).queue();
			start();
		}
	}
}
