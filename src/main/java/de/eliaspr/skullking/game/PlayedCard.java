package de.eliaspr.skullking.game;

public class PlayedCard {

    public final Player player;
    public final Card card;
    public final ScaryMaryMode scaryMaryMode;
    int bonusPointsReceived = 0;

    public PlayedCard(Player player, Card card, ScaryMaryMode scaryMaryMode) {
        this.player = player;
        this.card = card;
        this.scaryMaryMode = card == Card.SCARY_MARY ? scaryMaryMode : null;
    }

    public boolean isFlag() {
        return card == Card.FLAG || (card == Card.SCARY_MARY && scaryMaryMode == ScaryMaryMode.PLAY_AS_FLAG);
    }

    public boolean isPirate() {
        return card.isPirate || (card == Card.SCARY_MARY && scaryMaryMode == ScaryMaryMode.PLAY_AS_PIRATE);
    }
}
