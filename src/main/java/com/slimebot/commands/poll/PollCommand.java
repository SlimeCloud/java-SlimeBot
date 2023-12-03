package com.slimebot.commands.poll;

import com.slimebot.main.CommandPermission;
import com.slimebot.main.config.guild.GuildConfig;
import com.slimebot.util.Util;
import de.mineking.discord.commands.annotated.ApplicationCommand;
import de.mineking.discord.commands.annotated.ApplicationCommandMethod;
import de.mineking.discord.commands.annotated.option.Option;
import de.mineking.discord.commands.annotated.option.OptionArray;
import de.mineking.discord.events.Listener;
import de.mineking.discord.events.interaction.StringSelectHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.time.Instant;
import java.util.Arrays;

@ApplicationCommand(name = "poll", description = "Erstelle eine Abstimmung")
public class PollCommand {
	public final CommandPermission permission = CommandPermission.TEAM;

	@ApplicationCommandMethod
	public void performCommand(SlashCommandInteractionEvent event,
	                           @Option(name = "question") String question,
	                           @OptionArray(required = 2, optional = 8) @Option(name = "choice") String... options
	) {
		event.deferReply().flatMap(InteractionHook::retrieveOriginal).queue(mes -> {
			EmbedBuilder embed = new EmbedBuilder()
					.setTitle("Abstimmung")
					.setDescription(question)
					.setColor(GuildConfig.getColor(event.getGuild()))
					.setTimestamp(Instant.now());

			StringSelectMenu.Builder select = StringSelectMenu.create("poll:select")
					.setPlaceholder("Wähle weise");

			for (int i = 0; i < options.length; i++) {
				String option = options[i];
				if (option == null) continue;

				embed.addField(buildField(
						0,
						1,
						0,
						"#" + (i + 1) + "  " + option
				));

				select.addOption("#" + (i + 1) + "  " + option, "#" + (i + 1));
			}

			new Poll(mes.getIdLong(), options.length + 1).save();

			event.getHook().editOriginalEmbeds(embed.build())
					.setActionRow(select.build())
					.queue();
		});
	}

	@Listener(type = StringSelectHandler.class, filter = "poll:select")
	public void handle(StringSelectInteractionEvent event) {
		Poll.getPoll(event.getMessageIdLong()).ifPresent(poll -> {
			MessageEmbed embed = event.getMessage().getEmbeds().get(0);
			EmbedBuilder builder = new EmbedBuilder(embed).clearFields();

			for (int i = 0; i < embed.getFields().size(); i++) {
				MessageEmbed.Field field = embed.getFields().get(i);
				String option = field.getName();
				if (option == null || !option.startsWith("#")) continue;

				String optionNum = option.split(" ")[0];
				if (event.getSelectedOptions().get(0).getValue().equals(optionNum)) {
					Poll.Type res = poll.set(i, event.getMember().getIdLong());
					event.getHook().sendMessage(
							(switch (res) {
								case SET -> "Du hast für option **%s** gestimmt.";
								case REMOVED -> "Du hast deine Stimme von option **%s** entfernt.";
								case REMOVED_SET -> "Du hast deine Stimme zu option **%s** geändert.";
							}).formatted(optionNum)
					).setEphemeral(true).queue();
				}
			}

			poll.save();

			int totalCount = poll.getAll().size();
			int maxCountLength = Arrays.stream(poll.getOptions())
					.map(l -> String.valueOf(l.size()))
					.mapToInt(String::length)
					.max().orElse(0);

			for (int i = 0; i < embed.getFields().size(); i++) {
				MessageEmbed.Field field = embed.getFields().get(i);
				String option = field.getName();
				if (option == null || !option.startsWith("#")) continue;

				int count = poll.getOptions()[i].size();

				builder.addField(buildField(
						poll.getOptions()[i].size(),
						maxCountLength,
						totalCount == 0 ? 0 : ((double) count / totalCount),
						option
				));
			}

			event.editMessageEmbeds(builder.build()).queue();
		});
	}

	private MessageEmbed.Field buildField(int count, int maxCountLength, double percentage, String text) {
		return new MessageEmbed.Field(
				text,
				Util.padRight("(" + count + ") **" + ((int) (percentage * 100)) + "%**", ' ', 1 + maxCountLength + 4 + 3 + 3 + 2) + "\\|" + createProgressbar(percentage) + "\\|",
				false
		);
	}

	private String createProgressbar(double value) {
		StringBuilder base = new StringBuilder("░".repeat(40));
		if (value > 1.0 / 40) base.insert((int) (value * 40), "||").insert(0, "||");
		return base.toString();
	}
}