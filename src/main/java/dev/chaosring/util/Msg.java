package dev.chaosring.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX = "<gray>[<gold>ChaosRing</gold>]</gray> ";

    private Msg() {
    }

    public static Component of(String text) {
        return MM.deserialize(PREFIX + text);
    }

    public static Component raw(String text) {
        return MM.deserialize(text);
    }
}
