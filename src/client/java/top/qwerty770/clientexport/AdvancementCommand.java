package top.qwerty770.clientexport;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.advancements.*;
import net.minecraft.network.chat.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static top.qwerty770.clientexport.AdvancementTool.*;
import static top.qwerty770.clientexport.util.FileUtil.*;

public class AdvancementCommand {
    protected static void exportAdvancements(String namespace, CommandContext<FabricClientCommandSource> context) {
        AdvancementTree tree = getAdvancementTree();
        List<AdvancementHolder> advancements = getAdvancements();
        try {
            JsonObject json = new JsonObject();
            for (AdvancementHolder adv : advancements){
                if (!adv.id().getNamespace().equals(namespace)) continue;
                json.add(adv.id().toString(), getJson(adv, tree));
            }
            FileWriter fileWriter = createFile(namespace, "advancements");
            writeJson(fileWriter, json);
            fileWriter.close();
            ClientExportHelper.LOGGER.info("Client Export Helper exported {} advancements of namespace {}", advancements.size(), namespace);
            context.getSource().sendFeedback(Component.translatable("commands.clientexport.advancements.success", advancements.size()));
        }
        catch (IOException exception){
            sendFailure(exception, context, "commands.clientexport.advancements.fail");
        }
    }

    protected static void exportAllAdvancements(CommandContext<FabricClientCommandSource> context) {
        AdvancementTree tree = getAdvancementTree();
        List<AdvancementHolder> advancements = getAdvancements();
        Map<String, JsonObject> jsonMap = new HashMap<>();
        Map<String, FileWriter> fileWriterMap = new HashMap<>();
        try {
            for (AdvancementHolder adv : advancements){
                String ns = adv.id().getNamespace();
                if (!fileWriterMap.containsKey(ns)){
                    FileWriter writer = createFile(ns, "advancements");
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
            ClientExportHelper.LOGGER.info("Client Export Helper exported {} advancements", advancements.size());
            context.getSource().sendFeedback(Component.translatable("commands.clientexport.advancements.success", advancements.size()));
        }
        catch (IOException exception){
            sendFailure(exception, context, "commands.clientexport.advancements.fail");
        }
    }

    protected static void exportProgress(String namespace, CommandContext<FabricClientCommandSource> context) {
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
            ClientExportHelper.LOGGER.info("Client Export Helper exported {} lines of advancement progress of namespace {}", progress.size(), namespace);
            context.getSource().sendFeedback(Component.translatable("commands.clientexport.progress.success", progress.size()));
        }
        catch (IOException exception){
            sendFailure(exception, context, "commands.clientexport.progress.fail");
        }
    }

    protected static void exportAllProgress(CommandContext<FabricClientCommandSource> context) {
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
            ClientExportHelper.LOGGER.info("Client Export Helper exported {} lines of advancement progress", progress.size());
            context.getSource().sendFeedback(Component.translatable("commands.clientexport.progress.success", progress.size()));
        }
        catch (IOException exception){
            sendFailure(exception, context, "commands.clientexport.progress.fail");
        }
    }
}
