package com.asilvorcarp;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static com.asilvorcarp.ApexMC.MOD_ID;

public class ModConfig implements ModMenuApi, ConfigScreenFactory<Screen> {
    public static final File CFG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            MOD_ID + ".properties");
    // the num of pings save for each player (work only in client side)
    public static int pingNumEach = 3;
    // whether we can ping on the fluid
    public static boolean includeFluids = false;
    // resize the icon hud
    public static float iconSize = 1f;
    // info shown when looking at the ping point, format: 0xAARRGGBB
    public static int infoColor = 0xFFeb9d39;
    // 0 means never, only work at your client side
    public static long secondsToVanish = 0;
    // color of the highlight block
    public static Color highlightColor = new Color(247 / 256f, 175 / 256f, 53 / 256f);
    // the index of the ping sound
    public static byte soundIndex = 0;

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return this;
    }

    @Override
    public Screen create(Screen screen) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(screen)
                .setTitle(Text.translatable("title." + MOD_ID + ".config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config." + MOD_ID + ".general"));

        // Toggle include fluids
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config." + MOD_ID + ".includeFluids"), includeFluids)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + MOD_ID + ".includeFluids.description"))
                .setSaveConsumer(ModConfig::setIncludeFluids)
                .build());

        // sound index
        general.addEntry(entryBuilder.startIntField(Text.translatable("config." + MOD_ID + ".soundIndex"), soundIndex)
                .setDefaultValue(0)
                .setTooltip(Text.translatable("config." + MOD_ID + ".soundIndex.description"))
                .setSaveConsumer(ModConfig::setSoundIndex)
                .build());

        return builder.setSavingRunnable(() -> {
            saveConfig(CFG_FILE);
            loadConfig(CFG_FILE);
        }).build();
    }

    public static void setSoundIndex(int input) {
        if (input < 0 || input > ApexMC.soundEventsForPing.size()) {
            input = 0;
        }
        ModConfig.soundIndex = (byte) input;
    }

    public static void setIncludeFluids(boolean includeFluids) {
        ModConfig.includeFluids = includeFluids;
    }

    public static void loadConfig(File file) {
        try {
            Properties cfg = new Properties();
            if (!file.exists()) {
                saveConfig(file);
            }
            cfg.load(new FileInputStream(file));

            includeFluids = Boolean.parseBoolean(cfg.getProperty("includeFluids", "false"));
            soundIndex = (byte) Integer.parseInt(cfg.getProperty("soundIndex", "0"));

            // Re-save so that new properties will appear in old config files
            saveConfig(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(("includeFluids=" + includeFluids + "\n").getBytes());
            fos.write(("soundIndex=" + soundIndex + "\n").getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
