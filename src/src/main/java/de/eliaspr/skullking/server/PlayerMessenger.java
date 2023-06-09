package de.eliaspr.skullking.server;

import de.eliaspr.skullking.game.Player;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;

public class PlayerMessenger {

    public static void sendMessageToPlayer(Player player, String message) {
        if (player.webSocketSession == null) {
            return;
        }
        if (!player.webSocketSession.isOpen()) {
            player.game.playerDisconnected(player);
            return;
        }
        try {
            player.webSocketSession.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
