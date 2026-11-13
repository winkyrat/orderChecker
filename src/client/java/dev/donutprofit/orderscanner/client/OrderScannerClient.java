package dev.donutprofit.orderscanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client entrypoint. Until Phase 2 this only exposes the reconnaissance dump. */
public final class OrderScannerClient implements ClientModInitializer {
    public static final String MOD_ID = "donut_order_scanner";

    private static KeyMapping dumpKey;

    @Override
    public void onInitializeClient() {
        dumpKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donut_order_scanner.dump",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (dumpKey.consumeClick()) {
                ScreenDumper.dump(client);
            }
        });
    }
}
