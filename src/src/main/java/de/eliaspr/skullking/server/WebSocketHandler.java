package de.eliaspr.skullking.server;

import de.eliaspr.skullking.game.Card;
import de.eliaspr.skullking.game.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.UUID;

public class WebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String msg = message.getPayload();
        if (msg.startsWith("connect")) {
            Player player = getPlayerFromToken(msg);
            if (player != null) {
                logger.info("Player '" + player.name + "' connected with token " + player.accessToken);
                player.webSocketSession = session;
                player.game.broadcastGameState();
            }
        } else if (msg.startsWith("disconnect")) {
            Player player = getPlayerFromToken(msg);
            if (player != null) {
                logger.info("Player '" + player.name + "' disconnecting");
                player.webSocketSession = null;
                player.game.removePlayer(player);
            }
        } else if (msg.startsWith("game")) {
            String[] spl = msg.split(":");
            if (spl.length > 2) {
                UUID token = null;
                try {
                    token = UUID.fromString(spl[1]);
                } catch (IllegalArgumentException ignored) {
                }
                if (token != null) {
                    Player player = Player.getPlayer(token);
                    if (player != null) {
                        processPlayerCommand(player, spl[2]);
                    }
                }
            }
        } else if (msg.startsWith("alive")) {
            try {
                // send meaningless answer to client
                session.sendMessage(new TextMessage("{\"aliveAnswer\":" + System.currentTimeMillis() + "}"));
            } catch (IOException ignored) {
            }
        }
    }

    private void processPlayerCommand(Player player, String cmd) {
        if (cmd.equals("start")) {
            player.requestGameStart();
        } else if (cmd.equals("continue")) {
            player.requestGameContinue();
        } else if (cmd.startsWith("predict")) {
            String prediction = cmd.substring(7);
            try {
                int predictionNum = Integer.parseInt(prediction);
                player.notifyPredictedWins(predictionNum);
            } catch (NumberFormatException ignored) {
            }
        } else if (cmd.startsWith("play")) {
            String cardID = cmd.substring(4);
            if (cardID.equalsIgnoreCase("scarymary_flag")) {
                player.notifyPlayCard(Card.SCARY_MARY, Card.ScaryMaryMode.PLAY_AS_FLAG);
            } else if (cardID.equalsIgnoreCase("scarymary_pirate")) {
                player.notifyPlayCard(Card.SCARY_MARY, Card.ScaryMaryMode.PLAY_AS_PIRATE);
            } else {
                Card card = Card.getCard(cardID);
                if (card != null) {
                    player.notifyPlayCard(card, null);
                }
            }
        }
    }

    private Player getPlayerFromToken(String message) {
        UUID token = getPlayerToken(message);
        return token == null ? null : Player.getPlayer(token);
    }

    private UUID getPlayerToken(String message) {
        int i = message.indexOf(':');
        if (i > 0 && i < message.length() - 1) {
            String tokenStr = message.substring(i + 1);
            try {
                return UUID.fromString(tokenStr);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

}
