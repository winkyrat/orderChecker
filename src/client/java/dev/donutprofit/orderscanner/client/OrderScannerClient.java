package dev.donutprofit.orderscanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
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

        // Vanilla only processes key mappings when no screen is open, but the
        // menus worth dumping are screens. Listen on the screen instead and
        // consume the press so the menu never sees it.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, context) -> {
                    if (!dumpKey.matches(context)) {
                        return true;
                    }
                    ScreenDumper.dump(client, currentScreen);
                    return false;
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (dumpKey.consumeClick()) {
                ScreenDumper.dump(client, client.screen);
            }
        });
    }
}
