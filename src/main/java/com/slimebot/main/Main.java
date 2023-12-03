package com.slimebot.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.slimebot.alerts.holidays.HolidayAlert;
import com.slimebot.alerts.spotify.SpotifyListener;
import com.slimebot.commands.*;
import com.slimebot.commands.config.ConfigCommand;
import com.slimebot.commands.config.setup.SetupCommand;
import com.slimebot.commands.level.LeaderboardCommand;
import com.slimebot.commands.level.LevelCommand;
import com.slimebot.commands.level.RankCommand;
import com.slimebot.commands.level.card.CardCommand;
import com.slimebot.commands.poll.PollCommand;
import com.slimebot.commands.report.MessageReportCommand;
import com.slimebot.commands.report.ReportCommand;
import com.slimebot.commands.report.UserReportCommand;
import com.slimebot.commands.report.UserReportSlashCommand;
import com.slimebot.database.Database;
import com.slimebot.events.*;
import com.slimebot.main.config.Config;
import com.slimebot.main.config.guild.GuildConfig;
import com.slimebot.message.StaffMessage;
import de.mineking.discord.DiscordUtils;
import de.mineking.discord.commands.ContextBase;
import de.mineking.discord.commands.ContextCreator;
import de.mineking.discord.commands.inherited.Option;
import de.mineking.discord.list.ListCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Main {
	public final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(0);

	public final static Gson gson = new Gson();

	public final static Gson prettyGson = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	static {
		ListCommand.pageOption = new Option(OptionType.INTEGER, "seite", "Startseite").range(1, null);
	}

	public static Config config;

	public static JDA jdaInstance;

	public static Database database;
	public static DiscordUtils discordUtils;

	public static SpotifyListener spotify;
	public static HolidayAlert holiday;
	public static GitHub github;

	public static void main(String[] args) throws IOException {
		config = Config.readFromFile("config");

		logger.info("Bot Version: {}", BuildInfo.version);

		if (args.length == 0) {
			logger.error("BITTE EIN TOKEN NAME ALS COMMAND-LINE-PARAMETER ÜBERGEBEN (.env im bot-ordner)");
			System.exit(420);
		}

		String tokenName = args[0];

		logger.info("{}-Bot wird gestartet...", tokenName);
		String token = Config.env.get("TOKEN_" + tokenName.toUpperCase());
		if (token == null || token.isEmpty()) {
			logger.error("BITTE EIN TOKEN ANGEBEN (.env im bot-ordner)");
			System.exit(421);
		}

		database = new Database();

		boolean dbAvailable = Main.config.database != null;

		holiday = new HolidayAlert();

		JDABuilder jdaBuilder = JDABuilder.createDefault(token)
				.setActivity(config.activity.build())

				.enableIntents(EnumSet.allOf(GatewayIntent.class))
				.setEventPassthrough(true)
				.setMemberCachePolicy(MemberCachePolicy.ALL)

				//Events
				.addEventListeners(new ReadyListener())
				.addEventListeners(new AutoDeleteListener())
				.addEventListeners(new TimeoutListener())
				.addEventListeners(new MeetingListener())
				.addEventListeners(new StaffMessage())
				.addEventListeners(new MemberJoinListener());

		discordUtils = new DiscordUtils("", jdaBuilder)
				.useCustomRestactionManager(null)
				.useEventManager(null)
				.useListCommands(null)
				.useUIManager(config -> config.setDefaultHandler(
						event -> event.reply("Diese Interaktion ist abgelaufen! Führe den Befehl erneut aus um ein neues Menü zu erhalten!").setEphemeral(true).queue()
				))
				.useCommandManager(
						new ContextCreator<>(ContextBase.class, CommandContext::new),
						config -> {
							/*
							Hier kannst du deine Befehle registrieren.
							 */
							config.registerCommand(ConfigCommand.class);

							if (Main.config.github != null) {
								try {
									github = new GitHubBuilder()
											.withOAuthToken(Main.config.github.accessToken)
											.build();

									config.registerCommand(BugCommand.class);
									config.registerCommand(BugContextCommand.class);
								} catch (IOException e) {
									logger.error("Initialisieren der GitHub API fehlgeschlagen", e);
								}
							} else {
								logger.warn("Bug-Reporting aufgrund von fehlender GitHub konfiguration deaktiviert");
							}

							config.registerCommand(BulkAddRoleCommand.class);
							config.registerCommand(PingCommand.class);
							config.registerCommand(FdmdsCommand.class);
							config.registerCommand(InfoCommand.class);
							config.registerCommand(BonkCommand.class);
							config.registerCommand(ContributorCommand.class);

							config.registerCommand(SetupCommand.class);

							config.registerCommand(QuoteCommand.class);
							config.registerCommand(QuoteMessageCommand.class);

							config.registerCommand(WordchainCommand.class);

							if (dbAvailable) {
								if (Main.config.level != null) {
									config.registerCommand(RankCommand.class);
									config.registerCommand(LeaderboardCommand.class);
									config.registerCommand(LevelCommand.class);
									config.registerCommand(CardCommand.class);
								} else logger.warn("Level System aufgrund fehlender Config deaktiviert");
							} else logger.warn("Level System aufgrund von fehlender Datenbank deaktiviert");

							if (dbAvailable) {
								config.registerCommand(UserReportCommand.class);
								config.registerCommand(MessageReportCommand.class);
								config.registerCommand(UserReportSlashCommand.class);
								config.registerCommand(ReportCommand.class);
							} else logger.warn("Report System aufgrund von fehlender Datenbank deaktiviert");

							if (dbAvailable) config.registerCommand(PollCommand.class);
							else logger.warn("Poll System aufgrund von fehlender Datenbank deaktiviert");
						}
				)
				.useCommandCache(null);

		jdaInstance = discordUtils.build();

		if (dbAvailable && Main.config.level != null) jdaInstance.addEventListener(new LevelListener());

		if (config.spotify != null) spotify = new SpotifyListener();
		else logger.info("No spotify configuration found - Disabled spotify notifications");
	}

	/**
	 * Updatet die Befehle eines Servers. Diese Methode sollte immer aufgerufen werden, wenn Konfiguration verändert wird, die Befehle aktivieren oder deaktivieren kann.
	 *
	 * @param guild Der server, dessen Befehle geupdatet werden sollen.
	 */
	public static void updateGuildCommands(Guild guild) {
		GuildConfig config = GuildConfig.getConfig(guild);

		discordUtils.getCommandCache().updateGuildCommands(guild,
				Map.of(
						"fdmds", config.getFdmds().isPresent(),
						"level", config.getLevelConfig().isPresent(),
						"quote", config.getQuoteConfig().isPresent()
				),
				error -> logger.error("Failed to update guild commands for " + guild, error)
		);
	}

	public static ZonedDateTime atTime(Instant in, int hour) {
		return in.atZone(ZoneId.systemDefault())
				.withHour(hour)
				.withMinute(0)
				.withSecond(0);
	}

	/**
	 * Registriert eine Aufgabe, die täglich ausgeführt wird.
	 *
	 * @param hour Die Stunde, zu der die Aufgabe ausgeführt wird.
	 * @param task Die Aufgabe
	 */
	public static void scheduleDaily(int hour, Runnable task) {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime nextRun = now.withHour(hour).withMinute(0).withSecond(0);
		if (now.compareTo(nextRun) > 0)
			nextRun = nextRun.plusDays(1);

		long initialDelay = Duration.between(now, nextRun).getSeconds();

		executor.scheduleAtFixedRate(() -> {
			try {
				task.run();
			} catch (Exception e) {
				logger.error("Exception when executing the daily task", e);
			}
		}, initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
	}

	/**
	 * Registriert eine Aufgabe, die im angegebenen Intervall ausgeführt wird.
	 *
	 * @param amount Das Intervall
	 * @param unit   Die Einheit, in der das Intervall angegeben wurde.
	 * @param task   Die Aufgabe
	 */
	public static void scheduleAtFixedRate(int amount, TimeUnit unit, Runnable task) {
		executor.scheduleAtFixedRate(() -> {
			try {
				task.run();
			} catch (Exception e) {
				logger.error("Exception when executing the task", e);
			}
		}, 0, amount, unit);
	}

	public static Logger getLogger() {
		return logger;
	}

	public static <T> RestAction<T> emptyAction(T value) {
		return new CompletedRestAction<>(jdaInstance, value);
	}
}
