package io.github.qwerty770;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class AdvancementHelper implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("advhelper");
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> AdvancementCommand.register(dispatcher)));
        LOGGER.info("Advancement Helper initialized!");
    }
}