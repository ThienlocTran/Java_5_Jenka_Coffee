package com.springboot.jenka_coffee.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum BannerEffect {
    FADE("fade"),
    SLIDE("slide"),
    ZOOM("zoom"),
    KENBURNS("kenburns"),
    PUSH("push"),
    CURTAIN("curtain"),
    PARALLAX("parallax"),
    LIQUID("liquid"),
    WAVE("wave"),
    MAGNETIC("magnetic"),
    BLUR("blur"),
    VORTEX("vortex"),
    GLITCH("glitch"),
    CUBE("cube"),
    FLIP("flip"),
    DISSOLVE("dissolve"),
    SCALE_ROTATE("scale-rotate"),
    PRISM("prism"),
    STEAM("steam"),
    SPOTLIGHT("spotlight"),
    ESPRESSO("espresso"),
    NONE("none");

    public static final String DEFAULT_VALUE = "fade";
    private static final Set<String> VALUES = Arrays.stream(values())
            .map(BannerEffect::getValue)
            .collect(Collectors.toUnmodifiableSet());

    private final String value;

    BannerEffect(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String effect) {
        return effect != null && VALUES.contains(effect.trim().toLowerCase());
    }

    public static String normalize(String effect) {
        if (effect == null || effect.isBlank()) {
            return DEFAULT_VALUE;
        }
        String normalized = effect.trim().toLowerCase();
        if (!VALUES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid banner effect: " + effect);
        }
        return normalized;
    }
}
