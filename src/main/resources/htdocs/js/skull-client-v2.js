function sk_confirmLeave() {
    if (confirm("Bist du sicher, dass du das Spiel verlassen willst?")) {
        sk_sendMessage("disconnect:" + sk_getPlayerToken());
        skullKingSocket.close();
        window.location.href = "/";
    }
}

function sk_sendGameCommand(command) {
    sk_sendMessage("game:" + sk_getPlayerToken() + ":" + command)
}

function sk_masterButtonPressed() {
    if (lastGameState === "FINISHED" || lastGameState === "WAITING_FOR_START") {
        sk_sendGameCommand("start");
    } else if (lastGameState === "WAITING_FOR_CONTINUE") {
        sk_sendGameCommand("continue");
    }
}

function sk_getPlayerToken() {
    return document.getElementById("player-token").innerText;
}

let skullKingSocket = null;
let lastPrediction = -1;
let lastGameState = "";

function sk_clientInit() {
    skullKingSocket = new WebSocket(((window.location.protocol === "https:") ? "wss://" : "ws://") + window.location.host + "/ws");

    skullKingSocket.onopen = function (event) {
        console.log("Connected to SkullKing server");
        sk_sendMessage("connect:" + sk_getPlayerToken());
    };

    window.onbeforeunload = function () {
        if (skullKingSocket != null) {
            sk_sendMessage("disconnect:" + sk_getPlayerToken());
            skullKingSocket.close();
        }
    };

    skullKingSocket.onmessage = function (event) {
        let messageJSON = JSON.parse(event.data);
        if(!("aliveAnswer" in messageJSON))
            sk_processBroadcast(messageJSON);
    };

    setInterval(() => {
        sk_sendMessage("alive:" + sk_getPlayerToken());
    }, 10000);
}

function sk_sendMessage(message) {
    if (skullKingSocket != null)
        skullKingSocket.send(message);
}

function sk_processBroadcast(messageJSON) {
    let gameState = "WAITING_FOR_START";
    if ("gameState" in messageJSON)
        gameState = messageJSON["gameState"];
    lastGameState = gameState;

    // 'Game finished' page and player cards share an if-statement because they
    // also share the same root HTML element in which the UI is displayed.
    let hasRemainingCards = false;
    if (gameState === "FINISHED") {
        sk_dom_displayFinishedAndPlayerRanking("players" in messageJSON ? messageJSON["players"] : null);
    } else if ("cards" in messageJSON) {
        hasRemainingCards = messageJSON["cards"].length > 0;
        sk_dom_displayCards(messageJSON["cards"]);
    } else {
        sk_dom_displayCards(null);
    }

    let cardPlayingMode = gameState === "PLAYING_CARDS" || gameState === "WAITING_FOR_CONTINUE";
    if ("players" in messageJSON)
        sk_dom_displayPlayers(messageJSON["players"], cardPlayingMode);
    else
        sk_dom_displayPlayers(null, cardPlayingMode);

    if (gameState === "PREDICTING_WINS" && "roundIndex" in messageJSON) {
        sk_dom_displayPredictUI(messageJSON["roundIndex"]);
    }

    let isGameMaster = false;
    if ("gameMaster" in messageJSON)
        isGameMaster = messageJSON["gameMaster"];

    let startButton = document.getElementById("sk-master-button");
    if (isGameMaster) {
        if(gameState === "WAITING_FOR_CONTINUE") {
            startButton.classList.remove("d-none");
            if (!hasRemainingCards && "roundIndex" in messageJSON && messageJSON["roundIndex"] == 10) {
                startButton.innerHTML = "Spiel beenden";
            } else {
                startButton.innerHTML = "N&auml;chste Runde";
            }
        } else if (gameState === "FINISHED" || gameState === "WAITING_FOR_START") {
            startButton.classList.remove("d-none");
            startButton.innerText = "Spiel starten";
        } else {
            startButton.classList.add("d-none");
        }
    } else {
        startButton.classList.add("d-none");
    }
}

function sk_dom_getCardImg(cardID) {
    return '<img height="200" src="https://skull-king-assets.fra1.digitaloceanspaces.com/cards-hq-png/' + cardID + '.png"/>';
}

function sk_dom_displayFinishedAndPlayerRanking(players) {
    let newHTML = '<div class="p-3"><h5>Das Spiel ist zu Ende!</h5>';
    if (players != null) {
        newHTML += '<div class="mt-2"><div>Ranking:</div>';
        let playersCopy = [...players];
        playersCopy.sort((a, b) => {
            if (b['points'] == a['points']) {
                return a['name'].localeCompare(b['name']);
            } else {
                return b['points'] - a['points'];
            }
        });
        let position = 1;
        playersCopy.forEach(item => {
            newHTML += '<div class="mt-1 ms-2">';
            newHTML += position++;
            newHTML += '. ';
            newHTML += item['name'];
            newHTML += ' (<strong>';
            newHTML += item['points'];
            newHTML += '</strong>)</div>';
        });
        newHTML += "</div>";
    }
    newHTML += '</div>';
    document.getElementById("sk-own-cards").innerHTML = newHTML;
}

function sk_dom_displayCards(cards) {
    let newHTML = "";
    if (cards != null) {
        cards.forEach(function (item) {
            newHTML += '<div class="col-md-auto p-0" style="cursor: pointer;" onclick="sk_playCard(\'' + item + '\')">';
            newHTML += sk_dom_getCardImg(item);
            newHTML += '</div>';
        });
    }
    document.getElementById("sk-own-cards").innerHTML = newHTML;
}

function sk_dom_displayPlayers(players, isCardPlayingMode) {
    let playerHTML = "";
    let currentPlayerName = "";
    if (players != null) {
        players.forEach(function (item) {
            // noinspection EqualityComparisonWithCoercionJS
            let isThisPlayersTurn = (("nextTurn" in item) && item["nextTurn"] == true);
            let x = isThisPlayersTurn ? "strong" : "span";
            if (isThisPlayersTurn)
                currentPlayerName = item["name"];

            playerHTML += '<div class="col lh-sm">';
            playerHTML += '<' + x + ' class="fs-5">' + item["name"] + "</" + x + "><br>";
            playerHTML += 'Punkte: ' + item["points"] + "<br>";
            if (item["predicted"] != null && item["actual"] != null)
                playerHTML += 'Stiche: ' + item["actual"] + " / " + item["predicted"];
            playerHTML += '</div>';
        });
    }
    for (let i = (players == null ? 0 : players.length); i < 6; i++)
        playerHTML += '<div class="col lh-sm"></div>';
    document.getElementById("sk-player-display").innerHTML = playerHTML;

    if (isCardPlayingMode) {
        let playedCardsHTML = "";
        if (players != null) {
            let playedCards = [];
            let index = 0;
            let playedCardCount = 0;
            players.forEach(function (item) {
                let playedCard = null;
                if ("playedCard" in item)
                    playedCard = item["playedCard"];
                playedCards[index++] = playedCard;
                if (playedCard != null)
                    playedCardCount++;
            });

            if (playedCardCount > 0) {
                playedCards.forEach(function (item) {
                    playedCardsHTML += '<div class="col p-0">';
                    if (item != null)
                        playedCardsHTML += sk_dom_getCardImg(item);
                    playedCardsHTML += '</div>';
                });
            } else {
                playedCardsHTML += '<div class="col">' + currentPlayerName + ' muss anfangen!</div>';
            }
        }
        for (let i = (players == null ? 0 : players.length); i < 6; i++)
            playedCardsHTML += '<div class="col p-0"></div>';
        document.getElementById("sk-game-board").innerHTML = playedCardsHTML;
    }
}

function sk_dom_displayPredictUI(round) {
    lastPrediction = -1;
    let predictHTML = '<div class="col p-5">';
    predictHTML += "Wie viele Stiche willst du machen?";
    for (let i = 0; i <= round; i++) {
        predictHTML += '<button onclick="sk_makePrediction(' + i + ')" class="btn btn-outline-secondary ms-4" id="pred-' + i + '">' + i + '</button>';
    }
    predictHTML += '</div>';
    document.getElementById("sk-game-board").innerHTML = predictHTML;
    document.getElementById("sk-current-round-display").innerHTML = 'Aktuelle Runde: ' + round + '&#47;10';
}

function sk_makePrediction(predicted) {
    sk_sendGameCommand("predict" + predicted);

    let button = document.getElementById("pred-" + predicted);
    if (button != null) {
        button.classList.remove("btn-outline-secondary");
        if (!button.classList.contains("btn-secondary"))
            button.classList.add("btn-secondary");
    }

    if (lastPrediction >= 0) {
        button = document.getElementById("pred-" + lastPrediction);
        if (button != null) {
            button.classList.remove("btn-secondary");
            if (!button.classList.contains("btn-outline-secondary"))
                button.classList.add("btn-outline-secondary");
        }
    }
    lastPrediction = predicted;
}

function sk_playCard(cardID) {
    if (cardID === "scarymary") {
        $("#scaryMaryModal").modal();
    } else {
        if(cardID.startsWith("scarymary_"))
            $("#scaryMaryModal").modal("hide");
        sk_sendGameCommand("play" + cardID);
    }
}
