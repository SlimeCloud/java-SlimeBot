package com.slimebot.events;

import com.slimebot.main.Main;
import com.slimebot.main.config.guild.GuildConfig;
import com.slimebot.main.config.guild.MeetingConfig;
import de.mineking.discord.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MeetingListener extends ListenerAdapter {
	public final static Modal agendaAddModal = Modal.create("meeting:agenda:add", "Punkt zur Agenda hinzufügen")
			.addActionRow(
					TextInput.create("text", "Der Text", TextInputStyle.PARAGRAPH)
							.setPlaceholder("Du kannst mehrere Punkte durch Trennung mit einer neuen Zeile hinzufügen.")
							.build()
			)
			.build();

	public final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyy HH:mm");

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
		String[] id = event.getComponentId().split(":", 2);

		if (!id[0].equals("meeting")) return;

		switch (id[1]) {
			case "end" -> {
				event.editComponents().queue();

				String agenda = event.getMessage().getEmbeds().get(0).getFields().get(0).getValue();

				GuildConfig.getConfig(event.getGuild()).getMeetingConfig()
						.flatMap(MeetingConfig::getTodoChannel)
						.ifPresentOrElse(
								ch -> {
									if (Arrays.stream(agenda.split("\n")).noneMatch(MeetingListener::isValidAgenda)) {
										event.getMessage().replyEmbeds(
												new EmbedBuilder()
														.setTitle("Meeting Resultate")
														.setColor(GuildConfig.getColor(event.getGuild()))
														.setDescription("Das heutige Meeting wurde beendet. Es sind keine ToDos offen geblieben")
														.setTimestamp(Instant.now())
														.build()
										).queue(x -> sendEmptyMessage(event.getGuild(), Instant.now().plus(14, ChronoUnit.DAYS)));
									} else {
										ch.sendMessage(MessageCreateData.fromEditData(buildTodoMessage(event.getGuild(), Arrays.stream(agenda.split("\n")).filter(MeetingListener::isValidAgenda).collect(Collectors.joining("\n"))))).queue(mes -> {
											if (!ch.equals(event.getChannel())) {
												event.getMessage().replyEmbeds(
														new EmbedBuilder()
																.setTitle("Meeting Resultate")
																.setColor(GuildConfig.getColor(event.getGuild()))
																.setDescription("Das heutige Meeting wurde beendet. Daraus entstandene ToDos kannst du hier sehen: " + mes.getJumpUrl())
																.setTimestamp(Instant.now())
																.build()
												).queue(x -> sendEmptyMessage(event.getGuild(), Instant.now().plus(14, ChronoUnit.DAYS)));
											} else sendEmptyMessage(event.getGuild(), Instant.now().plus(14, ChronoUnit.DAYS));
										});
									}
								},
								() -> sendEmptyMessage(event.getGuild(), event.getMessage().getEmbeds().get(0).getTimestamp().toInstant().plus(14, ChronoUnit.DAYS))
						);
			}
			case "agenda:add" -> event.replyModal(agendaAddModal).queue();

			case "presence:yes" -> editPresence(event, event.getUser(), 2);
			case "presence:unknown" -> editPresence(event, event.getUser(), 3);
			case "presence:no" -> editPresence(event, event.getUser(), 4);
		}
	}

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		String[] id = event.getModalId().split(":", 2);

		if (!id[0].equals("meeting")) return;

		switch (id[1]) {
			case "agenda:add" -> {
				EmbedBuilder builder = new EmbedBuilder(event.getMessage().getEmbeds().get(0));

				for (String a : event.getValue("text").getAsString().split("\n"))
					modifyFieldValue(builder, 0, value -> value + (value.length() <= 1 ? "1. " : "\n" + (value.split("\n").length + 1) + ". ") + event.getUser().getAsMention() + ": " + a);

				event.editMessageEmbeds(builder.build())
						.setComponents(buildComponents(builder.getFields().get(0).getValue()))
						.queue();
			}

			case "agenda:edit" -> {
				EmbedBuilder builder = new EmbedBuilder(event.getMessage().getEmbeds().get(0));

				modifyFieldValue(builder, 0, value -> {
					int i = Integer.parseInt(event.getValue("id").getAsString());
					String[] temp = value.split("\n");

					temp[i - 1] = i + ". " + event.getValue("text").getAsString();

					return String.join("\n", temp);
				});

				event.editMessageEmbeds(builder.build())
						.setComponents(buildComponents(builder.getFields().get(0).getValue()))
						.queue();
			}
		}
	}

	@Override
	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		switch (event.getComponentId()) {
			case "meeting:agenda:remove" -> {
				EmbedBuilder builder = new EmbedBuilder(event.getMessage().getEmbeds().get(0));
				modifyFieldValue(builder, 0, value -> {
					int i = Integer.parseInt(event.getSelectedOptions().get(0).getValue());
					String[] temp = value.split("\n");

					temp[i] = temp[i].split(". ", 2)[0] + ". ~~" + temp[i].split(". ", 2)[1] + "~~";
					return String.join("\n", temp);
				});

				event.editMessageEmbeds(builder.build())
						.setComponents(buildComponents(builder.getFields().get(0).getValue()))
						.queue();
			}

			case "meeting:agenda:edit" -> {
				int i = Integer.parseInt(event.getSelectedOptions().get(0).getValue());
				event.replyModal(
						Modal.create("meeting:agenda:edit", "Punkt zur Agenda hinzufügen")
								.addActionRow(
										TextInput.create("id", "Die ID des Agendapunkts", TextInputStyle.SHORT)
												.setValue(String.valueOf(i + 1))
												.build()
								)
								.addActionRow(
										TextInput.create("text", "Der Text", TextInputStyle.SHORT)
												.setValue(event.getMessage().getEmbeds().get(0).getFields().get(0).getValue().split("\n")[i].split(". ", 2)[1])
												.build()
								)
								.build()
				).queue();
			}

			case "todo:done" -> {
				int i = Integer.parseInt(event.getSelectedOptions().get(0).getValue());
				String[] temp = event.getMessage().getEmbeds().get(0).getDescription().split("\n");

				temp[i] = temp[i].split(". ", 2)[0] + ". ~~" + temp[i].split(". ", 2)[1] + "~~";

				event.editMessage(
						buildTodoMessage(
								event.getGuild(),
								String.join("\n", temp)
						)
				).queue();
			}

			case "todo:assign" -> {
				int i = Integer.parseInt(event.getSelectedOptions().get(0).getValue());
				String[] temp = event.getMessage().getEmbeds().get(0).getDescription().split("\n");

				temp[i] = temp[i] + " (" + event.getMember().getAsMention() + ")";

				event.editMessage(
						buildTodoMessage(
								event.getGuild(),
								String.join("\n", temp)
						)
				).queue();
			}
		}
	}

	private void editPresence(ButtonInteractionEvent event, User user, int field) {
		EmbedBuilder builder = new EmbedBuilder(event.getMessage().getEmbeds().get(0));

		for (int i = 2; i <= 4; i++) modifyFieldValue(builder, i, value -> value.replace(user.getAsMention() + (value.contains(user.getAsMention() + "\n") ? "\n" : ""), ""));
		modifyFieldValue(builder, field, value -> value + (value.length() == 1 ? "" : "\n") + user.getAsMention());

		event.editMessageEmbeds(builder.build()).queue();
	}

	private void modifyFieldValue(EmbedBuilder builder, int field, Function<String, String> handler) {
		MessageEmbed.Field f = builder.getFields().get(field);
		builder.getFields().set(field, new MessageEmbed.Field(
				f.getName(),
				handler.apply(f.getValue()),
				f.isInline()
		));
	}

	public static void sendEmptyMessage(Guild guild, Instant time) {
		GuildConfig.getConfig(guild).getMeetingConfig().flatMap(MeetingConfig::getMeetingChannel).ifPresent(channel ->
				channel.sendMessage(
						new MessageCreateBuilder()
								.setContent(GuildConfig.getConfig(guild).getStaffRole().map(Role::getAsMention).orElse("") + " Beginnt " + TimeFormat.RELATIVE.format(Main.atTime(time, 20)))
								.setComponents(buildComponents(""))
								.setEmbeds(
										new EmbedBuilder()
												.setColor(GuildConfig.getColor(guild))
												.setTitle("Team Besprechung **" + formatter.format(Main.atTime(time, 20)) + "**")
												.setThumbnail(guild.getIconUrl())
												.addField(
														"Agenda",
														"",
														false
												)
												.addBlankField(false)
												.addField(
														"Voraussichtlich Anwesend",
														"",
														true
												)
												.addField(
														"Nicht sicher\n" + EmbedBuilder.ZERO_WIDTH_SPACE,
														GuildConfig.getConfig(guild).getStaffRole()
																.map(r -> guild.getMembersWithRoles(r).stream()
																		.map(Member::getAsMention)
																		.collect(Collectors.joining("\n"))
																)
																.orElse(""),
														true
												)
												.addField(
														"Voraussichtlich abwesend",
														"",
														true
												)
												.setTimestamp(Main.atTime(time, 20))
												.build()
								)
								.build()
				).queue(
						msg -> GuildConfig.getConfig(guild).getMeetingConfig().flatMap(MeetingConfig::getVoiceChannel).ifPresent(ch ->
								guild.createScheduledEvent(
												"\uD83D\uDCDC Teamsitzung",
												ch,
												Main.atTime(time, 20).toOffsetDateTime()
										).setDescription("Alle Infos: " + msg.getJumpUrl())
										.flatMap(e -> msg.editMessageEmbeds(
												new EmbedBuilder(msg.getEmbeds().get(0))
														.setDescription("https://discord.com/events/%s/%s".formatted(
																e.getGuild().getId(),
																e.getId()
														))
														.build()
										))
										.queue()
						)
				)
		);
	}

	private static MessageEditData buildTodoMessage(Guild guild, String agenda) {
		StringSelectMenu.Builder done = StringSelectMenu.create("todo:done")
				.setPlaceholder("ToDo abhaken");
		StringSelectMenu.Builder assign = StringSelectMenu.create("todo:assign")
				.setPlaceholder("ToDo übernehmen");

		String[] temp = agenda.split("\n");

		for (int i = 0; i < temp.length; i++) {
			if (temp[i].contains(". ")) temp[i] = (i + 1) + ". " + temp[i].split(". ", 2)[1];

			if (isValidAgenda(temp[i])) {
				SelectOption option = SelectOption.of(
						Utils.label(
								temp[i].replaceAll(" \\(<@\\d+>\\)", ""),
								SelectOption.LABEL_MAX_LENGTH
						),
						String.valueOf(i)
				).withDescription(
						Arrays.stream(temp[i].replaceAll("[^\\d ]|((?<!<@|\\d)\\d+(?!>))", "").split(" "))
								.filter(id -> !id.isEmpty())
								.map(id -> guild.retrieveMemberById(id).complete().getEffectiveName())
								.collect(Collectors.joining(" "))
				);

				done.addOptions(option);
				assign.addOptions(option);
			}
		}

		if (done.getOptions().isEmpty()) {
			done.setDisabled(true).addOption("---", "---");
			assign.setDisabled(true).addOption("---", "---");
		}

		return new MessageEditBuilder()
				.setEmbeds(
						new EmbedBuilder()
								.setTitle("ToDo des Meetings vom **" + formatter.format(Main.atTime(Instant.now(), 20)) + "**")
								.setColor(GuildConfig.getColor(guild))
								.setDescription(agenda)
								.setTimestamp(Instant.now())
								.build()
				)
				.setComponents(
						ActionRow.of(assign.build()),
						ActionRow.of(done.build())
				)
				.build();
	}

	private static boolean isValidAgenda(String s) {
		return s.length() > 1 && s.matches("\\D*\\d+. (?!~~).*(?!~~)$");
	}

	private static List<ActionRow> buildComponents(String agenda) {
		return List.of(
				ActionRow.of(
						Button.success("meeting:presence:yes", "Ich kann"),
						Button.secondary("meeting:presence:unknown", "Vielleicht"),
						Button.danger("meeting:presence:no", "Ich kann nicht"),
						Button.primary("meeting:agenda:add", "Agenda-Punkt hinzufügen")
				),
				ActionRow.of(createAgendaSelect(agenda, "remove", "Agenda-Punkt entfernen")),
				ActionRow.of(createAgendaSelect(agenda, "edit", "Agenda-Punkt bearbeiten")),
				ActionRow.of(Button.danger("meeting:end", "Meeting beenden"))
		);
	}

	private static StringSelectMenu createAgendaSelect(String agenda, String id, String placeholder) {
		StringSelectMenu.Builder builder = StringSelectMenu.create("meeting:agenda:" + id)
				.setPlaceholder(placeholder);

		String[] temp = agenda.split("\n");

		for (int i = 0; i < temp.length; i++)
			if (isValidAgenda(temp[i]))
				builder.addOption(Utils.label(temp[i], SelectOption.LABEL_MAX_LENGTH), String.valueOf(i));

		if (builder.getOptions().isEmpty()) builder.setDisabled(true).addOption("---", "---");

		return builder.build();
	}
}
