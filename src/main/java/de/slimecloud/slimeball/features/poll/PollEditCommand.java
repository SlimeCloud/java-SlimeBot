package de.slimecloud.slimeball.features.poll;

import de.mineking.discordutils.commands.ApplicationCommand;
import de.mineking.discordutils.commands.ApplicationCommandMethod;
import de.mineking.discordutils.ui.MessageMenu;
import de.mineking.discordutils.ui.MessageRenderer;
import de.mineking.discordutils.ui.UIManager;
import de.mineking.discordutils.ui.components.button.ButtonColor;
import de.mineking.discordutils.ui.components.button.ButtonComponent;
import de.mineking.discordutils.ui.components.button.MenuComponent;
import de.mineking.discordutils.ui.components.select.StringSelectComponent;
import de.mineking.discordutils.ui.components.types.ComponentRow;
import de.mineking.discordutils.ui.modal.ModalMenu;
import de.mineking.discordutils.ui.modal.TextComponent;
import de.slimecloud.slimeball.main.CommandPermission;
import de.slimecloud.slimeball.main.SlimeBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationCommand(name = "Abstimmung bearbeiten", type = Command.Type.MESSAGE)
public class PollEditCommand {
	public final CommandPermission permission = CommandPermission.ROLE_MANAGE; //This makes this command only visible for team members

	private MessageMenu menu;

	public PollEditCommand(@NotNull SlimeBot bot, @NotNull UIManager manager) {
		ModalMenu addModal = manager.createModal(
				"poll.options.add",
				s -> "Option hinzufügen",
				List.of(
						new TextComponent("name", "Option", TextInputStyle.SHORT)
								.setPlaceholder("Ja / Nein")
								.setMaxLength(90)
				),
				(state, response) -> {
					long id = state.getState("id", long.class);
					bot.getPolls().getPoll(id).ifPresent(poll -> {
						poll.getValues().put(response.getString("name"), Collections.emptyList());
						poll.update();
					});

					menu.createState(state).display(state.getEvent());
				}
		);

		ModalMenu renameModal = manager.createModal(
				"poll.options.rename",
				s -> "Option bearbeiten",
				List.of(
						new TextComponent("name", "Option", TextInputStyle.SHORT)
								.setPlaceholder("Ja / Nein")
								.setValue(s -> s.getState("current", String.class))
								.setMaxLength(90)
				),
				(state, response) -> {
					long id = state.getState("id", long.class);
					bot.getPolls().getPoll(id).ifPresent(poll -> {
						List<String> temp = poll.getValues().get(state.getState("current", String.class));
						poll.getValues().put(response.getString("name"), temp);
						poll.getValues().remove(state.getState("current", String.class));
						poll.update();
					});

					menu.createState(state).display(state.getEvent());
				}
		);

		menu = manager.createMenu(
				"poll.edit",
				MessageRenderer.embed(s -> new EmbedBuilder()
						.setColor(bot.getColor(s.getEvent().getGuild()))
						.setTitle("Umfrage bearbeiten")
						.setDescription("https://discord.com/channels/" + s.getEvent().getGuild().getId() + "/" + s.getEvent().getChannel().getId() + "/" + s.getState("id", String.class))
						.appendDescription(s.<Optional<Poll>>getCache("poll").map(p -> p.buildChoices(s.getEvent().getGuild())).orElse("*Nicht gefunden*"))
						.build()
				),
				ComponentRow.of(
						new ButtonComponent("max.label", ButtonColor.GRAY, "Maximale Stimmzahl pro Nutzer").asDisabled(true),
						new ButtonComponent("max.subtract", ButtonColor.BLUE, "-").asDisabled(s -> s.<Optional<Poll>>getCache("poll").map(Poll::getMax).map(m -> m <= 1).orElse(true)).appendHandler(s -> {
							s.<Optional<Poll>>getCache("poll").ifPresent(poll -> poll.setMax(poll.getMax() - 1).update());
							s.update();
						}),
						new ButtonComponent("max.add", ButtonColor.BLUE, "+").asDisabled(s -> s.<Optional<Poll>>getCache("poll").map(Poll::getMax).map(m -> m >= 25).orElse(true)).appendHandler(s -> {
							s.<Optional<Poll>>getCache("poll").ifPresent(poll -> poll.setMax(poll.getMax() + 1).update());
							s.update();
						})
				),
				new StringSelectComponent("options.remove", s -> s.<Optional<Poll>>getCache("poll").map(p -> p.getValues().keySet()).orElse(Collections.emptySet()).stream()
						.map(o -> SelectOption.of(o, o))
						.toList()
				).asDisabled(s -> s.<Optional<Poll>>getCache("poll").map(p -> p.getValues().size()).map(m -> m <= 1).orElse(true)).setPlaceholder("Option entfernen").appendHandler((state, values) -> {
					state.<Optional<Poll>>getCache("poll").ifPresent(poll -> {
						poll.getValues().remove(values.get(0).getValue());
						poll.update();
					});
					state.update();
				}),
				new StringSelectComponent("options.rename", s -> s.<Optional<Poll>>getCache("poll").map(p -> p.getValues().keySet()).orElse(Collections.emptySet()).stream()
						.map(o -> SelectOption.of(o, o))
						.toList()
				).setPlaceholder("Option bearbeiten").appendHandler((state, values) -> renameModal.createState(state)
						.setState("current", values.get(0).getValue())
						.display((GenericComponentInteractionCreateEvent) state.getEvent())
				),
				ComponentRow.of(
						new MenuComponent<>(addModal, ButtonColor.GRAY, "Option hinzufügen").transfereState().asDisabled(s -> s.<Optional<Poll>>getCache("poll").map(p -> p.getValues().size()).map(m -> m >= 9).orElse(true)),
						new ButtonComponent("names", s -> s.<Optional<Poll>>getCache("poll").filter(Poll::isNames).map(p -> ButtonColor.GREEN).orElse(ButtonColor.GRAY), "Namen anzeigen").appendHandler(s -> {
							s.<Optional<Poll>>getCache("poll").ifPresent(poll -> poll.setNames(!poll.isNames()).update());
							s.update();
						}),
						new ButtonComponent("update", ButtonColor.BLUE, "Nachricht aktualisieren").appendHandler(state -> {
							bot.getPolls().getPoll(state.getState("id", long.class)).ifPresent(poll ->
									state.getEvent().getMessageChannel().retrieveMessageById(poll.getId()).map(mes -> mes.getEmbeds().get(0)).queue(old -> {
										String[] temp = old.getDescription().split("### Ergebnisse\n\n", 2);
										state.getEvent().getMessageChannel().editMessageEmbedsById(poll.getId(), new EmbedBuilder(old)
												.clearFields()
												.setDescription((old.getFields().isEmpty() ? temp[0] : old.getDescription() + "\n") + "### Ergebnisse\n\n" + poll.buildChoices(state.getEvent().getGuild()))
												.build()
										).setActionRow(poll.buildMenu(state.getEvent().getGuild())).queue();
									})
							);
							state.update();
						})
				)
		).cache(state -> {
			long id = state.getState("id", long.class);
			state.setCache("poll", bot.getPolls().getPoll(id));
		});
	}

	@ApplicationCommandMethod
	public void performCommand(@NotNull SlimeBot bot, @NotNull MessageContextInteractionEvent event) {
		bot.getPolls().getPoll(event.getTarget().getIdLong()).ifPresentOrElse(
				poll -> menu.createState()
						.setState("id", poll.getId())
						.display(event),
				() -> event.reply(":x: Abstimmung nicht gefunden!").setEphemeral(true).queue()
		);
	}
}
