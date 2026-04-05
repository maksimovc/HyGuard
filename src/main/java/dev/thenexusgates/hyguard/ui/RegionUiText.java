package dev.thenexusgates.hyguard.ui;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.core.region.RegionRole;

import java.util.Locale;

final class RegionUiText {

    private RegionUiText() {
    }

    static String displayRole(RegionRole role) {
        return displayRole(null, role);
    }

    static String displayRole(PlayerRef playerRef, RegionRole role) {
        if (role == null) {
            return pick(playerRef, "Unknown", "Невідомо");
        }
        return switch (role) {
            case OWNER -> pick(playerRef, "Owner", "Власник");
            case CO_OWNER -> pick(playerRef, "Co-owner", "Співвласник");
            case MANAGER -> pick(playerRef, "Manager", "Менеджер");
            case MEMBER -> pick(playerRef, "Member", "Учасник");
            case TRUSTED -> pick(playerRef, "Trusted", "Довірений");
            case VISITOR -> pick(playerRef, "Visitor", "Відвідувач");
        };
    }

    static String roleDescription(RegionRole role) {
        return roleDescription(null, role);
    }

    static String roleDescription(PlayerRef playerRef, RegionRole role) {
        if (role == null) {
            return pick(playerRef, "Role information is unavailable.", "Інформація про роль недоступна.");
        }
        return switch (role) {
            case OWNER -> pick(playerRef,
                    "Full ownership. Cannot be removed or demoted from this screen.",
                    "Повне володіння. Цю роль не можна знизити або видалити з цього меню.");
            case CO_OWNER -> pick(playerRef,
                    "Can help manage region settings and trusted players.",
                    "Може допомагати керувати налаштуваннями регіону та довіреними гравцями.");
            case MANAGER -> pick(playerRef,
                    "Can manage members, flags, and maintenance actions.",
                    "Може керувати учасниками, прапорами та службовими діями.");
            case MEMBER -> pick(playerRef,
                    "Standard region member with normal access.",
                    "Звичайний учасник регіону зі стандартним доступом.");
            case TRUSTED -> pick(playerRef,
                    "Limited trusted access with fewer editing powers.",
                    "Обмежений довірений доступ із меншими правами редагування.");
            case VISITOR -> pick(playerRef,
                    "Basic access role with minimal permissions.",
                    "Базова роль із мінімальними дозволами.");
        };
    }

    static String displayFlag(RegionFlag flag) {
        return displayFlag(null, flag);
    }

    static String displayFlag(PlayerRef playerRef, RegionFlag flag) {
        if (flag == null) {
            return pick(playerRef, "Unknown flag", "Невідомий прапор");
        }
        return switch (flag) {
            case BLOCK_BREAK -> pick(playerRef, "Block break", "Ламання блоків");
            case BLOCK_PLACE -> pick(playerRef, "Block place", "Ставлення блоків");
            case BLOCK_INTERACT -> pick(playerRef, "Block interact", "Взаємодія з блоками");
            case BLOCK_TRAMPLE -> pick(playerRef, "Crop trampling", "Витоптування врожаю");
            case BLOCK_DECAY -> pick(playerRef, "Block decay", "Зникнення блоків");
            case BLOCK_SPREAD -> pick(playerRef, "Block spread", "Поширення блоків");
            case BLOCK_FADE -> pick(playerRef, "Block fade", "Згасання блоків");
            case BLOCK_FORM -> pick(playerRef, "Block form", "Утворення блоків");
            case PVP -> "PvP";
            case PLAYER_DAMAGE -> pick(playerRef, "Player damage", "Шкода гравцю");
            case PLAYER_FALL_DAMAGE -> pick(playerRef, "Fall damage", "Шкода від падіння");
            case PLAYER_HUNGER -> pick(playerRef, "Hunger", "Голод");
            case PLAYER_ITEM_DROP -> pick(playerRef, "Item drop", "Викидання предметів");
            case PLAYER_ITEM_PICKUP -> pick(playerRef, "Item pickup", "Підняття предметів");
            case INTERACT_INVENTORY -> pick(playerRef, "Inventory interaction", "Взаємодія з інвентарем");
            case MOB_DAMAGE_PLAYERS -> pick(playerRef, "Mob damage players", "Моби шкодять гравцям");
            case MOB_SPAWN -> pick(playerRef, "Mob spawn", "Спавн мобів");
            case MOB_SPAWN_HOSTILE -> pick(playerRef, "Hostile mob spawn", "Спавн ворожих мобів");
            case MOB_SPAWN_PASSIVE -> pick(playerRef, "Passive mob spawn", "Спавн мирних мобів");
            case MOB_DAMAGE_BLOCKS -> pick(playerRef, "Mob block damage", "Моби ламають блоки");
            case MOB_GRIEF -> pick(playerRef, "Mob griefing", "Шкідництво мобів");
            case ANIMAL_DAMAGE -> pick(playerRef, "Animal damage", "Шкода тваринам");
            case ENTITY_DAMAGE -> pick(playerRef, "Entity damage", "Шкода сутностям");
            case KNOCKBACK -> pick(playerRef, "Knockback", "Відкидання");
            case FIRE_SPREAD -> pick(playerRef, "Fire spread", "Поширення вогню");
            case TNT -> "TNT";
            case EXPLOSION -> pick(playerRef, "Explosions", "Вибухи");
            case EXPLOSION_BLOCK_DAMAGE -> pick(playerRef, "Explosion block damage", "Вибухи ламають блоки");
            case LIQUID_FLOW -> pick(playerRef, "Liquid flow", "Течія рідин");
            case LIGHTNING -> pick(playerRef, "Lightning", "Блискавка");
            case ENTRY -> pick(playerRef, "Region entry", "Вхід у регіон");
            case EXIT -> pick(playerRef, "Region exit", "Вихід з регіону");
            case ENTRY_DENY_MESSAGE -> pick(playerRef, "Entry denied message", "Повідомлення про заборону входу");
            case EXIT_DENY_MESSAGE -> pick(playerRef, "Exit denied message", "Повідомлення про заборону виходу");
            case GREET_MESSAGE -> pick(playerRef, "Greeting message", "Вітальне повідомлення");
            case FAREWELL_MESSAGE -> pick(playerRef, "Farewell message", "Прощальне повідомлення");
            case ENTRY_PLAYERS -> pick(playerRef, "Player entry", "Вхід гравців");
            case ENTRY_BLACKLIST -> pick(playerRef, "Entry blacklist", "Чорний список входу");
            case COMMAND_BLACKLIST -> pick(playerRef, "Command blacklist", "Чорний список команд");
            case INVINCIBLE -> pick(playerRef, "Invincibility", "Невразливість");
            case GAME_MODE -> pick(playerRef, "Game mode lock", "Фіксація режиму гри");
            case WEATHER_LOCK -> pick(playerRef, "Weather lock", "Фіксація погоди");
            case TIME_LOCK -> pick(playerRef, "Time lock", "Фіксація часу");
            case FLY -> pick(playerRef, "Flight", "Політ");
            case SPAWN_LOCATION -> pick(playerRef, "Spawn location", "Точка спавну");
        };
    }

    static String flagDescription(RegionFlag flag) {
        return flagDescription(null, flag);
    }

    static String flagDescription(PlayerRef playerRef, RegionFlag flag) {
        if (flag == null) {
            return pick(playerRef, "No description available.", "Опис недоступний.");
        }
        return switch (flag) {
            case BLOCK_BREAK -> pick(playerRef, "Controls whether players may break blocks.", "Керує тим, чи можуть гравці ламати блоки.");
            case BLOCK_PLACE -> pick(playerRef, "Controls whether players may place blocks.", "Керує тим, чи можуть гравці ставити блоки.");
            case BLOCK_INTERACT -> pick(playerRef, "Controls interaction with doors, chests, levers, and similar blocks.", "Керує взаємодією з дверима, скринями, важелями та подібними блоками.");
            case BLOCK_TRAMPLE -> pick(playerRef, "Controls whether crops may be trampled.", "Керує тим, чи можна витоптувати врожай.");
            case BLOCK_DECAY -> pick(playerRef, "Controls natural block decay such as leaves disappearing.", "Керує природним зникненням блоків, наприклад листя.");
            case BLOCK_SPREAD -> pick(playerRef, "Controls blocks that spread into neighboring space.", "Керує блоками, що поширюються на сусідні клітинки.");
            case BLOCK_FADE -> pick(playerRef, "Controls blocks that disappear or melt over time.", "Керує блоками, які зникають або тануть з часом.");
            case BLOCK_FORM -> pick(playerRef, "Controls environmental block creation such as ice or snow.", "Керує утворенням блоків довкілля, як-от льоду чи снігу.");
            case PVP -> pick(playerRef, "Controls whether players may damage each other.", "Керує тим, чи можуть гравці завдавати шкоди одне одному.");
            case PLAYER_DAMAGE -> pick(playerRef, "Controls general player damage from other sources.", "Керує загальною шкодою гравцю з інших джерел.");
            case PLAYER_FALL_DAMAGE -> pick(playerRef, "Controls whether fall damage applies.", "Керує тим, чи застосовується шкода від падіння.");
            case PLAYER_HUNGER -> pick(playerRef, "Controls hunger depletion inside the region.", "Керує витратою голоду всередині регіону.");
            case PLAYER_ITEM_DROP -> pick(playerRef, "Controls whether players can drop items.", "Керує тим, чи можуть гравці викидати предмети.");
            case PLAYER_ITEM_PICKUP -> pick(playerRef, "Controls whether players can pick up items.", "Керує тим, чи можуть гравці піднімати предмети.");
            case INTERACT_INVENTORY -> pick(playerRef, "Controls opening or using inventories and storage blocks.", "Керує відкриттям та використанням інвентарів і сховищ.");
            case MOB_DAMAGE_PLAYERS -> pick(playerRef, "Controls mob attacks against players.", "Керує атаками мобів по гравцях.");
            case MOB_SPAWN -> pick(playerRef, "Controls all mob spawning.", "Керує всім спавном мобів.");
            case MOB_SPAWN_HOSTILE -> pick(playerRef, "Controls hostile mob spawning.", "Керує спавном ворожих мобів.");
            case MOB_SPAWN_PASSIVE -> pick(playerRef, "Controls passive mob spawning.", "Керує спавном мирних мобів.");
            case MOB_DAMAGE_BLOCKS -> pick(playerRef, "Controls whether mobs can damage blocks.", "Керує тим, чи можуть моби ламати блоки.");
            case MOB_GRIEF -> pick(playerRef, "Controls mob grief behavior such as terrain damage.", "Керує шкідливою поведінкою мобів, як-от пошкодження ландшафту.");
            case ANIMAL_DAMAGE -> pick(playerRef, "Controls damage against animals and passive creatures.", "Керує шкодою тваринам і мирним істотам.");
            case ENTITY_DAMAGE -> pick(playerRef, "Controls damage against all non-player living entities, including NPCs, hostile mobs, and passive animals.", "Керує шкодою всім живим сутностям, крім гравців, включно з NPC, ворожими та мирними мобами.");
            case KNOCKBACK -> pick(playerRef, "Controls whether damage may apply knockback inside the region.", "Керує тим, чи може шкода спричиняти відкидання всередині регіону.");
            case FIRE_SPREAD -> pick(playerRef, "Controls whether fire spreads between blocks.", "Керує тим, чи поширюється вогонь між блоками.");
            case TNT -> pick(playerRef, "Controls TNT ignition or usage.", "Керує підпалом або використанням TNT.");
            case EXPLOSION -> pick(playerRef, "Controls whether explosions are allowed.", "Керує тим, чи дозволені вибухи.");
            case EXPLOSION_BLOCK_DAMAGE -> pick(playerRef, "Controls whether explosions damage blocks.", "Керує тим, чи вибухи пошкоджують блоки.");
            case LIQUID_FLOW -> pick(playerRef, "Controls water or lava movement.", "Керує рухом води чи лави.");
            case LIGHTNING -> pick(playerRef, "Controls lightning effects inside the region.", "Керує ефектами блискавки всередині регіону.");
            case ENTRY -> pick(playerRef, "Controls whether entities may enter the region.", "Керує тим, чи можуть сутності входити в регіон.");
            case EXIT -> pick(playerRef, "Controls whether entities may leave the region.", "Керує тим, чи можуть сутності виходити з регіону.");
            case ENTRY_DENY_MESSAGE -> pick(playerRef, "Custom message shown when entry is denied.", "Власне повідомлення, яке показується при забороні входу.");
            case EXIT_DENY_MESSAGE -> pick(playerRef, "Custom message shown when exit is denied.", "Власне повідомлення, яке показується при забороні виходу.");
            case GREET_MESSAGE -> pick(playerRef, "Message shown when a player enters the region.", "Повідомлення, яке показується, коли гравець входить у регіон.");
            case FAREWELL_MESSAGE -> pick(playerRef, "Message shown when a player leaves the region.", "Повідомлення, яке показується, коли гравець залишає регіон.");
            case ENTRY_PLAYERS -> pick(playerRef, "Controls whether players may enter the region.", "Керує тим, чи можуть гравці входити в регіон.");
            case ENTRY_BLACKLIST -> pick(playerRef, "Blocks listed players from entering the region, even if entry is otherwise allowed.", "Забороняє зазначеним гравцям входити в регіон, навіть якщо вхід загалом дозволено.");
            case COMMAND_BLACKLIST -> pick(playerRef, "Blocks listed slash commands while players are inside the region.", "Блокує вказані слеш-команди, поки гравці перебувають у регіоні.");
            case INVINCIBLE -> pick(playerRef, "Prevents players inside the region from taking damage.", "Захищає гравців усередині регіону від будь-якої шкоди.");
            case GAME_MODE -> pick(playerRef, "Forces players into a configured game mode while inside.", "Примусово встановлює гравцям налаштований режим гри всередині регіону.");
            case WEATHER_LOCK -> pick(playerRef, "Overrides weather behavior within the region.", "Перевизначає погоду всередині регіону.");
            case TIME_LOCK -> pick(playerRef, "Overrides time behavior within the region.", "Перевизначає час усередині регіону.");
            case FLY -> pick(playerRef, "Controls flight permission.", "Керує дозволом на політ.");
            case SPAWN_LOCATION -> pick(playerRef, "Overrides the location used when spawning in this region.", "Перевизначає точку, яка використовується для спавну в цьому регіоні.");
        };
    }

    static String flagModeHint(RegionFlag flag, RegionFlagValue.Mode mode) {
        return flagModeHint(null, flag, mode);
    }

    static String flagModeHint(PlayerRef playerRef, RegionFlag flag, RegionFlagValue.Mode mode) {
        String englishEffect = switch (flag) {
            case BLOCK_BREAK -> "players may break blocks";
            case BLOCK_PLACE -> "players may place blocks";
            case BLOCK_INTERACT -> "players may interact with blocks";
            case BLOCK_TRAMPLE -> "crop trampling is permitted";
            case BLOCK_DECAY -> "natural decay runs normally";
            case BLOCK_SPREAD -> "spreading blocks may expand";
            case BLOCK_FADE -> "fading blocks may disappear";
            case BLOCK_FORM -> "new environmental blocks may form";
            case PVP -> "players may fight each other";
            case PLAYER_DAMAGE -> "general player damage is allowed";
            case PLAYER_FALL_DAMAGE -> "fall damage applies";
            case PLAYER_HUNGER -> "hunger drains normally";
            case PLAYER_ITEM_DROP -> "players may drop items";
            case PLAYER_ITEM_PICKUP -> "players may pick up items";
            case INTERACT_INVENTORY -> "inventories can be opened and used";
            case MOB_DAMAGE_PLAYERS -> "mobs may damage players";
            case MOB_SPAWN -> "all mobs may spawn";
            case MOB_SPAWN_HOSTILE -> "hostile mobs may spawn";
            case MOB_SPAWN_PASSIVE -> "passive mobs may spawn";
            case MOB_DAMAGE_BLOCKS -> "mobs may damage blocks";
            case MOB_GRIEF -> "mobs may grief the world";
            case ANIMAL_DAMAGE -> "animals may be damaged";
            case ENTITY_DAMAGE -> "non-player living entities may be damaged";
            case KNOCKBACK -> "damage may apply knockback";
            case FIRE_SPREAD -> "fire may spread";
            case TNT -> "TNT use is permitted";
            case EXPLOSION -> "explosions are permitted";
            case EXPLOSION_BLOCK_DAMAGE -> "explosions may damage blocks";
            case LIQUID_FLOW -> "liquids may flow";
            case LIGHTNING -> "lightning effects are enabled";
            case ENTRY -> "entry is permitted";
            case EXIT -> "exit is permitted";
            case ENTRY_PLAYERS -> "players may enter";
            case ENTRY_BLACKLIST -> "listed players are blocked from entering";
            case COMMAND_BLACKLIST -> "listed slash commands are blocked";
            case INVINCIBLE -> "players become invincible";
            case WEATHER_LOCK -> "the region weather override applies";
            case TIME_LOCK -> "the region time override applies";
            case FLY -> "flight is permitted";
            case SPAWN_LOCATION -> "the region spawn override applies";
            default -> flagDescription(flag).toLowerCase(Locale.ROOT);
        };
        String ukrainianEffect = switch (flag) {
            case BLOCK_BREAK -> "гравці можуть ламати блоки";
            case BLOCK_PLACE -> "гравці можуть ставити блоки";
            case BLOCK_INTERACT -> "гравці можуть взаємодіяти з блоками";
            case BLOCK_TRAMPLE -> "дозволено витоптувати врожай";
            case BLOCK_DECAY -> "природне зникнення працює нормально";
            case BLOCK_SPREAD -> "блоки, що поширюються, можуть рости";
            case BLOCK_FADE -> "блоки, що згасають, можуть зникати";
            case BLOCK_FORM -> "можуть утворюватися нові природні блоки";
            case PVP -> "гравці можуть битися між собою";
            case PLAYER_DAMAGE -> "загальна шкода гравцю дозволена";
            case PLAYER_FALL_DAMAGE -> "шкода від падіння застосовується";
            case PLAYER_HUNGER -> "голод витрачається нормально";
            case PLAYER_ITEM_DROP -> "гравці можуть викидати предмети";
            case PLAYER_ITEM_PICKUP -> "гравці можуть піднімати предмети";
            case INTERACT_INVENTORY -> "інвентарі можна відкривати й використовувати";
            case MOB_DAMAGE_PLAYERS -> "моби можуть шкодити гравцям";
            case MOB_SPAWN -> "усі моби можуть спавнитись";
            case MOB_SPAWN_HOSTILE -> "ворожі моби можуть спавнитись";
            case MOB_SPAWN_PASSIVE -> "мирні моби можуть спавнитись";
            case MOB_DAMAGE_BLOCKS -> "моби можуть ламати блоки";
            case MOB_GRIEF -> "моби можуть псувати світ";
            case ANIMAL_DAMAGE -> "тваринам можна завдавати шкоди";
            case ENTITY_DAMAGE -> "живим сутностям, крім гравців, можна завдавати шкоди";
            case KNOCKBACK -> "шкода може спричиняти відкидання";
            case FIRE_SPREAD -> "вогонь може поширюватися";
            case TNT -> "використання TNT дозволено";
            case EXPLOSION -> "вибухи дозволені";
            case EXPLOSION_BLOCK_DAMAGE -> "вибухи можуть ламати блоки";
            case LIQUID_FLOW -> "рідини можуть текти";
            case LIGHTNING -> "ефекти блискавки увімкнені";
            case ENTRY -> "вхід дозволений";
            case EXIT -> "вихід дозволений";
            case ENTRY_PLAYERS -> "гравці можуть входити";
            case ENTRY_BLACKLIST -> "вказаним гравцям заборонено входити";
            case COMMAND_BLACKLIST -> "вказані слеш-команди заблоковані";
            case INVINCIBLE -> "гравці стають невразливими";
            case WEATHER_LOCK -> "застосовується перевизначення погоди";
            case TIME_LOCK -> "застосовується перевизначення часу";
            case FLY -> "політ дозволений";
            case SPAWN_LOCATION -> "застосовується перевизначення точки спавну";
            default -> flagDescription(playerRef, flag).toLowerCase(Locale.ROOT);
        };
        return switch (mode) {
            case ALLOW -> pick(playerRef,
                    "Allowed: " + capitalize(englishEffect) + ".",
                    "Дозволено: " + capitalize(ukrainianEffect) + ".");
            case DENY -> pick(playerRef,
                    "Denied: " + capitalize(englishEffect) + ".",
                    "Заборонено: " + capitalize(ukrainianEffect) + ".");
            case INHERIT -> pick(playerRef,
                    "Inherited: Uses the parent or global rule for this setting.",
                    "Успадковано: використовується правило батьківського або глобального регіону.");
            case ALLOW_MEMBERS -> pick(playerRef,
                    "Members only: Members and higher roles are allowed while visitors are restricted.",
                    "Лише для учасників: учасники та вищі ролі мають доступ, а відвідувачі обмежені.");
            case ALLOW_TRUSTED -> pick(playerRef,
                    "Trusted only: Trusted players and higher roles are allowed while visitors are restricted.",
                    "Лише для довірених: довірені гравці та вищі ролі мають доступ, а відвідувачі обмежені.");
        };
    }

    static String displayMode(RegionFlagValue.Mode mode) {
        return displayMode(null, mode);
    }

    static String displayMode(PlayerRef playerRef, RegionFlagValue.Mode mode) {
        if (mode == null) {
            return pick(playerRef, "unknown", "невідомо");
        }
        return switch (mode) {
            case ALLOW -> pick(playerRef, "allow", "allow");
            case DENY -> pick(playerRef, "deny", "deny");
            case INHERIT -> pick(playerRef, "inherit", "inherit");
            case ALLOW_MEMBERS -> pick(playerRef, "members only", "тільки учасники");
            case ALLOW_TRUSTED -> pick(playerRef, "trusted only", "тільки довірені");
        };
    }

    static String textFlagPlaceholder(RegionFlag flag) {
        return textFlagPlaceholder(null, flag);
    }

    static String textFlagPlaceholder(PlayerRef playerRef, RegionFlag flag) {
        if (flag == null) {
            return "";
        }
        return switch (flag) {
            case GREET_MESSAGE -> pick(playerRef, "Welcome to the region.", "Ласкаво просимо до регіону.");
            case FAREWELL_MESSAGE -> pick(playerRef, "See you next time.", "До зустрічі.");
            case ENTRY_DENY_MESSAGE -> pick(playerRef, "You cannot enter this region.", "Ви не можете увійти в цей регіон.");
            case EXIT_DENY_MESSAGE -> pick(playerRef, "You cannot leave this region right now.", "Ви зараз не можете покинути цей регіон.");
            case GAME_MODE -> pick(playerRef, "Adventure or Creative", "Adventure або Creative");
            case WEATHER_LOCK -> "0, 1, 2 or clear/rain/storm";
            case TIME_LOCK -> "HH or HH:MM";
            case COMMAND_BLACKLIST -> "spawn, home, warp shop, tpa*";
            default -> "";
        };
    }

    static String textFlagHelper(RegionFlag flag) {
        return textFlagHelper(null, flag);
    }

    static String textFlagHelper(PlayerRef playerRef, RegionFlag flag) {
        if (flag == null) {
            return "";
        }
        return switch (flag) {
            case GREET_MESSAGE -> pick(playerRef, "Shown to players when they cross into the region.", "Показується гравцям, коли вони входять у регіон.");
            case FAREWELL_MESSAGE -> pick(playerRef, "Shown to players when they leave the region.", "Показується гравцям, коли вони залишають регіон.");
            case ENTRY_DENY_MESSAGE -> pick(playerRef, "Explains why entry is blocked.", "Пояснює, чому вхід заблоковано.");
            case EXIT_DENY_MESSAGE -> pick(playerRef, "Explains why exit is blocked.", "Пояснює, чому вихід заблоковано.");
            case GAME_MODE -> pick(playerRef, "Pick a valid enforced mode instead of typing a raw enum value.", "Оберіть коректний режим примусу замість введення сирого enum-значення.");
            case WEATHER_LOCK -> pick(playerRef, "Set a fixed weather index for players in the region. Aliases clear, rain, and storm are accepted.", "Задає фіксований індекс погоди для гравців у регіоні. Підтримуються псевдоніми clear, rain і storm.");
            case TIME_LOCK -> pick(playerRef, "Set a fixed region clock using a 24-hour value such as 6, 18, or 18:30.", "Задає фіксований час регіону у 24-годинному форматі, наприклад 6, 18 або 18:30.");
            case COMMAND_BLACKLIST -> pick(playerRef, "Enter comma-separated slash commands or subcommands to block. Entries match the command root, and a trailing * matches prefixes.", "Введіть через кому слеш-команди або підкоманди для блокування. Записи звіряються з коренем команди, а зірочка в кінці блокує префікси.");
            default -> "";
        };
    }

    static String displayConfiguredGameMode(PlayerRef playerRef, String modeName) {
        if (modeName == null || modeName.isBlank()) {
            return pick(playerRef, "Unknown", "Невідомо");
        }
        return switch (modeName.trim().toLowerCase(Locale.ROOT)) {
            case "adventure" -> pick(playerRef, "Adventure", "Adventure");
            case "creative" -> pick(playerRef, "Creative", "Creative");
            default -> modeName;
        };
    }

    private static String pick(PlayerRef playerRef, String english, String ukrainian) {
        return UiText.choose(playerRef, english, ukrainian);
    }

    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}