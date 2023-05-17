package com.slimebot.report.modals;

import com.slimebot.main.Main;
import com.slimebot.report.assets.Report;
import com.slimebot.report.assets.Status;
import com.slimebot.report.assets.Type;
import com.slimebot.report.commands.ReportCmd;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public class ReportModal extends ListenerAdapter {

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        super.onModalInteraction(event);

        if (!(event.getModalId().equals("userReport"))){return;}

        Report currentReport = null;

        ModalMapping id = event.getValue("id");
        ModalMapping description = event.getValue("usrDescr");

        for (Report report:Main.reports) {
            if (Objects.equals(String.valueOf(report.getId()), id.getAsString()) && event.getMember() == report.getBy()) {
                currentReport = report;
                break;
            }
        }

        currentReport.setMsgContent(description.getAsString());

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTimestamp(LocalDateTime.now().atZone(ZoneId.systemDefault()))
                .setColor(Main.embedColor)
                .setTitle(":white_check_mark: Report Erfolgreich")
                .setDescription(currentReport.getUser().getAsMention() + " wurde erfolgreich gemeldet");
        event.replyEmbeds(embedBuilder.build()).queue();
        Report.log(currentReport.getId());

    }
}