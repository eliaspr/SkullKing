package de.eliaspr.skullking.game;

import java.util.ArrayList;
import java.util.Collections;

public enum Card {
    SKULL_KING("skullking", -1, CardColor.SPECIAL, 1),
    PIRATE_BETTY("pirate-betty", -1, CardColor.SPECIAL, 1),
    PIRATE_EMMY("pirate-emmy", -1, CardColor.SPECIAL, 1),
    PIRATE_HARRY("pirate-harry", -1, CardColor.SPECIAL, 1),
    PIRATE_JACK("pirate-jack", -1, CardColor.SPECIAL, 1),
    PIRATE_JOE("pirate-joe", -1, CardColor.SPECIAL, 1),
    MERMAID("mermaid", -1, CardColor.SPECIAL, 2),
    SCARY_MARY("scarymary", -1, CardColor.SPECIAL, 1),
    FLAG("flag", -1, CardColor.SPECIAL, 5),

    BLACK_13("s13", 13, CardColor.BLACK, 1),
    BLACK_12("s12", 12, CardColor.BLACK, 1),
    BLACK_11("s11", 11, CardColor.BLACK, 1),
    BLACK_10("s10", 10, CardColor.BLACK, 1),
    BLACK_9("s9", 9, CardColor.BLACK, 1),
    BLACK_8("s8", 8, CardColor.BLACK, 1),
    BLACK_7("s7", 7, CardColor.BLACK, 1),
    BLACK_6("s6", 6, CardColor.BLACK, 1),
    BLACK_5("s5", 5, CardColor.BLACK, 1),
    BLACK_4("s4", 4, CardColor.BLACK, 1),
    BLACK_3("s3", 3, CardColor.BLACK, 1),
    BLACK_2("s2", 2, CardColor.BLACK, 1),
    BLACK_1("s1", 1, CardColor.BLACK, 1),

    RED_13("r13", 13, CardColor.RED, 1),
    RED_12("r12", 12, CardColor.RED, 1),
    RED_11("r11", 11, CardColor.RED, 1),
    RED_10("r10", 10, CardColor.RED, 1),
    RED_9("r9", 9, CardColor.RED, 1),
    RED_8("r8", 8, CardColor.RED, 1),
    RED_7("r7", 7, CardColor.RED, 1),
    RED_6("r6", 6, CardColor.RED, 1),
    RED_5("r5", 5, CardColor.RED, 1),
    RED_4("r4", 4, CardColor.RED, 1),
    RED_3("r3", 3, CardColor.RED, 1),
    RED_2("r2", 2, CardColor.RED, 1),
    RED_1("r1", 1, CardColor.RED, 1),

    BLUE_13("b13", 13, CardColor.BLUE, 1),
    BLUE_12("b12", 12, CardColor.BLUE, 1),
    BLUE_11("b11", 11, CardColor.BLUE, 1),
    BLUE_10("b10", 10, CardColor.BLUE, 1),
    BLUE_9("b9", 9, CardColor.BLUE, 1),
    BLUE_8("b8", 8, CardColor.BLUE, 1),
    BLUE_7("b7", 7, CardColor.BLUE, 1),
    BLUE_6("b6", 6, CardColor.BLUE, 1),
    BLUE_5("b5", 5, CardColor.BLUE, 1),
    BLUE_4("b4", 4, CardColor.BLUE, 1),
    BLUE_3("b3", 3, CardColor.BLUE, 1),
    BLUE_2("b2", 2, CardColor.BLUE, 1),
    BLUE_1("b1", 1, CardColor.BLUE, 1),

    YELLOW_13("g13", 13, CardColor.YELLOW, 1),
    YELLOW_12("g12", 12, CardColor.YELLOW, 1),
    YELLOW_11("g11", 11, CardColor.YELLOW, 1),
    YELLOW_10("g10", 10, CardColor.YELLOW, 1),
    YELLOW_9("g9", 9, CardColor.YELLOW, 1),
    YELLOW_8("g8", 8, CardColor.YELLOW, 1),
    YELLOW_7("g7", 7, CardColor.YELLOW, 1),
    YELLOW_6("g6", 6, CardColor.YELLOW, 1),
    YELLOW_5("g5", 5, CardColor.YELLOW, 1),
    YELLOW_4("g4", 4, CardColor.YELLOW, 1),
    YELLOW_3("g3", 3, CardColor.YELLOW, 1),
    YELLOW_2("g2", 2, CardColor.YELLOW, 1),
    YELLOW_1("g1", 1, CardColor.YELLOW, 1);

    public final String cardID;
    public final int numericValue;
    public final CardColor cardColor;
    public final int instanceCount;
    public final boolean isPirate;

    Card(String cardID, int numericValue, CardColor cardColor, int instanceCount) {
        this.cardID = cardID;
        this.isPirate = cardID.startsWith("pirate");
        this.numericValue = numericValue;
        this.cardColor = cardColor;
        this.instanceCount = instanceCount;
    }

    public static Card getCard(String cardID) {
        for (var card : values()) {
            if (card.cardID.equalsIgnoreCase(cardID)) {
                return card;
            }
        }
        return null;
    }

    public static ArrayList<Card> getCardDeck(boolean shuffled) {
        var list = new ArrayList<Card>();
        for (var card : Card.values()) {
            for (var i = 0; i < card.instanceCount; i++) {
                list.add(card);
            }
        }
        if (shuffled) {
            Collections.shuffle(list, SkullKing.skullKingRNG);
        }
        return list;
    }

    public boolean isNumeric() {
        return numericValue >= 1;
    }
}
