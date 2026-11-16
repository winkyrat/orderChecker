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

/**
 * Client entrypoint.
 *
 * <p>F8 dumps the open screen (reconnaissance and manual capture). F9 toggles
 * the autonomous scan. Both work while a menu is open, which is why they are
 * handled per screen instead of relying on the vanilla key mapping loop.
 */
public final class OrderScannerClient implements ClientModInitializer {
    public static final String MOD_ID = "donut_order_scanner";

    private static KeyMapping dumpKey;
    private static KeyMapping scanKey;
    private static final AutoScanner SCANNER = new AutoScanner();

    @Override
    public void onInitializeClient() {
        // One category instance for both keys: registering it twice throws
        // "Category ... is already registered" and kills the client at startup.
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        dumpKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donut_order_scanner.dump",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                category));

        scanKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donut_order_scanner.scan",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                category));

        // Vanilla only processes key mappings when no screen is open, but the
        // menus worth dumping are screens. Listen on the screen instead and
        // consume the press so the menu never sees it.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, context) -> {
                    if (scanKey.matches(context)) {
                        SCANNER.toggle(client);
                        return false;
                    }
                    if (!dumpKey.matches(context)) {
                        return true;
                    }
                    ScreenDumper.dump(client, currentScreen);
                    return false;
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (scanKey.consumeClick()) {
                SCANNER.toggle(client);
            }
            while (dumpKey.consumeClick()) {
                ScreenDumper.dump(client, client.screen);
            }
            SCANNER.tick(client);
        });
    }
}
