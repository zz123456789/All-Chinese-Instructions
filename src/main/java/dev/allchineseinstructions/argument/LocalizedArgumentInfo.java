package dev.allchineseinstructions.argument;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Serializes both the delegate's own argument template and the alias table. */
public final class LocalizedArgumentInfo
        implements ArgumentTypeInfo<LocalizedArgumentType<?>, LocalizedArgumentInfo.Template> {

    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
        writeDelegate(buffer, template.delegate);
        buffer.writeVarInt(template.aliases.size());
        template.aliases.forEach((canonical, aliases) -> {
            buffer.writeUtf(canonical, LocalizedArgumentType.MAX_TOKEN_LENGTH);
            buffer.writeVarInt(aliases.size());
            aliases.forEach(alias -> buffer.writeUtf(alias, LocalizedArgumentType.MAX_TOKEN_LENGTH));
        });
    }

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
        int typeId = buffer.readVarInt();
        ArgumentTypeInfo<?, ?> delegateInfo = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId(typeId);
        if (delegateInfo == null) {
            throw new IllegalArgumentException("Unknown delegate argument type ID: " + typeId);
        }
        ArgumentTypeInfo.Template<?> delegate = delegateInfo.deserializeFromNetwork(buffer);
        int size = readCount(buffer, LocalizedArgumentType.MAX_TOKENS);
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String canonical = buffer.readUtf(LocalizedArgumentType.MAX_TOKEN_LENGTH);
            int aliasCount = readCount(buffer, LocalizedArgumentType.MAX_ALIASES_PER_TOKEN);
            List<String> values = new ArrayList<>(aliasCount);
            for (int j = 0; j < aliasCount; j++) {
                values.add(buffer.readUtf(LocalizedArgumentType.MAX_TOKEN_LENGTH));
            }
            if (aliases.putIfAbsent(canonical, values) != null) {
                throw new IllegalArgumentException("Duplicate canonical argument token: " + canonical);
            }
        }
        return new Template(delegate, aliases);
    }

    @Override
    public void serializeToJson(Template template, JsonObject json) {
        ResourceLocation id = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(template.delegate.type());
        if (id == null) {
            throw new IllegalArgumentException("Delegate argument type is not registered");
        }
        JsonObject delegateJson = new JsonObject();
        delegateJson.addProperty("parser", id.toString());
        JsonObject properties = new JsonObject();
        writeDelegateJson(template.delegate, properties);
        delegateJson.add("properties", properties);
        json.add("delegate", delegateJson);
        JsonObject aliasesJson = new JsonObject();
        template.aliases.forEach((canonical, aliases) -> {
            JsonArray values = new JsonArray();
            aliases.forEach(values::add);
            aliasesJson.add(canonical, values);
        });
        json.add("aliases", aliasesJson);
    }

    @Override
    public Template unpack(LocalizedArgumentType<?> argument) {
        // byClass/unpack deliberately rejects unregistered delegate classes.
        return new Template(ArgumentTypeInfos.unpack(argument.getDelegate()), argument.getAliases());
    }

    private static int readCount(FriendlyByteBuf buffer, int maximum) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException("Argument alias count outside 0.." + maximum + ": " + size);
        }
        return size;
    }

    private static <A extends ArgumentType<?>> void writeDelegate(FriendlyByteBuf buffer,
                                                                  ArgumentTypeInfo.Template<A> template) {
        writeDelegate(buffer, template.type(), template);
    }

    @SuppressWarnings("unchecked")
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void writeDelegate(
            FriendlyByteBuf buffer, ArgumentTypeInfo<A, T> info, ArgumentTypeInfo.Template<A> template) {
        int typeId = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(info);
        if (typeId < 0) {
            throw new IllegalArgumentException("Delegate argument type is not registered: " + info);
        }
        buffer.writeVarInt(typeId);
        info.serializeToNetwork((T) template, buffer);
    }

    private static <A extends ArgumentType<?>> void writeDelegateJson(ArgumentTypeInfo.Template<A> template,
                                                                      JsonObject json) {
        writeDelegateJson(template.type(), template, json);
    }

    @SuppressWarnings("unchecked")
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void writeDelegateJson(
            ArgumentTypeInfo<A, T> info, ArgumentTypeInfo.Template<A> template, JsonObject json) {
        info.serializeToJson((T) template, json);
    }

    public final class Template implements ArgumentTypeInfo.Template<LocalizedArgumentType<?>> {
        private final ArgumentTypeInfo.Template<?> delegate;
        private final Map<String, List<String>> aliases;

        public Template(ArgumentTypeInfo.Template<?> delegate, Map<String, List<String>> aliases) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.aliases = LocalizedArgumentType.copyAliases(aliases);
        }

        public ArgumentTypeInfo.Template<?> getDelegate() {
            return delegate;
        }

        public Map<String, List<String>> getAliases() {
            return aliases;
        }

        @Override
        public LocalizedArgumentType<?> instantiate(CommandBuildContext context) {
            return new LocalizedArgumentType<>(delegate.instantiate(context), aliases);
        }

        @Override
        public LocalizedArgumentInfo type() {
            return LocalizedArgumentInfo.this;
        }
    }
}
