package io.github.qwerty770;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdvancementCommand {
    @Environment(EnvType.CLIENT)
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("advhelper")
                .executes(context -> {
                    context.getSource().sendFeedback(Component.literal("Advancement Helper 1.0.0 running!"));
                    return 0;
                })
                .then(ClientCommandManager.literal("export").executes(context -> {
                    exportAllAdvancements(context);
                    return 1;
                }).then(ClientCommandManager.argument("namespace", StringArgumentType.string()).executes(context -> {
                    exportAdvancements(context.getArgument("namespace", String.class), context);
                    return 1;
                }))));
    }

    private static void exportAdvancements(String namespace, CommandContext<FabricClientCommandSource> context) {
        Collection<Advancement> advancements = getAdvancements();
        File file = new File("export/" + namespace + "-adv-" +
                new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()) + ".txt");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8);
            for (Advancement adv : advancements){
                if (!adv.getId().getNamespace().equals(namespace)) continue;
                JsonObject json = getJson(adv);
                fileWriter.write(adv.getId() + "\n");
                fileWriter.write(json.toString());
                fileWriter.write("\n");
            }
            fileWriter.close();
            AdvancementHelper.LOGGER.info("Advancement Helper exported advancements of namespace " + namespace);
            context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.success"));
        }
        catch (IOException exception){
            AdvancementHelper.LOGGER.warn("Could not export to a file because:");
            AdvancementHelper.LOGGER.warn(exception.getMessage());
            context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.fail"));
        }
    }
    private static void exportAllAdvancements(CommandContext<FabricClientCommandSource> context) {
        Collection<Advancement> advancements = getAdvancements();
        Map<String, FileWriter> fileWriterMap = new HashMap<>();
        try {
            for (Advancement adv : advancements){
                String ns = adv.getId().getNamespace();
                if (!fileWriterMap.containsKey(ns)){
                    File file = new File("export/" + ns + "-adv-" +
                            new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()) + ".txt");
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    file.createNewFile();
                    FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8);
                    fileWriterMap.put(ns, writer);
                }
                FileWriter fileWriter = fileWriterMap.get(ns);
                JsonObject json = getJson(adv);
                fileWriter.write(adv.getId() + "\n");
                fileWriter.write(json.toString());
                fileWriter.write("\n");
            }
            for (FileWriter writer : fileWriterMap.values()){
                writer.close();
            }
            AdvancementHelper.LOGGER.info("Advancement Helper exported" + advancements.size() + "advancements");
            context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.success"));
        }
        catch (IOException exception){
            AdvancementHelper.LOGGER.warn("Could not export to a file because:");
            AdvancementHelper.LOGGER.warn(exception.getMessage());
            context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.fail"));
        }
    }

    public static Collection<Advancement> getAdvancements(){
        assert Minecraft.getInstance().player != null;
        ClientAdvancements clientAdvancements = Minecraft.getInstance().player.connection.getAdvancements();
        return clientAdvancements.getAdvancements().getAllAdvancements();
    }

    public static JsonObject getJson(Advancement advancement){
        // To avoid exception "JsonSyntaxException: Missing trigger"
        Advancement.Builder builder;
        if (advancement.sendsTelemetryEvent()){
            builder = Advancement.Builder.advancement();
        }
        else {
            builder = Advancement.Builder.recipeAdvancement();
        }
        builder.display(advancement.getDisplay());
        builder.rewards(advancement.getRewards());
        builder.requirements(advancement.getRequirements());
        builder.parent(advancement.getParent() == null ? null : advancement.getParent());
        Map<String, Criterion> criteria = advancement.getCriteria();
        for (String str : criteria.keySet()){
            if (criteria.get(str).getTrigger() == null){
                builder.addCriterion(str, new Criterion(new ImpossibleTrigger.TriggerInstance()));
            }
            else {
                builder.addCriterion(str, criteria.get(str));
            }
        }
        return builder.serializeToJson();
    }
}
