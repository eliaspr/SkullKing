package de.eliaspr.skullking.server;

import de.eliaspr.skullking.game.Player;
import de.eliaspr.skullking.game.SkullKing;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@SpringBootApplication
@RestController
public class SkullKingServer {
    
    
    
    private static final Logger logger = LoggerFactory.getLogger(SkullKingServer.class);

    private static String HEADER_HTML;
    private static String HOME_HTML;
    private static String LOBBY_HTML;

    static {
        try {
            HEADER_HTML = StaticFileHandler.readFileAsString("layout/header.html");
            HOME_HTML = StaticFileHandler.readFileAsString("layout/home.html");
            LOBBY_HTML = StaticFileHandler.readFileAsString("layout/ingame.html");
        } catch (Exception e) {
            logger.error("Could not load static html files.", e);
            System.exit(1);
        }
    }

    @GetMapping(value = {"", "/", "/index.html"})
    public String getIndexPage() {
        return generateHtmlPage(stringBuilder -> stringBuilder.append(HOME_HTML));
    }

    @GetMapping(value = "/js/{file}", produces = "text/javascript")
    public ResponseEntity<Object> getJavaScriptFile(@PathVariable String file) {
        return getFileContentAsResponse("htdocs/js/" + file, false);
    }

    @GetMapping(value = "/css/{file}", produces = "text/css")
    public ResponseEntity<Object> getCssFile(@PathVariable String file) {
        return getFileContentAsResponse("htdocs/css/" + file, false);
    }

    @GetMapping(
            value = "/{file}.png",
            produces = {"image/png"})
    public ResponseEntity<Object> getPngIcon(@PathVariable String file) {
        return getFileContentAsResponse("htdocs/" + file + ".png", true);
    }

    @GetMapping(
            value = "/favicon.ico",
            produces = {"image/x-icon"})
    public ResponseEntity<Object> getIcoIcon() {
        return getFileContentAsResponse("htdocs/favicon.ico", true);
    }

    @GetMapping(
            value = "/game/play",
            params = {"code", "name"})
    public RedirectView redirectToGameLobby(
            @RequestParam(value = "code", required = false, defaultValue = "0") int code,
            @RequestParam("name") String playerName) {
        var playerToken = SkullKing.getAccessTokenForPlayer(code, playerName);
        if (playerToken != null) {
            return new RedirectView("/game/lobby?token=" + playerToken);
        } else {
            return new RedirectView("/");
        }
    }

    @GetMapping(
            value = "/game/lobby",
            params = {"token"})
    public ModelAndView getGameLobbyPage(ModelMap modelMap, @RequestParam("token") String playerToken) {
        UUID tokenUUID = null;
        try {
            tokenUUID = UUID.fromString(playerToken);
        } catch (IllegalArgumentException ignored) {
        }
        var player = tokenUUID == null ? null : Player.getPlayer(tokenUUID);
        if (player == null) {
            return new ModelAndView("redirect:/", modelMap);
        }

        return new ModelAndView((model, request, response) -> {
            var buttonHTML =
                    "<button onclick=\"sk_masterButtonPressed()\" type=\"button\" class=\"btn btn-sm btn-success d-none\" id=\"sk-master-button\">Spiel starten</button>";
            response.getWriter()
                    .write(generateHtmlPage(stringBuilder -> stringBuilder.append(LOBBY_HTML
                            .replace("{playerToken}", playerToken)
                            .replace("{lobbyCode}", String.valueOf(player.game.gameCode))
                            .replace("{gameMasterButton}", buttonHTML))));
        });
    }

    private ResponseEntity<Object> getFileContentAsResponse(String path, boolean isBinary) {
        try {
            Object content;
            if (isBinary) {
                content = StaticFileHandler.readeFileContents(path);
            } else {
                content = StaticFileHandler.readFileAsString(path);
            }
            return ResponseEntity.ok(content);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            logger.warn("Failed to load static file from classpath.", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generateHtmlPage(Consumer<StringBuilder> body) {
        var html = new StringBuilder();
        html.append(HEADER_HTML);
        html.append("<body>");
        body.accept(html);
        html.append("</body></html>");
        return html.toString();
    }
}
