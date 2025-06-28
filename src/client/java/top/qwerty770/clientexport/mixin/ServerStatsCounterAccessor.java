package top.qwerty770.clientexport.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ServerStatsCounter.class)
public interface ServerStatsCounterAccessor {
    @Accessor("STATS_CODEC")
    static Codec<Map<Stat<?>, Integer>> getStatsCodec(){
        throw new AssertionError();
    }
}
