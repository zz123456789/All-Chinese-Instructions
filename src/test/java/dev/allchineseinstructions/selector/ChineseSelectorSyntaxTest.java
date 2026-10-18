package dev.allchineseinstructions.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChineseSelectorSyntaxTest {
    @Test
    void translatesEverySelectorAndPreservesFollowingArguments() {
        Map<String, String> expected = Map.of(
                "选择所有实体", "@e", "选择所有玩家", "@a", "选择最近玩家", "@p",
                "选择随机玩家", "@r", "选择自身", "@s", "选择自己", "@s", "选择执行者", "@s");
        expected.forEach((chinese, vanilla) -> {
            var result = ChineseSelectorSyntax.transform("传送 " + chinese + " ~ ~1 ~", 3);
            assertEquals("传送 " + vanilla + " ~ ~1 ~", result.translated());
            assertTrue(result.changed());
            assertEquals(vanilla, ChineseSelectorSyntax.selectorAt(chinese, 0));
        });
    }

    @Test
    void exposesAllTwentyOneVanillaOptionsAndReadOnlyTables() {
        assertEquals(Set.of("name", "distance", "level", "x", "y", "z", "dx", "dy", "dz",
                        "x_rotation", "y_rotation", "limit", "sort", "gamemode", "team", "type", "tag",
                        "nbt", "scores", "advancements", "predicate"),
                new HashSet<>(ChineseSelectorSyntax.options().values()));
        assertThrows(UnsupportedOperationException.class,
                () -> ChineseSelectorSyntax.aliases().put("新选择器", "@e"));
        assertThrows(UnsupportedOperationException.class,
                () -> ChineseSelectorSyntax.options().put("新选项", "name"));
    }

    @Test
    void translatesEveryOptionInChineseAndVanillaSelectors() {
        ChineseSelectorSyntax.options().forEach((chinese, vanilla) -> {
            assertEquals("@e[" + vanilla + "=test]",
                    translate("选择所有实体[" + chinese + "=test]"));
            assertEquals("@e[" + vanilla + "=test]", translate("@e[" + chinese + "=test]"));
        });
    }

    @Test
    void allowsChineseEnglishOptionsAndValuesToMix() {
        assertEquals("@a[distance=..10,gamemode=!creative,sort=nearest,limit=2]",
                translate("选择所有玩家[distance=..10,游戏模式=!创造,排序=最近,limit=2]"));
        assertEquals("@p[gamemode=spectator,sort=furthest]",
                translate("@p[gamemode=旁观者,sort=最远]"));
    }

    @Test
    void translatesEveryGameModeAndSortOrder() {
        for (String key : new String[]{"gamemode", "sort"}) {
            ChineseSelectorSyntax.values(key).forEach((chinese, vanilla) ->
                    assertEquals("@a[" + key + "=" + vanilla + "]",
                            translate("@a[" + key + "=" + chinese + "]")));
        }
    }

    @Test
    void preservesSpacingAndNegationAroundEnums() {
        assertEquals("@a[ gamemode = ! creative , sort = random ]",
                translate("选择所有玩家[ 游戏模式 = ! 创造 , 排序 = 随机 ]"));
    }

    @Test
    void keepsUserNamesTeamsTagsResourcesAndObjectivesUntranslated() {
        assertEquals("@e[name=创造,team=最近,tag=真,type=示例:选择所有实体,"
                        + "predicate=示例:游戏模式,scores={游戏模式=1..,名称=2,真=3}]",
                translate("选择所有实体[名称=创造,队伍=最近,标签=真,类型=示例:选择所有实体,"
                        + "谓词=示例:游戏模式,分数={游戏模式=1..,名称=2,真=3}]"));
    }

    @Test
    void leavesQuotedStringsAndNestedNbtCompletelyIntact() {
        String data = "{CustomName:'{\"text\":\"名称=创造,选择所有实体\"}',"
                + "Tags:[\"排序=最近\",\"进度={真=假}\"],Nested:{名称:\"游戏模式\"}}";
        assertEquals("@e[name=\"选择所有实体[游戏模式=创造]\",nbt=" + data + ",limit=1]",
                translate("选择所有实体[名称=\"选择所有实体[游戏模式=创造]\",实体数据=" + data + ",数量=1]"));
        assertEquals("@a[gamemode=\"创造\",sort='最近']",
                translate("选择所有玩家[游戏模式=\"创造\",排序='最近']"));
    }

    @Test
    void escapedQuotesDoNotExposeFakeOptionKeys() {
        assertEquals("@e[name=\"a\\\",游戏模式=创造\",limit=2]",
                translate("选择所有实体[名称=\"a\\\",游戏模式=创造\",数量=2]"));
        assertEquals("@e[nbt={text:'a\\'b,数量=3'},limit=2]",
                translate("选择所有实体[实体数据={text:'a\\'b,数量=3'},数量=2]"));
    }

    @Test
    void translatesOnlyAdvancementBooleanValuesIncludingCriteria() {
        assertEquals("@a[advancements={示例:真=true,minecraft:story/root=false,"
                        + "示例:开始={真=true,假=false,\"显示=真\"=true}}]",
                translate("选择所有玩家[进度={示例:真=真,minecraft:story/root=假,"
                        + "示例:开始={真=真,假=假,\"显示=真\"=真}}]"));
        assertEquals("@a[advancements={示例:开始={名字=\"真\"}}]",
                translate("选择所有玩家[进度={示例:开始={名字=\"真\"}}]"));
    }

    @Test
    void recognizesOnlyCompleteAliasWithSelectorBoundary() {
        for (String input : new String[]{"选择所有实体玩家", "选择所有实体:名字", "选择所有", "\"选择所有实体\""}) {
            assertFalse(ChineseSelectorSyntax.isChineseSelectorAt(input, 0), input);
            assertNull(ChineseSelectorSyntax.selectorAt(input, 0), input);
            assertFalse(ChineseSelectorSyntax.transform(input, 0).changed(), input);
        }
        assertTrue(ChineseSelectorSyntax.isChineseSelectorAt("选择所有实体[", 0));
        assertTrue(ChineseSelectorSyntax.isChineseSelectorAt("选择所有实体 ", 0));
        assertTrue(ChineseSelectorSyntax.isChineseSelectorAt("选择所有实体", 0));
        assertFalse(ChineseSelectorSyntax.isChineseSelectorAt("@e", 0));
    }

    @Test
    void rewritesOnlyTheOneRequestedSelector() {
        String input = "传送 选择所有实体[数量=1] 选择所有玩家[游戏模式=创造]";
        var result = ChineseSelectorSyntax.transform(input, 3);
        assertEquals("传送 @e[limit=1] 选择所有玩家[游戏模式=创造]", result.translated());
        assertEquals(input.indexOf(']') + 1, result.originalEnd());
        assertEquals(result.translated().indexOf(']') + 1, result.translatedEnd());
    }

    @Test
    void leavesIncompleteValuesAndNestedStructuresAvailableToParser() {
        assertEquals("@e[gamemode=创", translate("选择所有实体[游戏模式=创"));
        assertEquals("@e[gamemode", translate("选择所有实体[游戏模式"));
        assertEquals("@e[nbt={Tags:[\"数量=1\"", translate("选择所有实体[实体数据={Tags:[\"数量=1\""));
        assertEquals("@e[name=\"未完成,游戏模式=创造", translate("选择所有实体[名称=\"未完成,游戏模式=创造"));
    }

    @Test
    void mapsParserAndSuggestionBoundariesBackToOriginalInput() {
        String original = "执行 实体 选择所有实体[游戏模式=!创造,分数={得分=1..}] 然后 获取数据";
        int start = original.indexOf("选择");
        var rewrite = ChineseSelectorSyntax.transform(original, start);
        String translated = "执行 实体 @e[gamemode=!creative,scores={得分=1..}] 然后 获取数据";
        assertEquals(translated, rewrite.translated());
        for (String token : new String[]{"!", ",", "{", "得分", "1..", "]", "然后", "获取数据"}) {
            int originalCursor = original.indexOf(token);
            int translatedCursor = translated.indexOf(token);
            assertEquals(originalCursor, rewrite.originalCursor(translatedCursor), token);
            assertEquals(translatedCursor, rewrite.translatedCursor(originalCursor), token);
        }
        assertEquals(original.length(), rewrite.originalCursor(translated.length()));
        assertEquals(translated.length(), rewrite.translatedCursor(original.length()));
        assertEquals(start, rewrite.originalCursor(start));
        for (int index = 1; index <= original.length(); index++) {
            assertTrue(rewrite.translatedCursor(index) >= rewrite.translatedCursor(index - 1));
        }
        for (int index = 1; index <= translated.length(); index++) {
            assertTrue(rewrite.originalCursor(index) >= rewrite.originalCursor(index - 1));
        }
    }

    @Test
    void unchangedSelectorsKeepIdentityCursorMapping() {
        String input = "give @p[limit=1] minecraft:stone";
        var rewrite = ChineseSelectorSyntax.transform(input, 5);
        assertFalse(rewrite.changed());
        assertEquals(input, rewrite.translated());
        for (int cursor = 0; cursor <= input.length(); cursor++) {
            assertEquals(cursor, rewrite.originalCursor(cursor));
            assertEquals(cursor, rewrite.translatedCursor(cursor));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> ChineseSelectorSyntax.transform(input, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> ChineseSelectorSyntax.transform(input, input.length() + 1));
    }

    @Test
    void completesPartialSelectorAndTopLevelKeys() {
        assertEquals(new ChineseSelectorSyntax.Completion("selector", 3, "", "选择所有"),
                ChineseSelectorSyntax.completionContext("击杀 选择所有", 3));
        assertEquals(new ChineseSelectorSyntax.Completion("option", 7, "", ""),
                ChineseSelectorSyntax.completionContext("选择所有实体[", 0));
        String input = "选择所有实体[实体数据={Tags:[\"名称=伪造\"]},游";
        assertEquals(new ChineseSelectorSyntax.Completion("option", input.length() - 1, "游", "游"),
                ChineseSelectorSyntax.completionContext(input, 0));
    }

    @Test
    void completesEnumValuesWithoutReplacingNegation() {
        String input = "选择所有玩家[游戏模式=!创";
        assertEquals(new ChineseSelectorSyntax.Completion("value", input.length() - 1, "gamemode", "创"),
                ChineseSelectorSyntax.completionContext(input, 0));
        input = "@e[排序=最";
        assertEquals(new ChineseSelectorSyntax.Completion("value", input.length() - 1, "sort", "最"),
                ChineseSelectorSyntax.completionContext(input, 0));
        input = "@a[进度={示例:开场={获得石头=假";
        assertEquals(new ChineseSelectorSyntax.Completion("value", input.length() - 1, "advancements", "假"),
                ChineseSelectorSyntax.completionContext(input, 0));
    }

    @Test
    void neverOffersSyntaxCompletionsInsideArbitraryData() {
        for (String input : new String[]{"@e[实体数据={游戏模式=", "@e[实体数据={Tags:[\"名称=",
                "@e[名称=\"游戏模式=创", "@e[分数={游", "@e[标签=最", "@e[类型=示例:最",
                "@e[进度={", "@e[进度={示例:开场={真", "@e[进度={示例:开场={名称=\"真",
                "@e[游戏模式=\"创", "@e[排序=最 ", "@e[]", "选择所有实体玩家"}) {
            assertEquals("none", ChineseSelectorSyntax.completionContext(input, 0).kind(), input);
        }
    }

    private static String translate(String selector) {
        return ChineseSelectorSyntax.transform(selector, 0).translated();
    }
}
