package dev.allchineseinstructions.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Extensible translations. Argument keys are registered argument-type IDs. */
public final class ChineseTranslationRegistry {
    private final Map<String, List<String>> commands = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> literals = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> arguments = new LinkedHashMap<>();

    public void command(String original, String... chinese) {
        add(commands, original, List.of(chinese));
    }

    /** Use "*" for a translation shared by all commands. */
    public void literal(String command, String original, String... chinese) {
        add(literals.computeIfAbsent(command, key -> new LinkedHashMap<>()), original, List.of(chinese));
    }

    public void argument(String typeId, String original, String... chinese) {
        add(arguments.computeIfAbsent(typeId, key -> new LinkedHashMap<>()), original, List.of(chinese));
    }

    public List<String> commandAliases(String name) {
        return List.copyOf(commands.getOrDefault(name, List.of()));
    }

    public List<String> literalAliases(String command, String name) {
        Map<String, List<String>> scoped = literals.getOrDefault(command, Map.of());
        return List.copyOf(scoped.containsKey(name) ? scoped.get(name)
                : literals.getOrDefault("*", Map.of()).getOrDefault(name, List.of()));
    }

    public Map<String, List<String>> argumentAliases(String typeId) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        arguments.getOrDefault(typeId, Map.of()).forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    /** Merges a UTF-8 translation document without discarding the built-in aliases. */
    public void load(Reader reader) {
        JsonObject root = new Gson().fromJson(reader, JsonObject.class);
        if (root == null) throw new IllegalArgumentException("Translation document is empty");
        if (root.has("commands")) readAliases(root.getAsJsonObject("commands"), commands);
        readGroups(root, "literals", literals);
        readGroups(root, "arguments", arguments);
    }

    private static void readGroups(JsonObject root, String key,
                                   Map<String, Map<String, List<String>>> target) {
        if (!root.has(key)) return;
        root.getAsJsonObject(key).entrySet().forEach(entry -> readAliases(entry.getValue().getAsJsonObject(),
                target.computeIfAbsent(entry.getKey(), ignored -> new LinkedHashMap<>())));
    }

    private static void readAliases(JsonObject object, Map<String, List<String>> target) {
        object.entrySet().forEach(entry -> {
            List<String> values = new ArrayList<>();
            for (JsonElement value : entry.getValue().getAsJsonArray()) values.add(value.getAsString());
            add(target, entry.getKey(), values);
        });
    }

    private static void add(Map<String, List<String>> target, String original, List<String> aliases) {
        if (original.isBlank()) throw new IllegalArgumentException("Empty original command token");
        List<String> values = target.computeIfAbsent(original, ignored -> new ArrayList<>());
        for (String alias : aliases) {
            if (alias.isBlank() || alias.codePoints().anyMatch(Character::isWhitespace)
                    || alias.startsWith("/") || alias.indexOf('"') >= 0) {
                throw new IllegalArgumentException("Invalid command alias: " + alias);
            }
            if (!alias.equals(original) && !values.contains(alias)) values.add(alias);
        }
    }
}
