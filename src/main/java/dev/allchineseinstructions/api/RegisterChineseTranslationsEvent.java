package dev.allchineseinstructions.api;

import net.minecraftforge.eventbus.api.Event;

/** Fired on MinecraftForge.EVENT_BUS each time the server command tree is rebuilt.
 * Register the original commands in RegisterCommandsEvent, then add translations here.
 */
public final class RegisterChineseTranslationsEvent extends Event {
    private final ChineseTranslationRegistry translations;

    public RegisterChineseTranslationsEvent(ChineseTranslationRegistry translations) {
        this.translations = translations;
    }

    public ChineseTranslationRegistry translations() {
        return translations;
    }
}
