package com.asilvorcarp;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import static com.asilvorcarp.ApexMC.MOD_ID;

public class ModMenuConfig implements ModMenuApi, ConfigScreenFactory<Screen> {
    // format: 0xAARRGGBB
    public static final int INFO_COLOR = 0xFFeb9d39;
    public static boolean includeFluids = false;
    // TODO be able to config this
    public static float iconSize = 1f;
    // the num of pings save for each player (work only in client side)
    public static int pingNumEach;
    // TODO be able to config this
    public static SoundEvent DEFAULT_SOUND_EVENT = ApexMC.PING_LOCATION_EVENT;
    // TODO config for this
    // 0 means never, only work at your client side
    public static long secondsToVanish = 0;

    public static void setIncludeFluids(boolean includeFluids) {
        ModMenuConfig.includeFluids = includeFluids;
    }

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
                .setSaveConsumer(ModMenuConfig::setIncludeFluids)
                .build());

        // Delay Buffer
        general.addEntry(entryBuilder.startIntField(Text.translatable("config." + MOD_ID + ".delayBuffer"), AutoRunMod.getDelayBuffer())
                .setDefaultValue(20)
                .setTooltip(Text.translatable("config." + MOD_ID + ".delayBuffer.description"))
                .setSaveConsumer(AutoRunMod::setDelayBuffer)
                .build());

        return builder.setSavingRunnable(() -> {
            AutoRunMod.saveConfig(AutoRunMod.CFG_FILE);
            AutoRunMod.loadConfig(AutoRunMod.CFG_FILE);
        }).build();
    }
}
