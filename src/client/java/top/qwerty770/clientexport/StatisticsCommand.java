package top.qwerty770.clientexport;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stat;
import top.qwerty770.clientexport.mixin.ServerStatsCounterAccessor;
import top.qwerty770.clientexport.mixin.StatsCounterAccessor;

import java.io.FileWriter;
import java.io.IOException;

import static top.qwerty770.clientexport.util.FileUtil.*;

public class StatisticsCommand {
    public static void exportStatistics(CommandContext<FabricClientCommandSource> context) {
        assert Minecraft.getInstance().player != null;
        Object2IntMap<Stat<?>> stats = ((StatsCounterAccessor) Minecraft.getInstance().player.getStats()).getStats();
        try {
            JsonObject json = new JsonObject();
            json.add("stats", ServerStatsCounterAccessor.getStatsCodec().encodeStart(JsonOps.INSTANCE, stats).getOrThrow());
            json.addProperty("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
            FileWriter fileWriter = createFile("statistics");
            writeJson(fileWriter, json);
            fileWriter.close();
            ClientExportHelper.LOGGER.info("Client Export Helper exported {} lines of statistics", stats.size());
            context.getSource().sendFeedback(Component.translatable("commands.clientexport.statistics.success", stats.size()));
        }
        catch (IOException exception){
            sendFailure(exception, context, "commands.clientexport.statistics.fail");
        }
    }
}
