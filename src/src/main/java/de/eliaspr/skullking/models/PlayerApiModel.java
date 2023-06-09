package de.eliaspr.skullking.models;

public record PlayerApiModel(String name, boolean nextTurn, Integer predicted, Integer actual, int points,
                             String playedCard) {
}
