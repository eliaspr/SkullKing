package de.eliaspr.skullking.models;

public record GameStateApiModel(
        String gameState,
        int roundIndex,
        int playerCount,
        PlayerApiModel[] players,
        String[] cards,
        boolean gameMaster) {}
