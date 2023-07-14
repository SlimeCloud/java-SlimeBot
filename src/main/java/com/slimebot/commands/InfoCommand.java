package com.slimebot.commands;

import com.slimebot.main.Main;
import com.slimebot.utils.Config;
import de.mineking.discord.commands.annotated.ApplicationCommand;
import de.mineking.discord.commands.annotated.ApplicationCommandMethod;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.time.Instant;

@ApplicationCommand(name = "info", description = "Bekomme genauere Informationen über den Bot")
public class InfoCommand {
	@ApplicationCommandMethod
	public void performCommand(SlashCommandInteractionEvent event) {
		event.replyEmbeds(
				new EmbedBuilder()
						.setTitle("Informationen über den SlimeBall bot")
						.setColor(Main.embedColor(event.getGuild().getId()))
						.setTimestamp(Instant.now())
						.setDescription("Dieser Bot ist ein Custom bot des SlimeCloud Discords und stellt Features bereit die so von keinem anderen Bot gelöst werden können.")
						.addField("Gecodet von:", "[SlimeCloud DevTeam](https://github.com/SlimeCloud)", true)
						.addField("Version:", Config.getBotInfo("version"), true)//ToDo get Version form build.gradle
						.addField("Support:", "Bei Fragen, Verbesserungen, Bugs öffne ein Ticket", true)
						.addField("Prefix:", "Dieser Bot nutzt Slash Commands", true)
						.setFooter("SlimeBall", "https://media.discordapp.net/attachments/1098639892608712714/1098639949592539166/SlimeBall.png")
						.build()
		).setEphemeral(true).queue();
	}
}