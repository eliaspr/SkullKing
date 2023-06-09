package de.eliaspr.skullking.game;

import de.eliaspr.json.JSONArray;
import de.eliaspr.json.JSONObject;
import de.eliaspr.json.JSONValue;
import de.eliaspr.json.JSONWriter;
import de.eliaspr.skullking.server.PlayerMessenger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;

public class Game {

    private static final JSONWriter jsonWriter = new JSONWriter().enableMinifyJSON();

    public final UUID gameUUID;
    public final int gameCode;
    private final ArrayList<Player> playerList = new ArrayList<>();
    private final ArrayList<Card.PlayedCard> playedCards = new ArrayList<>();
    private Player gameMaster = null;
    private GameState gameState = GameState.WAITING_FOR_START;
    private int roundIndex;
    private int playedTricks;
    private Player nextPlayer;
    private Runnable continueAction;

    public Game(int gameCode) {
        this.gameCode = gameCode;
        gameUUID = UUID.randomUUID();
    }

    public int getPlayerCount() {
        return playerList.size();
    }

    public int getConnectedPlayerCount() {
        return (int) playerList.stream().filter(Player::isConnected).count();
    }

    public Player getPlayer(String name) {
        for (Player pl : playerList) {
            if (pl.name.equals(name)) {
                return pl;
            }
        }
        return null;
    }

    public Player getPlayer(UUID playerToken) {
        for (Player player : playerList) {
            if (player.accessToken == playerToken) {
                return player;
            }
        }
        return null;
    }

    public Collection<Player> getPlayers() {
        return playerList;
    }

    public UUID addPlayer(String playerName) {
        if (getPlayerCount() == 6 || getPlayer(playerName) != null) {
            return null;
        }
        Player player = new Player(playerName, this);
        player.timeJoined = System.currentTimeMillis();
        System.out.println("Adding player \"" + playerName + "\" to game " + gameCode + " (token: " + player.accessToken + ")");
        playerList.add(player);
        if (playerList.size() == 1 || gameMaster == null) {
            gameMaster = player;
        }
        return player.accessToken;
    }

    public void removePlayer(Player player) {
        if (gameState == GameState.WAITING_FOR_START || gameState == GameState.FINISHED) {
            player.forceDisconnect();
            playerList.remove(player);
            checkGameMaster();
            player.removeFromGlobalList();
            broadcastGameState();
        }

        if (getConnectedPlayerCount() == 0) {
            SkullKing.closeGame(this);
        }
    }

    private void checkGameMaster() {
        if (!playerList.contains(gameMaster)) {
            if (playerList.isEmpty()) {
                gameMaster = null;
            } else {
                gameMaster = playerList.get(0);
            }
        }
    }

    public Player getGameMaster() {
        return gameMaster;
    }

    public boolean isInGame() {
        return !(gameState == GameState.WAITING_FOR_START || gameState == GameState.FINISHED);
    }

    public void resetLobbyToNewGame() {
        if (playerList.size() < 2) {
            return;
        }
        playerList.sort(Comparator.comparingLong(Player::getTimeJoined));
        for (Player player : playerList) {
            player.pointTotal = 0;
        }
        nextPlayer = playerList.get(SkullKing.skullKingRNG.nextInt(playerList.size()));
        prepareRound(1);
        broadcastGameState();
    }

    private void prepareRound(int roundIndex) {
        gameState = GameState.PREDICTING_WINS;

        this.roundIndex = roundIndex;
        this.playedTricks = 0;
        playedCards.clear();

        for (Player player : playerList) {
            player.predictedWins = -1;
            player.actualWins = 0;
            player.roundBonusPoints = 0;
        }

        shuffleCards();
    }

    private void shuffleCards() {
        ArrayList<Card.CardInstance> cards = Card.getCardDeck(true);
        for (Player player : playerList) {
            player.currentCards.clear();
        }
        for (int i = 1; i <= roundIndex; i++) {
            for (Player player : playerList) {
                int j = cards.size() - 1;
                Card.CardInstance next = cards.get(j);
                cards.remove(j);
                player.currentCards.add(next);
            }
        }
    }

    public void notifyPredictedWins(Player player, int numPredicted) {
        if (gameState == GameState.PREDICTING_WINS) {
            player.predictedWins = numPredicted;

            int playersFinished = 0;
            for (Player pl : playerList) {
                if (pl.predictedWins >= 0) {
                    playersFinished++;
                }
            }
            if (playersFinished == playerList.size()) {
                gameState = GameState.PLAYING_CARDS;
                broadcastGameState();
            }
        }
    }

    public void notifyPlayCard(Player player, Card card, Card.ScaryMaryMode scaryMaryMode) {
        if (gameState == GameState.PLAYING_CARDS && player == nextPlayer) {
            boolean doesPlayerHaveCard = false;
            for (Card.CardInstance playerCard : player.currentCards) {
                if (playerCard.card == card) {
                    doesPlayerHaveCard = true;
                    break;
                }
            }
            if (!doesPlayerHaveCard) {
                return;
            }

            Card.PlayedCard playedCard = new Card.PlayedCard(player, card, scaryMaryMode);
            playCard(playedCard);
        }
    }

    private void playCard(Card.PlayedCard playedCard) {
        boolean isPlayAllowed;
        Card card = playedCard.card;
        if (card == Card.FLAG || card.isPirate || card == Card.SKULL_KING || card == Card.SCARY_MARY || card == Card.MERMAID) {
            isPlayAllowed = true;
        } else {
            Card.CardColor forcedColor = null;
            for (Card.PlayedCard previous : playedCards) {
                if (previous.isFlag()) {
                    continue;
                }
                forcedColor = previous.card.cardColor;
                break;
            }
            if (forcedColor == null || !forcedColor.isActualColor) {
                isPlayAllowed = true;
            } else {
                if (playedCard.card.cardColor == forcedColor) {
                    isPlayAllowed = true;
                } else if (playedCard.card.cardColor == Card.CardColor.BLACK) {
                    isPlayAllowed = true;
                } else {
                    boolean doesPlayerHaveColor = false;
                    for (Card.CardInstance playerCard : playedCard.player.currentCards) {
                        if (playerCard.card.cardColor == forcedColor) {
                            doesPlayerHaveColor = true;
                            break;
                        }
                    }
                    isPlayAllowed = !doesPlayerHaveColor;
                }
            }
        }
        if (!isPlayAllowed) {
            return;
        }

        playedCards.add(playedCard);

        Player player1 = playedCard.player;
        for (int i = 0; i < player1.currentCards.size(); i++) {
            if (player1.currentCards.get(i).card == playedCard.card) {
                player1.currentCards.remove(i);
                break;
            }
        }

        int playerIndex = playerList.indexOf(nextPlayer);
        playerIndex++;
        if (playerIndex >= playerList.size()) {
            playerIndex = 0;
        }
        nextPlayer = playerList.get(playerIndex);

        if (playedCards.size() == playerList.size()) {
            Card.PlayedCard winningCard = getWinningCard();
            if (winningCard == null) {
                winningCard = playedCards.get(0);
            }

            Player winningPlayer = winningCard.player;
            winningPlayer.actualWins++;
            winningPlayer.roundBonusPoints += winningCard.bonusPointsReceived;
            nextPlayer = winningPlayer;

            if ((playedTricks + 1) >= roundIndex) {
                // this was the last trick in this round

                for (Player player : playerList) {
                    int predicted = player.predictedWins;
                    int actual = player.actualWins;

                    if (predicted == 0) {
                        if (actual == 0) {
                            player.pointTotal += roundIndex * 10;
                        } else {
                            player.pointTotal -= roundIndex * 10;
                        }
                    } else {
                        if (predicted == actual) {
                            player.pointTotal += 20 * predicted;
                            player.pointTotal += player.roundBonusPoints;
                        } else {
                            int difference = actual > predicted ? actual - predicted : predicted - actual;
                            player.pointTotal -= 10 * difference;
                        }
                    }

                    player.roundBonusPoints = 0;
                }

                waitForContinue(() -> {
                    playedCards.clear();
                    if (roundIndex == 10) {
                        onGameFinished();
                    } else {
                        prepareRound(roundIndex + 1);
                        broadcastGameState();
                    }
                });
            } else {
                waitForContinue(() -> {
                    playedTricks++;
                    playedCards.clear();
                    gameState = GameState.PLAYING_CARDS;
                    broadcastGameState();
                });
            }
        } else {
            broadcastGameState();
        }
    }

    private void onGameFinished() {
        this.gameState = GameState.FINISHED;
        broadcastGameState();
    }

    private Card.PlayedCard getWinningCard() {
        if (playedCards.isEmpty()) {
            return null;
        }

        Card.PlayedCard skullKing = wasCardPlayed(Card.SKULL_KING);
        if (skullKing != null) {
            Card.PlayedCard firstMermaid = wasCardPlayed(Card.MERMAID);
            if (firstMermaid != null) {
                firstMermaid.bonusPointsReceived += 50;
                return firstMermaid;
            } else {
                int pirateCount = 0;
                for (Card.PlayedCard playedCard : playedCards) {
                    if (playedCard.isPirate()) {
                        pirateCount++;
                    }
                }
                skullKing.bonusPointsReceived = pirateCount * 30;
                return skullKing;
            }
        }

        Card.PlayedCard winningCard = null;
        Card.CardColor acceptedColor = null;
        for (Card.PlayedCard next : playedCards) {
            if (winningCard == null) {
                winningCard = next;
                if (!winningCard.isFlag()) {
                    acceptedColor = winningCard.card.cardColor;
                }
            } else {
                // a flag in 2nd or later position can never win
                if (next.isFlag()) {
                    continue;
                }

                // when the best card is already a pirate, no other card can win
                // because the cases where a skull was played are previously handled
                if (winningCard.isPirate()) {
                    continue;
                }

                // when a pirate is played, and the highest previous card is not a pirate,
                // the next pirate will always win
                if (next.isPirate()) {
                    winningCard = next;
                    continue;
                }

                // when a mermaid is played, it will always win, as long as the previous
                // highest card is a numeric card
                if (next.card == Card.MERMAID) {
                    if (winningCard.card.isNumeric()) {
                        winningCard = next;
                        continue;
                    }
                }

                // all numeric cards will always lose against a mermaid/pirate
                if (winningCard.card != Card.MERMAID && !winningCard.isPirate()) {
                    boolean doesNextCardWin;

                    if (next.card.cardColor == Card.CardColor.BLACK) {
                        if (winningCard.card.cardColor == Card.CardColor.BLACK) {
                            doesNextCardWin = next.card.numericValue > winningCard.card.numericValue;
                        } else {
                            doesNextCardWin = true;
                        }
                    } else {
                        if (acceptedColor == null) {
                            doesNextCardWin = true;
                        } else if (next.card.cardColor == acceptedColor) {
                            doesNextCardWin = next.card.numericValue > winningCard.card.numericValue;
                        } else {
                            doesNextCardWin = false;
                        }
                    }

                    if (doesNextCardWin) {
                        winningCard = next;
                        if (acceptedColor == null || next.card.cardColor == Card.CardColor.BLACK) {
                            acceptedColor = next.card.cardColor;
                        }
                    }
                }
            }
        }
        return winningCard;
    }

    private Card.PlayedCard wasCardPlayed(Card cardType) {
        for (Card.PlayedCard playedCard : playedCards) {
            if (playedCard.card == cardType) {
                return playedCard;
            }
        }
        return null;
    }

    private void waitForContinue(Runnable exec) {
        gameState = GameState.WAITING_FOR_CONTINUE;
        continueAction = exec;
        broadcastGameState();
    }

    public void requestGameContinue() {
        // rely on continueAction to change the gameState
        if (gameState == GameState.WAITING_FOR_CONTINUE) {
            Runnable old = continueAction;
            if (continueAction != null) {
                continueAction.run();
            }
            // remember the old action, in case that action changes the
            // continueAction to a new one. in that case we would not
            // want to reset it directly afterward
            if (continueAction == old) {
                continueAction = null;
            }
        }
    }

    public void broadcastGameState() {
        JSONObject state = new JSONObject();
        state.putString("gameState", gameState.toString());
        state.putInteger("roundIndex", roundIndex);
        state.putInteger("playerCount", playerList.size());
        JSONArray players = new JSONArray(playerList.size());
        int index = 0;
        for (Player player : playerList) {
            players.setObject(index++, getPlayerJSON(player));
        }
        state.putArray("players", players);

        // copy to temp list, because sendMessageToPlayer deletes disconnected
        // players which would cause a ConcurrentModificationException
        ArrayList<Player> tempPlayers = new ArrayList<>(playerList);
        for (Player player : tempPlayers) {
            JSONArray playerCards = getPlayerCardsJSON(player);
            state.putArray("cards", playerCards);
            state.putBoolean("gameMaster", player == gameMaster);
            String jsonString = jsonWriter.writeJSON(state);
            PlayerMessenger.sendMessageToPlayer(player, jsonString);
        }
    }

    private JSONArray getPlayerCardsJSON(Player player) {
        JSONArray array = new JSONArray(player.currentCards.size());
        for (int i = 0; i < array.length(); i++) {
            array.setString(i, player.currentCards.get(i).card.cardID);
        }
        return array;
    }

    private JSONObject getPlayerJSON(Player player) {
        JSONObject json = new JSONObject();
        json.putString("name", player.name);
        json.putBoolean("nextTurn", nextPlayer == player);
        if (gameState == GameState.PLAYING_CARDS || gameState == GameState.WAITING_FOR_CONTINUE) {
            json.putInteger("predicted", player.predictedWins);
            json.putInteger("actual", player.actualWins);
        }
        json.putInteger("points", player.pointTotal);

        Card.PlayedCard playedCard = null;
        for (Card.PlayedCard pc : playedCards) {
            if (pc.player == player) {
                playedCard = pc;
                break;
            }
        }
        if (playedCard == null) {
            json.putValue("playedCard", JSONValue.fromNull());
        } else {
            if (playedCard.card == Card.SCARY_MARY) {
                json.putString("playedCard", playedCard.isPirate() ? "scarymary-pirate" : "scarymary-flag");
            } else {
                json.putString("playedCard", playedCard.card.cardID);
            }
        }

        return json;
    }

    public void playerDisconnected(Player player) {
        System.out.println("Player '" + player.name + "' was disconnected");
        removePlayer(player);
    }

    void requestGameStart() {
        if (!isInGame()) {
            resetLobbyToNewGame();
        }
    }

    private enum GameState {
        WAITING_FOR_START,
        PREDICTING_WINS,
        WAITING_FOR_CONTINUE,
        PLAYING_CARDS,
        FINISHED
    }

}
