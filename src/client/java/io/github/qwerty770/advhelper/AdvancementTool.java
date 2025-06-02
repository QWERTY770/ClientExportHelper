package io.github.qwerty770.advhelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.qwerty770.advhelper.mixin.ClientAdvancementsMixin;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public class AdvancementTool {
    public static AdvancementTree getAdvancementTree() {
        assert Minecraft.getInstance().player != null;
        ClientAdvancements clientAdvancements = Minecraft.getInstance().player.connection.getAdvancements();
        return clientAdvancements.getTree();
    }

    public static List<AdvancementHolder> getAdvancements() {
        return getAdvancementTree().nodes().stream().map(AdvancementNode::holder).toList();
    }

    public static JsonElement getJson(AdvancementHolder holder, AdvancementTree tree) {
        // To avoid exception "JsonSyntaxException: Missing trigger"
        Advancement.Builder builder;
        Advancement advancement = holder.value();
        if (advancement.sendsTelemetryEvent()) {
            builder = Advancement.Builder.advancement();
        } else {
            builder = Advancement.Builder.recipeAdvancement();
        }
        advancement.display().ifPresent(builder::display);
        builder.rewards(advancement.rewards());
        builder.requirements(advancement.requirements());
        for (List<String> strings : advancement.requirements().requirements()) {
            for (String string : strings) {
                // Criteria are not synchronized to the client
                builder.addCriterion(string, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()));
            }
        }
        if (advancement.parent().isPresent()) {
            AdvancementNode node = tree.get(advancement.parent().orElseGet(() -> ResourceLocation.parse("")));
            if (node != null) builder.parent(node.holder());
        }
        try {
            return Advancement.CODEC.encodeStart(JsonOps.INSTANCE, builder.build(holder.id()).value()).getOrThrow();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return error(e, "Failed to encode the advancement {}!", holder.id().toString());
        } catch (Exception e){
            return error(e, "Failed to encode the advancement {} due to an unexpected error!", holder.id().toString());
        }
    }

    public static Map<AdvancementHolder, AdvancementProgress> getProgress() {
        assert Minecraft.getInstance().player != null;
        ClientAdvancements clientAdvancements = Minecraft.getInstance().player.connection.getAdvancements();
        return ((ClientAdvancementsMixin) clientAdvancements).getProgress();
    }

    public static JsonElement getJson(AdvancementProgress progress) {
        try {
            JsonObject result = AdvancementProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow().getAsJsonObject();
            JsonArray remaining = new JsonArray();
            progress.getRemainingCriteria().forEach(remaining::add);
            result.add("remaining", remaining);
            result.addProperty("done_percent", progress.getPercent());
            return result;
        } catch (IllegalStateException | IllegalArgumentException e) {
            return error(e, "Failed to encode the progress of the advancement {}!", progress.toString());
        } catch (Exception e){
            return error(e, "Failed to encode the progress of the advancement {} due to an unexpected error!", progress.toString());
        }
    }

    private static JsonElement error(Exception exception, String str1, String str2) {
        AdvancementHelper.LOGGER.error(str1, str2);
        AdvancementHelper.LOGGER.error(exception.getMessage());
        JsonObject fail = new JsonObject();
        fail.addProperty("error", exception.getMessage());
        return fail;
    }
}
