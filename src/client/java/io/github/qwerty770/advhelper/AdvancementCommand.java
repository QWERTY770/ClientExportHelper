package io.github.qwerty770.advhelper;

import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.advancements.*;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.github.qwerty770.advhelper.AdvancementHelper.version;
import static io.github.qwerty770.advhelper.AdvancementTool.*;

public class AdvancementCommand {
    @Environment(EnvType.CLIENT)
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("advhelper")
                .executes(context -> {
                    context.getSource().sendFeedback(Component.literal("Advancement Helper " + version + " running!"));
                    return 0;
                })
                .then(ClientCommandManager.literal("export").executes(context -> {
                    exportAllAdvancements(context);
                    return 1;
                }).then(ClientCommandManager.argument("namespace", StringArgumentType.string()).executes(context -> {
                    exportAdvancements(context.getArgument("namespace", String.class), context);
                    return 1;
                })))
                .then(ClientCommandManager.literal("progress").executes(context -> {
                    exportAllProgress(context);
                    return 1;
                }).then(ClientCommandManager.argument("namespace", StringArgumentType.string()).executes(context -> {
                    exportProgress(context.getArgument("namespace", String.class), context);
                    return 1;
                })))
        );
    }

    private static void exportAdvancements(String namespace, CommandContext<FabricClientCommandSource> context) {
        AdvancementTree tree = getAdvancementTree();
        List<AdvancementHolder> advancements = getAdvancements();
        try {
            JsonObject json = new JsonObject();
            for (AdvancementHolder adv : advancements){
                if (!adv.id().getNamespace().equals(namespace)) continue;
                json.add(adv.id().toString(), getJson(adv, tree));
            }
            FileWriter fileWriter = createFile(namespace, "adv");
            writeJson(fileWriter, json);
            fileWriter.close();
            AdvancementHelper.LOGGER.info("Advancement Helper exported the advancements of namespace {}", namespace);
            context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.success"));
        }
        catch (IOException exception){
            sendFailure(exception, context);
        }
    }

    private static void exportAllAdvancements(CommandContext<FabricClientCommandSource> context) {
        AdvancementTree tree = getAdvancementTree();
        List<AdvancementHolder> advancements = getAdvancements();
        Map<String, JsonObject> jsonMap = new HashMap<>();
        Map<String, FileWriter> fileWriterMap = new HashMap<>();
        try {
            for (AdvancementHolder adv : advancements){
                String ns = adv.id().getNamespace();
                if (!fileWriterMap.containsKey(ns)){
                    FileWriter writer = createFile(ns, "adv");
                    fileWriterMap.put(ns, writer);
                }
                if (!jsonMap.containsKey(ns)){
                    jsonMap.put(ns, new JsonObject());
                }
                jsonMap.get(ns).add(adv.id().toString(), getJson(adv, tree));
            }
            for (String ns : jsonMap.keySet()){
                writeJson(fileWriterMap.get(ns), jsonMap.get(ns));
            }
            for (FileWriter writer : fileWriterMap.values()){
                writer.close();
            }
            AdvancementHelper.LOGGER.info("Advancement Helper exported {} advancements", advancements.size());
            context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.success"));
        }
        catch (IOException exception){
            sendFailure(exception, context);
        }
    }

    private static void exportProgress(String namespace, CommandContext<FabricClientCommandSource> context) {
        Map<AdvancementHolder, AdvancementProgress> progress = getProgress();
        try {
            JsonObject json = new JsonObject();
            for (AdvancementHolder adv : progress.keySet()){
                if (!adv.id().getNamespace().equals(namespace)) continue;
                json.add(adv.id().toString(), getJson(progress.get(adv)));
            }
            FileWriter fileWriter = createFile(namespace, "progress");
            writeJson(fileWriter, json);
            fileWriter.close();
            AdvancementHelper.LOGGER.info("Advancement Helper exported the advancements' progress of namespace {}", namespace);
            context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.success"));
        }
        catch (IOException exception){
            sendFailure(exception, context);
        }
    }

    private static void exportAllProgress(CommandContext<FabricClientCommandSource> context) {
        Map<AdvancementHolder, AdvancementProgress> progress = getProgress();
        Map<String, JsonObject> jsonMap = new HashMap<>();
        Map<String, FileWriter> fileWriterMap = new HashMap<>();
        try {
            for (AdvancementHolder adv : progress.keySet()){
                String ns = adv.id().getNamespace();
                if (!fileWriterMap.containsKey(ns)){
                    FileWriter writer = createFile(ns, "progress");
                    fileWriterMap.put(ns, writer);
                }
                if (!jsonMap.containsKey(ns)){
                    jsonMap.put(ns, new JsonObject());
                }
                jsonMap.get(ns).add(adv.id().toString(), getJson(progress.get(adv)));
            }
            for (String ns : jsonMap.keySet()){
                writeJson(fileWriterMap.get(ns), jsonMap.get(ns));
            }
            for (FileWriter writer : fileWriterMap.values()){
                writer.close();
            }
            AdvancementHelper.LOGGER.info("Advancement Helper exported {} advancements' progress", progress.size());
            context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.success"));
        }
        catch (IOException exception){
            sendFailure(exception, context);
        }
    }

    private static FileWriter createFile(String namespace, String type) throws IOException {
        File file = new File("export/" + namespace + "-" + type + "-" +
                new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()) + ".json");
        if (!file.getParentFile().exists()) {
            if(file.getParentFile().mkdirs()){
                AdvancementHelper.LOGGER.debug("Created path: {}", file.getParentFile().getPath());
            }
        }
        if (!file.createNewFile()) {
            AdvancementHelper.LOGGER.warn("File already exists: {}.", file.getName());
        }
        return new FileWriter(file, StandardCharsets.UTF_8);
    }

    private static void sendFailure(IOException exception, CommandContext<FabricClientCommandSource> context){
        AdvancementHelper.LOGGER.error("Failed to export to a file");
        AdvancementHelper.LOGGER.error(exception.getMessage());
        context.getSource().sendFeedback(Component.translatable("commands.advhelper.export.fail"));
    }

    private static void writeJson(FileWriter fileWriter, JsonObject jsonObject) throws IOException {
        JsonWriter jsonWriter = new JsonWriter(fileWriter);
        jsonWriter.setLenient(true);
        jsonWriter.setIndent("  ");
        Streams.write(jsonObject, jsonWriter);
    }
}
