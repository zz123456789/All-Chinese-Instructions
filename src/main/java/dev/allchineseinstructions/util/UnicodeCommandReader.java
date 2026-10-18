package dev.allchineseinstructions.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/** Unicode-aware command token reading without transforming Brigadier's bootstrap-loaded classes. */
public final class UnicodeCommandReader {
    private UnicodeCommandReader() {
    }

    public static String readUnquotedString(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && isAllowedInUnquotedString(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    public static String readString(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && StringReader.isQuotedStringStart(reader.peek())) {
            return reader.readString();
        }
        return readUnquotedString(reader);
    }

    public static boolean isAllowedInUnquotedString(char character) {
        return StringReader.isAllowedInUnquotedString(character) || UnicodeResourceNames.isHan(character);
    }
}
