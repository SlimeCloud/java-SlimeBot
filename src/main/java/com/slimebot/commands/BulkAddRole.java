package com.slimebot.commands;


import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

public class BulkAddRole extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        super.onSlashCommandInteraction(event);

        if (!(event.getName().equals("role_check"))){return;}

        Member cmdRunner = (Member) event.getGuild().retrieveMemberById(event.getMember().getId());

        Role staffRole = event.getGuild().getRoleById("1081650648124248124"); //ToDo get ID from a Config eg. Settings



        assert cmdRunner != null;
        if (!(cmdRunner.getRoles().contains(staffRole))) {
            event.reply("kein Teammitglied!").queue();
            return;
        }

        ArrayList<Member> memberWithout = new ArrayList<>();

        OptionMapping botsOption = event.getOption("bots");
        OptionMapping roleOption = event.getOption("rolle");



        for (Member member: event.getGuild().getMembers()) {
            assert botsOption != null;
            User memberAsUser = (User) member;
            if (!botsOption.getAsBoolean() && memberAsUser.isBot()){ //ToDo add check if member is a Bot
                continue;
            }

            assert roleOption != null;
            if (member.getRoles().contains(roleOption.getAsRole())){
                continue;
            } else {
                event.getGuild().addRoleToMember(member, Objects.requireNonNull(event.getGuild().getRoleById(roleOption.getAsString()))).queue();
                memberWithout.add(member);
            }
            String abc = "abc";

        }

        assert roleOption != null;
        event.reply("Die Rolle " + roleOption.getAsRole().getAsMention() + " wurde " + (long) memberWithout.size() + " Member gegeben!").queue();


    }
}