package de.eliaspr.skullking.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eliaspr.skullking.models.GameStateApiModel;
import de.eliaspr.skullking.models.PlayerApiModel;
import de.eliaspr.skullking.server.PlayerMessenger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Game {

    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public final UUID gameUUID;
    public final int gameCode;
    private final ArrayList<Player> playerList = new ArrayList<>();
    private final ArrayList<PlayedCard> playedCards = new ArrayList<>();
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
        logger.info(
                "Adding player \"" + playerName + "\" to game " + gameCode + " (token: " + player.accessToken + ")");
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
        ArrayList<Card> cards = Card.getCardDeck(true);
        for (Player player : playerList) {
            player.currentCards.clear();
        }
        for (int i = 1; i <= roundIndex; i++) {
            for (Player player : playerList) {
                int j = cards.size() - 1;
                Card next = cards.get(j);
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

    public void notifyPlayCard(Player player, Card card, ScaryMaryMode scaryMaryMode) {
        if (gameState == GameState.PLAYING_CARDS && player == nextPlayer) {
            boolean doesPlayerHaveCard = false;
            for (Card playerCard : player.currentCards) {
                if (playerCard == card) {
                    doesPlayerHaveCard = true;
                    break;
                }
            }
            if (!doesPlayerHaveCard) {
                return;
            }

            PlayedCard playedCard = new PlayedCard(player, card, scaryMaryMode);
            playCard(playedCard);
        }
    }

    private void playCard(PlayedCard playedCard) {
        boolean isPlayAllowed;
        Card card = playedCard.card;
        if (card == Card.FLAG
                || card.isPirate
                || card == Card.SKULL_KING
                || card == Card.SCARY_MARY
                || card == Card.MERMAID) {
            isPlayAllowed = true;
        } else {
            CardColor forcedColor = null;
            for (PlayedCard previous : playedCards) {
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
                } else if (playedCard.card.cardColor == CardColor.BLACK) {
                    isPlayAllowed = true;
                } else {
                    boolean doesPlayerHaveColor = false;
                    for (Card playerCard : playedCard.player.currentCards) {
                        if (playerCard.cardColor == forcedColor) {
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
            if (player1.currentCards.get(i) == playedCard.card) {
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
            PlayedCard winningCard = getWinningCard();
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

    private PlayedCard getWinningCard() {
        if (playedCards.isEmpty()) {
            return null;
        }

        PlayedCard skullKing = wasCardPlayed(Card.SKULL_KING);
        if (skullKing != null) {
            PlayedCard firstMermaid = wasCardPlayed(Card.MERMAID);
            if (firstMermaid != null) {
                firstMermaid.bonusPointsReceived += 50;
                return firstMermaid;
            } else {
                int pirateCount = 0;
                for (PlayedCard playedCard : playedCards) {
                    if (playedCard.isPirate()) {
                        pirateCount++;
                    }
                }
                skullKing.bonusPointsReceived = pirateCount * 30;
                return skullKing;
            }
        }

        PlayedCard winningCard = null;
        CardColor acceptedColor = null;
        for (PlayedCard next : playedCards) {
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

                    if (next.card.cardColor == CardColor.BLACK) {
                        if (winningCard.card.cardColor == CardColor.BLACK) {
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
                        if (acceptedColor == null || next.card.cardColor == CardColor.BLACK) {
                            acceptedColor = next.card.cardColor;
                        }
                    }
                }
            }
        }
        return winningCard;
    }

    private PlayedCard wasCardPlayed(Card cardType) {
        for (PlayedCard playedCard : playedCards) {
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
        var gameStateString = gameState.toString();
        var playerCount = playerList.size();
        var playerApiModels = playerList.stream().map(this::getPlayerApiModel).toArray(PlayerApiModel[]::new);

        // copy to temp list, because sendMessageToPlayer deletes disconnected
        // players which would cause a ConcurrentModificationException
        ArrayList<Player> tempPlayers = new ArrayList<>(playerList);
        for (Player player : tempPlayers) {
            var playerCards =
                    player.currentCards.stream().map(x -> x.cardID).toArray(String[]::new);
            var apiModel = new GameStateApiModel(
                    gameStateString, roundIndex, playerCount, playerApiModels, playerCards, player == gameMaster);

            try {
                String jsonString = jsonMapper.writeValueAsString(apiModel);
                PlayerMessenger.sendMessageToPlayer(player, jsonString);
            } catch (JsonProcessingException e) {
                logger.error("Could not convert GameStateApiModel to json string", e);
            }
        }
    }

    private PlayerApiModel getPlayerApiModel(Player player) {
        PlayedCard playedCard = null;
        for (PlayedCard pc : playedCards) {
            if (pc.player == player) {
                playedCard = pc;
                break;
            }
        }
        var playedCardString = playedCard == null
                ? null
                : playedCard.card == Card.SCARY_MARY
                        ? (playedCard.isPirate() ? "scarymary-pirate" : "scarymary-flag")
                        : playedCard.card.cardID;

        var showPredictedActual = gameState == GameState.PLAYING_CARDS || gameState == GameState.WAITING_FOR_CONTINUE;

        return new PlayerApiModel(
                player.name,
                nextPlayer == player,
                showPredictedActual ? player.predictedWins : null,
                showPredictedActual ? player.actualWins : null,
                player.pointTotal,
                playedCardString);
    }

    public void playerDisconnected(Player player) {
        logger.info("Player '" + player.name + "' was disconnected");
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
