package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Server-side catalogue of IMBA feedback messages.
 *
 * Restrictions themselves are never disabled here: only their HUD text can be
 * hidden. Unknown vanilla/mod messages are not touched.
 */
public final class MessageSettingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_messages.json");

    public static final class Rule {
        public final String id;
        public final String label;
        public final String example;
        public final boolean defaultVisible;
        private final List<String> needles;

        private Rule(String id, String label, String example, boolean defaultVisible, String... needles) {
            this.id = id;
            this.label = label;
            this.example = example;
            this.defaultVisible = defaultVisible;
            this.needles = List.of(needles);
        }

        boolean matches(String plainLower) {
            for (String needle : needles) {
                if (plainLower.contains(needle)) return true;
            }
            return false;
        }
    }

    private static final List<Rule> RULES = List.of(
            new Rule("restriction.item_frame", "Запрет предметов в рамках",
                    "Класть предметы в рамки можно только в креативе", false,
                    "класть предметы в рамки можно только в креативе"),
            new Rule("restriction.interactive_entity", "Запрет интерактивных сущностей",
                    "Эта интерактивная сущность запрещена настройками карты", false,
                    "эта интерактивная сущность запрещена настройками карты"),
            new Rule("restriction.interactive_block", "Запрет интерактивных блоков",
                    "Этот интерактивный блок запрещён настройками карты", false,
                    "этот интерактивный блок запрещён настройками карты"),
            new Rule("restriction.container", "Запрет сундуков во время игры",
                    "Во время игры участникам нельзя открывать сундуки", false,
                    "во время игры участникам нельзя открывать сундуки"),
            new Rule("mask.requirements", "Ошибки установки маски",
                    "Сначала надень модель / недостаточно места / нельзя зафиксироваться", true,
                    "сначала надень модель", "нельзя зафиксироваться", "здесь нельзя зафиксироваться",
                    "в конечной точке маскировки недостаточно места"),
            new Rule("mask.status", "Вход и выход из маскировки",
                    "Вы замаскировались / Вы вышли из маскировки-статуи", false,
                    "вы замаскировались", "вы вышли из маскировки"),
            new Rule("seeker.penalty", "Штрафы искателя",
                    "Минус сердце: ... / Вы потеряли все сердца", true,
                    "минус сердце", "вы потеряли все сердца"),
            new Rule("game.autopause", "Автопауза",
                    "Автопауза: ключевой игрок отключился", true,
                    "автопауза:"),
            new Rule("game.flow", "Состояние раунда",
                    "Игра поставлена на паузу / Игра продолжена / Раунд сброшен", true,
                    "игра поставлена на паузу", "игра продолжена", "раунд полностью сброшен",
                    "тестовый таймер запущен", "тестовый таймер остановлен"),
            new Rule("admin.settings", "Сообщения настройщиков",
                    "Настройки сохранены / сброшены / некорректное значение", true,
                    "настройки панели", "хитбоксы стрелок", "настройка блоков", "точка сохранена",
                    "автопозиция", "настройки локации", "не удалось сохранить маску локации",
                    "некорректная маска локации", "для настройки", "для телепортации через камеру"),
            new Rule("admin.photos", "Фото локаций",
                    "Фотография сохранена / удалена / отклонена", true,
                    "фотография отклонена", "фотография локации", "все фотографии удалены")
    );

    private static final Map<String, Boolean> VISIBLE = new LinkedHashMap<>();

    private MessageSettingsConfig() {
    }

    public static void load() {
        resetDefaults(false);
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data != null && data.visible != null) {
                for (Rule rule : RULES) {
                    Boolean value = data.visible.get(rule.id);
                    if (value != null) VISIBLE.put(rule.id, value);
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить фильтр сообщений: " + e.getMessage());
        }
    }

    public static List<Rule> rules() {
        return RULES;
    }

    public static boolean isVisible(String id) {
        Rule rule = rule(id);
        return VISIBLE.getOrDefault(id, rule == null || rule.defaultVisible);
    }

    public static Map<String, Boolean> snapshot() {
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        for (Rule rule : RULES) result.put(rule.id, isVisible(rule.id));
        return result;
    }

    public static void setVisible(String id, boolean visible) {
        if (rule(id) == null) return;
        VISIBLE.put(id, visible);
        save();
    }

    public static boolean shouldShow(Text message) {
        if (message == null) return true;
        String plain = message.getString().toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (rule.matches(plain)) return isVisible(rule.id);
        }
        return true;
    }

    public static void resetDefaults() {
        resetDefaults(true);
    }

    private static void resetDefaults(boolean save) {
        VISIBLE.clear();
        for (Rule rule : RULES) VISIBLE.put(rule.id, rule.defaultVisible);
        if (save) save();
    }

    private static Rule rule(String id) {
        if (id == null) return null;
        for (Rule rule : RULES) if (rule.id.equals(id)) return rule;
        return null;
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(snapshot())));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить фильтр сообщений: " + e.getMessage());
        }
    }

    private static final class Data {
        Map<String, Boolean> visible = new LinkedHashMap<>();

        Data() {
        }

        Data(Map<String, Boolean> visible) {
            this.visible = visible;
        }
    }
}
