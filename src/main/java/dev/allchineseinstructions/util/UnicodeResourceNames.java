package dev.allchineseinstructions.util;

/**
 * The extra characters accepted in Chinese resource identifiers.
 *
 * <p>Minecraft 1.20.1 validates resource identifiers one UTF-16 character at a
 * time. This deliberately supports BMP Han ideographs only: accepting isolated
 * surrogate characters would also admit malformed Unicode. Supplementary-plane
 * ideographs therefore remain unsupported, as do whitespace and punctuation.</p>
 */
public final class UnicodeResourceNames {
    private UnicodeResourceNames() {
    }

    public static boolean isHan(char character) {
        return character == '\u3007'
                || character >= '\u3400' && character <= '\u4DBF'
                || character >= '\u4E00' && character <= '\u9FFF'
                || character >= '\uF900' && character <= '\uFAFF';
    }

    /**
     * Extends FileUtil's original {@code [-._a-z0-9]+} segment check with Han.
     * The caller retains responsibility for rejecting dot segments; Minecraft's
     * decomposePath and validatePath already reject {@code .} and {@code ..}.
     */
    public static boolean isValidStrictPathSegment(String segment) {
        if (segment.isEmpty()) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            char character = segment.charAt(i);
            if (!(character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '-' || character == '.' || character == '_'
                    || isHan(character))) {
                return false;
            }
        }
        return true;
    }
}
