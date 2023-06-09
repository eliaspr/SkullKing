package de.eliaspr.skullking.game;


import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class SkullKing {

    public static final Random skullKingRNG;
    private static HashMap<UUID, Game> activeGames;

    static {
        skullKingRNG = new Random();
        activeGames = new HashMap<>();
    }

    private static Game createNewGame() {
        int gameCode;
        do {
            gameCode = skullKingRNG.nextInt(900000) + 100000;
        } while (getGame(gameCode) != null);
        Game game = new Game(gameCode);
        System.out.println("Creating new game, code: " + gameCode + " id: " + game.gameUUID);
        activeGames.put(game.gameUUID, game);
        return game;
    }

    private static UUID createNewGameAndAddPlayer(String playerName) {
        Game game = createNewGame();
        return game.addPlayer(playerName);
    }

    public static void closeGame(Game game) {
        System.out.println("Force-Closing game: " + game.gameCode);
        for (Player pl : game.getPlayers()) {
            pl.removeFromGlobalList();
            pl.forceDisconnect();
        }
        activeGames.remove(game.gameUUID);
    }

    public static Game getGame(int gameCode) {
        for (Game game : activeGames.values()) {
            if (game.gameCode == gameCode) {
                return game;
            }
        }
        return null;
    }

    public static UUID getAccessTokenForPlayer(int gameCode, String playerName) {
        Game activeGame = getGame(gameCode);
        if (activeGame == null) {
            if (gameCode == 0) {
                return createNewGameAndAddPlayer(playerName);
            } else {
                return null;
            }
        } else {
            Player player = activeGame.getPlayer(playerName);
            if (player == null) {
                if (activeGame.getPlayerCount() < 6 && !activeGame.isInGame()) {
                    return activeGame.addPlayer(playerName);
                }
            } else {
                System.out.println("Player " + playerName + " is re-joining lobby " + activeGame.gameCode);
                return player.accessToken;
            }
        }
        return null;
    }

}
