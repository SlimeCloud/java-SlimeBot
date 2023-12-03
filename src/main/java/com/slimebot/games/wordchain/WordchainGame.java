package com.slimebot.games.wordchain;

import com.slimebot.games.GamePlayer;
import com.slimebot.games.MultiPlayerGame;
import com.slimebot.main.Main;
import com.slimebot.main.config.guild.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WordchainGame extends MultiPlayerGame<WordchainPlayer, WordchainGame> {
	private String lastWord;
	private final List<String> words = new ArrayList<>();

	private int playerTurn;
	private int round;
	private ScheduledFuture<?> scheduledFuture;
	private final short seconds;
	private final short lives;

	public WordchainGame(long guild, long channel, long gameMaster, short seconds, short lives) {
		super(WordchainPlayer.class, (game, id) -> new WordchainPlayer(id, lives, (WordchainGame) game),
				guild, channel, gameMaster,
				true, (short) 2, (short) 10
		);

		this.seconds = seconds;
		this.lives = lives;

		sendJoinMessage();
	}

	@Override
	public void start() {
		super.start();

		getChannel().sendMessage("Das Game hat begonnen! (" + uuid + ")").queue(); // TODO: better message

		round = 0;
		playerTurn = 0;

		nextTurn();
	}

	@Override
	protected void handleMessageEvent(WordchainPlayer player, MessageReceivedEvent event) {
		if (!player.equals(players.get(playerTurn))) return;

		String content = event.getMessage().getContentRaw();
		if (content.contains(" ")) {
			event.getMessage().reply(":x: Du darfst nur 1 Wort schreiben! Versuch es noch einmal!").queue();
			return;
		}

		if (words.contains(content.toLowerCase())) {
			event.getMessage().reply(":x: Dieses Wort wurde schon genannt! Versuche es noch einmal!").queue();
			return;
		}

		// TODO: Blocked list

		if (lastWord != null) {
			// check the word and set the new word
			if (content.toLowerCase().charAt(0) != lastWord.toLowerCase().charAt(lastWord.length() - 1)) {
				player.damage(lives -> getChannel().sendMessage(":x: " + player.getAsMention() + " hat ein Fehler gemacht und hat nur noch **" + lives + "** Versuche!").queue());
				return;
			}
		}

		lastWord = content.toLowerCase();
		words.add(lastWord);
		nextTurn();
	}

	@Override
	protected EmbedBuilder buildJoinEmbed() {
		return new EmbedBuilder()
				.setColor(GuildConfig.getColor(guild))
				.setTitle("Neues Wortketten Spiel erstellt!")
				.setDescription("Um zu erfahren wie \"Wortkette\" funktioniert nutze </wordchain explaination:" + Main.discordUtils.getCommandCache().getGuildCommand(guild, "wordchain") + ">") // TODO

				.addField("Spielleiter:in:", "<@" + gameMaster + ">", true)
				.addField(":timer: Timeout:", seconds + "s", true)
				.addField(":x: Max Fehler:", lives + " Fehler", true)
				.addField("Spieler:", players.stream().map(GamePlayer::getAsMention).collect(Collectors.joining("\n")), true)

				.setFooter("GameID: " + uuid)
				.setTimestamp(Instant.now());
	}

	public void nextTurn() {
		if (scheduledFuture != null) scheduledFuture.cancel(true);

		if (players.size() == 1) {
			// TODO: Winn stuff

			getChannel().sendMessage("Spiel vorbei! " + players.get(0).getAsMention() + " hat gewonnen!").queue(); // TODO: better message
			end();
			return;
		}

		round++;
		playerTurn = (playerTurn + 1) % players.size();

		if (lastWord == null) getChannel().sendMessage(round + ": " + this.players.get(playerTurn).getAsMention() + " ist an der Reihe! Suche dir ein Wort aus!").queue();
		if (lastWord != null) getChannel().sendMessage(round + ": " + this.players.get(playerTurn).getAsMention() + " ist an der Reihe! Das letzte Wort ist \"**" + lastWord + "**\"").queue();

		scheduledFuture = Main.executor.schedule(() -> {
			WordchainPlayer player = this.players.get(playerTurn);
			player.damage(l -> getChannel().sendMessage(":x: Die Zeit von " + player.getAsMention() + " ist abgelaufen und hat nur noch **" + l + "** Versuche!").queue());
		}, seconds, TimeUnit.SECONDS);
	}
}