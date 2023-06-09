package de.eliaspr.skullking.game;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Player {

    private static final HashMap<UUID, Player> activePlayers = new HashMap<>();
    public final String name;
    public final Game game;
    public final UUID accessToken;
    public WebSocketSession webSocketSession;
    long timeJoined;
    ArrayList<Card.CardInstance> currentCards = new ArrayList<>();
    int pointTotal = 0;
    int predictedWins = -1;
    int actualWins = 0;
    int roundBonusPoints = 0;
    public Player(String name, Game game) {
        this.name = name;
        this.game = game;
        accessToken = UUID.randomUUID();
        activePlayers.put(accessToken, this);
    }

    public static Player getPlayer(UUID accessToken) {
        return activePlayers.get(accessToken);
    }

    public long getTimeJoined() {
        return timeJoined;
    }

    public void requestGameStart() {
        if (game.getGameMaster() == this) {
            game.requestGameStart();
        }
    }

    public void requestGameContinue() {
        if (game.getGameMaster() == this) {
            game.requestGameContinue();
        }
    }

    public void notifyPredictedWins(int numPredicted) {
        if (numPredicted < 0) {
            numPredicted = 0;
        }
        if (numPredicted > 10) {
            numPredicted = 10;
        }
        game.notifyPredictedWins(this, numPredicted);
    }

    public void notifyPlayCard(Card card, Card.ScaryMaryMode scaryMaryMode) {
        game.notifyPlayCard(this, card, scaryMaryMode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Player player = (Player) o;

        return Objects.equals(accessToken, player.accessToken);
    }

    public void removeFromGlobalList() {
        activePlayers.remove(accessToken);
    }

    public boolean isConnected() {
        return webSocketSession != null && webSocketSession.isOpen();
    }

    public void forceDisconnect() {
        if (isConnected()) {
            try {
                webSocketSession.close();
            } catch (Exception ignored) {
            }
        }
    }
}
