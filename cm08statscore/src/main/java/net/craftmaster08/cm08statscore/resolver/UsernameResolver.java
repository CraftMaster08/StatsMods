package net.craftmaster08.cm08statscore.resolver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.craftmaster08.cm08statscore.cache.UsernameCache;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public interface UsernameResolver {
    Logger LOGGER = LogManager.getLogger(UsernameResolver.class);

    static String resolve(MinecraftServer server, UUID uuid, String uuidString) {
        // Try server profile cache
        String username = server.getProfileCache()
                .get(uuid)
                .map(profile -> {
                    String name = profile.getName();
                    if (name == null || name.isEmpty()) {
                        LOGGER.warn("Profile cache returned null/empty name for UUID: {}", uuidString);
                        return null;
                    }
                    return name;
                })
                .orElse(null);

        if (username != null) {
            return username;
        }

        // Try custom cache
        UsernameCache cache = UsernameCache.getInstance(server);
        username = cache.getUsername(uuid);
        if (username != null) {
            return username;
        }

        // Fallback to Mojang API
        try {
            HttpClient client = HttpClient.newHttpClient();
            String uuidNoHyphens = uuidString.replace("-", "");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/user/profile/" + uuidNoHyphens))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                username = json.get("name").getAsString();
                if (username != null && !username.isEmpty()) {
                    cache.storeUsername(uuid, username);
                    return username;
                }
            } else {
                LOGGER.warn("Mojang API request failed for UUID: {}, status: {}", uuidString, response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error querying Mojang API for UUID: {}", uuidString, e);
        }

        // Final fallback
        return "Unknown_" + uuidString.substring(0, 8);
    }
}