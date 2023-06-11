package de.eliaspr.skullking.game;

public enum CardColor {
    SPECIAL(false),
    BLACK(true),
    RED(true),
    BLUE(true),
    YELLOW(true);

    public final boolean isActualColor;

    CardColor(boolean isActualColor) {
        this.isActualColor = isActualColor;
    }
}
