package dev.allchineseinstructions.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnicodeResourceNamesTest {
    @Test
    void acceptsBmpHanIncludingExtensionsAndIdeographicZero() {
        for (char character : "中文函数〇\u3400\u4DBF\u4E00\u9FFF\uF900\uFAFF".toCharArray()) {
            assertTrue(UnicodeResourceNames.isHan(character));
        }
    }

    @Test
    void preservesOriginalAsciiSegmentsAndSupportsChineseFileNames() {
        for (String segment : new String[]{"minecraft", "abc-123_test.json", "中文命名空间",
                "启动.mcfunction", "函数_第一阶段-1", ".", ".."}) {
            assertTrue(UnicodeResourceNames.isValidStrictPathSegment(segment), segment);
        }
    }

    @Test
    void rejectsSeparatorsTraversalSyntaxWhitespaceAndSpecialSymbols() {
        for (String segment : new String[]{"", "foo/bar", "../外部", "..\\外部", "foo\\bar",
                "中文:函数", "a b", "换\n行", "制\t表", "\u0000", "大写A", "Ａ", "？", "。",
                "日本語カナ", "%2e%2e", "\u202E", "\uD840\uDC00", "\uD840", "\uDC00"}) {
            assertFalse(UnicodeResourceNames.isValidStrictPathSegment(segment), segment);
        }
    }

    @Test
    void doesNotExpandResourceIdentifierPunctuationOrAcceptSurrogates() {
        for (char character : new char[]{'a', 'A', '/', '\\', ':', '.', '-', '_', ' ', '\n',
                '\u3006', '\u3008', '\u33FF', '\u4DC0', '\uFAFF' + 1, '\uD840', '\uDC00'}) {
            assertFalse(UnicodeResourceNames.isHan(character));
        }
    }
}
