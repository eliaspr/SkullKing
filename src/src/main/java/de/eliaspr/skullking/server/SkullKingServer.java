package de.eliaspr.skullking.server;

import de.eliaspr.skullking.game.Player;
import de.eliaspr.skullking.game.SkullKing;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Consumer;

@SpringBootApplication
@RestController
public class SkullKingServer {

    private static final String HEADER_HTML;
    private static final String HOME_HTML;
    private static final String LOBBY_HTML;

    static {
        HEADER_HTML = readFileAsString("layout/header.html");
        HOME_HTML = readFileAsString("layout/home.html");
        LOBBY_HTML = readFileAsString("layout/ingame.html");
    }

    private static byte[] readeFileContents(String file) {
        try {
            InputStream is = SkullKingServer.class.getResourceAsStream("/" + file);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[]{};
    }

    private static String readFileAsString(String file) {
        return new String(readeFileContents(file));
    }

    @GetMapping("/")
    public String index() {
        return makeHtmlPage(stringBuilder -> {
            stringBuilder.append(HOME_HTML);
        });
    }

    @GetMapping("/index.html")
    public String index2() {
        return makeHtmlPage(stringBuilder -> {
            stringBuilder.append(HOME_HTML);
        });
    }

    @GetMapping(value = "/assets/cards/{card}", produces = "image/png")
    public byte[] cardImage(@PathVariable String card) {
        try {
            return readeFileContents("cards/" + card);
        } catch (Exception e) {
        }
        return new byte[]{};
    }

    @GetMapping(value = "/js/{file}", produces = "text/javascript")
    public String javascript(@PathVariable String file) {
        try {
            return readFileAsString("htdocs/js/" + file);
        } catch (Exception e) {
        }
        return "";
    }

    @GetMapping(value = "/css/{file}", produces = "text/css")
    public String stylesheet(@PathVariable String file) {
        try {
            return readFileAsString("htdocs/css/" + file);
        } catch (Exception e) {
        }
        return "";
    }

    @GetMapping(value = "/image/{file}", produces = {"image/jpeg", "image/png"})
    public byte[] image(@PathVariable String file) {
        try {
            return readeFileContents("htdocs/image/" + file);
        } catch (Exception e) {
        }
        return new byte[]{};
    }

    @GetMapping(value = "/{file}.png", produces = {"image/png"})
    public byte[] icon(@PathVariable String file) {
        try {
            return readeFileContents("htdocs/" + file + ".png");
        } catch (Exception e) {
        }
        return new byte[]{};
    }

    @GetMapping(value = "/favicon.ico", produces = {"image/x-icon"})
    public byte[] icon() {
        try {
            return readeFileContents("htdocs/favicon.ico");
        } catch (Exception e) {
        }
        return new byte[]{};
    }

    private String makeHtmlPage(Consumer<StringBuilder> body) {
        StringBuilder html = new StringBuilder();
        html.append(HEADER_HTML);
        html.append("<body>");
        body.accept(html);
        html.append("</body></html>");
        return html.toString();
    }

    @GetMapping(value = "/game/play", params = {"code", "name"})
    public RedirectView play(@RequestParam(value = "code", required = false, defaultValue = "0") int code,
                             @RequestParam("name") String playerName) {
        UUID playerToken = SkullKing.getAccessTokenForPlayer(code, playerName);
        if (playerToken != null) {
            return new RedirectView("/game/lobby?token=" + playerToken.toString());
        } else {
            return new RedirectView("/");
        }
    }

    @GetMapping(value = "/game/lobby", params = {"token"})
    public ModelAndView lobby(ModelMap modelMap, @RequestParam("token") String playerToken) {
        UUID tokenUUID = null;
        try {
            tokenUUID = UUID.fromString(playerToken);
        } catch (IllegalArgumentException ignored) {
        }
        Player player = tokenUUID == null ? null : Player.getPlayer(tokenUUID);
        if (player == null) {
            return new ModelAndView("redirect:/", modelMap);
        }

        return new ModelAndView((model, request, response) -> {
            String buttonHTML = "<button onclick=\"sk_masterButtonPressed()\" type=\"button\" class=\"btn btn-sm btn-success d-none\" id=\"sk-master-button\">Spiel starten</button>";
            response.getWriter().write(makeHtmlPage(stringBuilder -> {
                stringBuilder.append(LOBBY_HTML
                        .replace("{playerToken}", playerToken)
                        .replace("{lobbyCode}", String.valueOf(player.game.gameCode))
                        .replace("{gameMasterButton}", buttonHTML));
            }));
        });
    }

}
