# AI Agent Prompt — Hytale Territory Protection Mod (HyGuard)

## Контекст середовища

Ти знаходишся в директорії де розробляються моди для гри Hytale. Структура:
- `MyMods/` — готові, робочі моди (вивчи їх структуру, архітектуру, патерни — вони є основою)
- `Hytale-API/` — вихідні файли гри, API класи, інтерфейси
- `Hytale-Mod-Template/` — чистий шаблон мода
- `Hytale-Modding-Tutorials/` — документація від спільноти

**Перед будь-яким кодом** виконай повне дослідження:
1. Вивчи всі моди в `MyMods/` — архітектура, package-структура, як реєструються команди, events, GUI, збереження даних
2. Вивчи `Hytale-API/` — знайди всі класи пов'язані з: виділенням регіонів, creative tools, selection tools, BlockPos/Vec3, player permissions, entity interaction, NBT/data storage, GUI/screen API, particle/visual effects, chunk система
3. Вивчи `Hytale-Modding-Tutorials/` — особливо все про existing selection system в creative mode
4. Вивчи `Hytale-Mod-Template/` — build system, gradle конфіг, entrypoint

**Тільки після повного дослідження** — приступай до реалізації.

---

## Мета

Створити мод **HyGuard** — повноцінну систему захисту (приватів) територій для Hytale.  
Аналог поєднання **WorldEdit wand** + **WorldGuard** + **LWC** з Minecraft, але адаптований під Hytale API та ігрову механіку.

---

## Фаза 1 — Дослідження та планування

### 1.1 Аудит існуючої Selection System в Hytale
- Знайди в `Hytale-API/` всі класи/інтерфейси пов'язані з виділенням блоків у Creative Mode
- Визнач: які події генеруються при виділенні, які класи відповідають за візуалізацію рамки виділення, які структури даних зберігають координати кутів
- Визнач чи можна **перевикористати** цю систему або потрібно реалізувати власну
- Запиши висновки у `src/main/resources/RESEARCH_NOTES.md`

### 1.2 Аудит суміжних систем API
Знайди та задокументуй:
- Система прав гравців (permissions/roles якщо є)
- Система збереження даних (NBT, JSON, власний serialization)
- Chunk API та block access API
- GUI/Screen API (як відкривати кастомні екрани)
- Particle та visual effects API
- Command registration API
- Event system (block break, block place, entity move, player interact, etc.)
- Item API (custom items, enchantments)

---

## Фаза 2 — Архітектура мода

### Структура пакетів (адаптуй під конвенції з MyMods):
```
com.yourname.hyguard/
├── HyGuardMod.java              # Entrypoint, ініціалізація
├── core/
│   ├── region/
│   │   ├── Region.java              # Модель регіону
│   │   ├── RegionFlag.java          # Enum всіх прапорів
│   │   ├── RegionFlagValue.java     # Значення прапора (ALLOW/DENY/INHERIT + параметри)
│   │   ├── RegionMember.java        # Учасник регіону (UUID + роль)
│   │   ├── RegionRole.java          # Enum ролей: OWNER, MANAGER, MEMBER, TRUSTED, VISITOR
│   │   ├── RegionPriority.java      # Пріоритет при перетині регіонів
│   │   └── RegionShape.java         # CUBOID (основна), CYLINDER, SPHERE (якщо API дозволяє)
│   ├── selection/
│   │   ├── SelectionSession.java    # Сесія виділення для конкретного гравця
│   │   ├── SelectionPoint.java      # Один кут виділення
│   │   ├── SelectionVisualizer.java # Відображення виділення через viewer-only debug shapes
│   │   └── WandItem.java            # Кастомна "wand" палиця
│   ├── protection/
│   │   ├── ProtectionEngine.java    # Головний рушій перевірки дозволів
│   │   ├── ProtectionQuery.java     # Запит: гравець + дія + позиція → результат
│   │   └── BypassHandler.java       # Логіка bypass для адмінів
│   └── overlap/
│       ├── OverlapResolver.java     # Вирішення конфліктів при перетині регіонів
│       └── RegionHierarchy.java     # Ієрархія: глобальний → батьківський → дочірній
├── storage/
│   ├── RegionRepository.java        # Interface для збереження/завантаження
│   ├── JsonRegionRepository.java    # JSON реалізація (файлова)
│   ├── RegionSerializer.java        # Серіалізація/десеріалізація
│   ├── RegionCache.java             # In-memory кеш завантажених регіонів
│   └── MigrationManager.java       # Міграція формату даних між версіями мода
├── command/
│   ├── GuardCommand.java            # /hg або /guard — головна команда
│   ├── RegionCreateCommand.java     # /hg create <name>
│   ├── RegionDeleteCommand.java     # /hg delete <name>
│   ├── RegionInfoCommand.java       # /hg info [name]
│   ├── RegionListCommand.java       # /hg list [page]
│   ├── RegionFlagCommand.java       # /hg flag <region> <flag> <value>
│   ├── RegionMemberCommand.java     # /hg member add/remove/role
│   ├── RegionTeleportCommand.java   # /hg tp <region>
│   ├── RegionRedefineCommand.java   # /hg redefine <region> — перевизначити межі
│   ├── RegionSelectCommand.java     # /hg select <region> — показати виділення
│   ├── RegionExpandCommand.java     # /hg expand <vert/N/S/E/W/UP/DOWN> <amt>
│   ├── RegionContractCommand.java   # /hg contract ...
│   ├── RegionBypassCommand.java     # /hg bypass — toggle bypass mode
│   ├── WandCommand.java             # /hg wand — отримати паличку
│   └── HelpCommand.java             # /hg help [command]
├── gui/
│   ├── RegionMainScreen.java        # Головне GUI регіону (вкладки)
│   ├── FlagEditorScreen.java        # GUI редактора прапорів
│   ├── MemberManagerScreen.java     # GUI керування учасниками
│   ├── RegionListScreen.java        # GUI список регіонів
│   ├── RegionInfoScreen.java        # GUI інформація про регіон
│   └── widgets/
│       ├── FlagToggleWidget.java    # Компонент: тогл прапора
│       ├── MemberEntryWidget.java   # Компонент: рядок учасника
│       └── ConfirmDialogWidget.java # Компонент: підтвердження дії
├── event/
│   ├── BlockProtectionListener.java  # Захист блоків
│   ├── EntityProtectionListener.java # Захист від PvP, моб шкоди
│   ├── PlayerMoveListener.java       # Enter/Exit events регіону
│   ├── InteractionListener.java      # Захист використання предметів
│   ├── ExplosionListener.java        # Захист від вибухів
│   ├── WandInteractionListener.java  # Обробка кліків паличкою
│   └── RegionEventPublisher.java    # Кастомні події: RegionEnterEvent, RegionExitEvent
├── visual/
│   ├── SelectionVisualizer.java    # Territory-frame preview для поточного виділення
│   ├── EnterExitMessageRenderer.java # Повідомлення при вході/виході
│   └── VisualScheduler.java         # Shared scheduler для selection visual redraw
├── config/
│   ├── HyGuardConfig.java       # Головна конфігурація
│   ├── DefaultFlagsConfig.java      # Дефолтні значення прапорів
│   ├── LimitsConfig.java            # Ліміти: max регіонів на гравця, max розмір
│   └── MessageConfig.java          # Кастомізовані повідомлення (i18n)
└── util/
    ├── BlockPosUtils.java           # Утиліти роботи з координатами
    ├── PlayerUtils.java             # Утиліти роботи з гравцями
    ├── TextFormatter.java           # Форматування текстових повідомлень
    ├── GeometryUtils.java           # Перетин кубоїдів, overlap detection
    └── PermissionUtils.java         # Перевірка прав
```

---

## Фаза 3 — Модель даних

### Region.java — повна модель:
```
- String id (UUID)
- String name (унікальна назва, регістронезалежна)
- UUID ownerUuid
- String ownerName (кешований для відображення)
- RegionShape shape
- BlockPos min, max (для CUBOID)
- String worldId / dimensionId
- Map<RegionFlag, RegionFlagValue> flags
- Map<UUID, RegionMember> members
- int priority (0 = lowest, вищий = перекриває нижчий)
- String parentRegionId (nullable — для ієрархії)
- List<String> childRegionIds
- long createdAt (unix timestamp)
- long lastModifiedAt
- String createdByName
- boolean isGlobal (спеціальний глобальний регіон "__global__")
- BlockPos spawnPoint (nullable — кастомний спавн при вході)
- Map<String, String> metadata (розширюване поле для майбутніх фіч)
```

### RegionFlag enum — повний список прапорів:

**Блоки:**
- `BLOCK_BREAK` — ламати блоки (non-members)
- `BLOCK_PLACE` — ставити блоки
- `BLOCK_INTERACT` — взаємодіяти з блоками (скрині, двері, тощо)
- `BLOCK_TRAMPLE` — витоптування (farmland)
- `BLOCK_DECAY` — природне руйнування (листя, тощо)
- `BLOCK_SPREAD` — поширення (вогонь, рідини)
- `BLOCK_FADE` — зникнення (сніг, лід)
- `BLOCK_FORM` — формування (сніг, лід)

**Гравці:**
- `PVP` — пошкодження між гравцями
- `PLAYER_DAMAGE` — будь-яка шкода гравцеві
- `PLAYER_FALL_DAMAGE` — шкода від падіння
- `PLAYER_HUNGER` — голод
- `PLAYER_ITEM_DROP` — викидання предметів
- `PLAYER_ITEM_PICKUP` — підбирання предметів
- `INTERACT_INVENTORY` — відкривання інвентарів (скрині)

**Мобі:**
- `MOB_DAMAGE_PLAYERS` — мобі атакують гравців
- `MOB_SPAWN` — природний спавн мобів
- `MOB_SPAWN_HOSTILE` — спавн ворожих мобів
- `MOB_SPAWN_PASSIVE` — спавн мирних мобів
- `MOB_DAMAGE_BLOCKS` — мобі руйнують блоки (creeper, ендерман)
- `MOB_GRIEF` — загальний гріфінг від мобів
- `ANIMAL_DAMAGE` — пошкодження тварин гравцями

**Навколишнє:**
- `FIRE_SPREAD` — поширення вогню
- `TNT` — активація TNT
- `EXPLOSION` — шкода від вибухів
- `EXPLOSION_BLOCK_DAMAGE` — руйнування блоків вибухом
- `LIQUID_FLOW` — потік рідин з/в регіон
- `LIGHTNING` — блискавка

**Вхід/Вихід:**
- `ENTRY` — вхід у регіон (гравці)
- `EXIT` — вихід з регіону
- `ENTRY_DENY_MESSAGE` — кастомне повідомлення при забороні входу
- `EXIT_DENY_MESSAGE` — кастомне повідомлення при забороні виходу
- `GREET_MESSAGE` — повідомлення при вході
- `FAREWELL_MESSAGE` — повідомлення при виході
- `ENTRY_PLAYERS` — кому дозволено входити (all/members/trusted/none)

**Спецефекти та налаштування:**
- `INVINCIBLE` — невразливість у регіоні
- `GAME_MODE` — примусовий gamemode у регіоні (nullable)
- `WEATHER_LOCK` — фіксований погодний стан (nullable)
- `TIME_LOCK` — фіксований час доби (nullable)
- `FLY` — дозволити/заборонити польоту
- `SPAWN_LOCATION` — точка телепортації при вході (якщо задана)

**Права за роллю (per-flag role override):**
- Кожен прапор може мати значення: `ALLOW`, `DENY`, `INHERIT` (з батьківського/глобального)
- Додатково: `ALLOW_MEMBERS`, `ALLOW_TRUSTED` (дозволити тільки учасникам/довіреним)

### RegionRole enum:
```
OWNER      — повний контроль, не може бути видалений ніким крім себе, 1 на регіон
CO_OWNER   — майже всі права, може редагувати прапори та учасників
MANAGER    — може додавати/видаляти MEMBER і VISITOR
MEMBER     — має bypass основних захистів (ламати/ставити/взаємодіяти)
TRUSTED    — може взаємодіяти але не ламати/ставити (якщо прапори так налаштовані)
VISITOR    — явно заборонений (override DENY для цього гравця)
```

---

## Фаза 4 — Wand (Selection Tool)

### Логіка виділення:
- Гравець отримує wand через `/hg wand` — це кастомний предмет (паличка/кристал/інше — вибери найбільш відповідний CustomItem з API)
- **ЛКМ по блоку** → встановлює Точку 1 (мінімальний кут), після чого оновлюється viewer-only preview
- **ПКМ по блоку** → встановлює Точку 2 (максимальний кут), після чого оновлюється viewer-only preview
- Після встановлення обох точок — автоматично малюється **3D territory frame** через debug shapes
- Виділення **зберігається в SelectionSession** прив'язаній до гравця (не зникає при смерті, зберігається до ребуту або явного очищення)
- `/hg create <name>` — перетворює поточне виділення на регіон

### SelectionVisualizer:
- Використовувати viewer-only `DisplayDebug`/`ClearDebugShapes`, а не creative selection tools
- Рендерити статичну territory-style рамку built-in shape'ами (`Cube`, `Cylinder`, `Sphere`)
- Використовувати один shared refresh task замість окремого task на кожного гравця
- Палітра: синьо-помаранчева для валідного selection, червоно-помаранчева для overlap conflict
- Не покладатися на particle effects або block overlay assets для survival selection preview

---

## Фаза 5 — Protection Engine

### ProtectionEngine — алгоритм перевірки:
```
1. Отримати позицію події
2. Знайти всі регіони що містять цю позицію (з кешу по chunk)
3. Якщо регіонів 0 → перевірити глобальний регіон "__global__" → результат
4. Якщо гравець у bypass mode → ALLOW
5. Якщо гравець OP/admin → ALLOW (або перевірити config)
6. Сортувати регіони за пріоритетом (вищий пріоритет — перший)
7. Для кожного регіону (від вищого до нижчого):
   a. Перевірити чи гравець є VISITOR (явна заборона) → DENY
   b. Перевірити чи гравець є OWNER/CO_OWNER/MANAGER/MEMBER/TRUSTED → відповідні права
   c. Перевірити значення прапора для цієї дії
   d. Якщо INHERIT → перейти до наступного (нижчий пріоритет або батьківський)
8. Якщо жоден регіон не дав відповідь → глобальні дефолти з config
```

### Chunk-based індексування:
- При завантаженні регіону — індексувати всі чанки що він торкається
- `Map<ChunkPos, List<String>> chunkIndex` — швидкий lookup
- При зміні меж регіону — перерахувати індекс
- Кеш має бути thread-safe (використати відповідні concurrent колекції)

---

## Фаза 6 — GUI

### RegionMainScreen — головний екран регіону:
- Відкривається командою `/hg gui <region>` або `/hg info <region>` (з кнопкою "Відкрити GUI")
- Містить вкладки: **Загально** | **Прапори** | **Учасники** | **Статистика**
- Вкладка "Загально":
  - Назва регіону (редагується якщо OWNER)
  - Власник
  - Розміри (X×Y×Z блоків, загальна площа)
  - Пріоритет
  - Батьківський регіон (якщо є)
  - Кнопки: "Перемістити спавн сюди", "Телепортуватись", "Видалити регіон"

### FlagEditorScreen — редактор прапорів:
- Групи прапорів (вкладки або секції): Блоки | Гравці | Мобі | Навколишнє | Вхід/Вихід | Спеціальні
- Кожен прапор відображається як рядок:
  - Іконка (відповідний блок/предмет як візуальний хінт)
  - Назва прапора (локалізована)
  - Короткий опис (tooltip)
  - Поточне значення: [ALLOW] / [DENY] / [INHERIT] — кнопки для перемикання
  - Для текстових прапорів (GREET_MESSAGE) — текстове поле
  - Для role-based прапорів — dropdown вибору мінімальної ролі
- Кнопка "Скинути до дефолтів" внизу

### MemberManagerScreen — керування учасниками:
- Список поточних учасників з іконками ролей
- Пошук гравця (по нікнейму)
- Кнопки: Додати | Видалити | Змінити роль
- Підтвердження для видалення OWNER або CO_OWNER
- Відображення онлайн/офлайн статусу гравця (якщо API дозволяє)

---

## Фаза 7 — Система збереження даних

### Формат збереження (JSON):
```
data/
└── hyguard/
    ├── config.json              # Конфігурація мода
    ├── regions/
    │   ├── world_overworld/     # По вимірах
    │   │   ├── __global__.json  # Глобальний регіон
    │   │   ├── <region_id>.json # Окремий файл на регіон
    │   │   └── index.json       # Індекс: name→id, chunk→[ids]
    │   └── world_nether/
    ├── players/
    │   └── <uuid>.json          # Дані гравця: активна wand сесія, bypass статус, owned regions list
    └── backups/
        └── YYYY-MM-DD/          # Автоматичні бекапи раз на день
```

### RegionRepository — важливі вимоги:
- **Атомарне збереження**: писати в temp файл → rename (щоб не корумпувати при краші)
- **Lazy loading**: завантажувати регіони чанку тільки коли чанк завантажується
- **Async saves**: зберігати асинхронно, не блокувати main thread
- **Validation при завантаженні**: перевіряти цілісність даних, логувати і пропускати корумповані
- **MigrationManager**: при завантаженні перевіряти `schemaVersion` у файлі, запускати міграції якщо потрібно

---

## Фаза 8 — Events (кастомні)

Якщо Hytale API підтримує custom events — реалізувати:
- `RegionEnterEvent(player, region, fromPos, toPos)` — відміняємо якщо `ENTRY = DENY`
- `RegionExitEvent(player, region, fromPos, toPos)` — відміняємо якщо `EXIT = DENY`
- `RegionCreatedEvent(player, region)`
- `RegionDeletedEvent(player, region)`
- `RegionModifiedEvent(player, region, whatChanged)`

---

## Фаза 9 — Команди (повний список)

```
/hg wand                          — отримати wand
/hg create <name>                 — створити регіон з поточного виділення
/hg delete <name> [--confirm]     — видалити регіон
/hg info [name|here]              — інфо про регіон (або той де стоїш)
/hg list [page] [--owner=<name>]  — список регіонів
/hg gui [name|here]               — відкрити GUI

/hg select <name>                 — завантажити межі регіону у wand сесію
/hg redefine <name>               — перевизначити межі з поточного виділення
/hg expand <dir> <amt> [region]   — розширити регіон
/hg contract <dir> <amt> [region] — зменшити регіон
/hg shift <dir> <amt> [region]    — зсунути регіон

/hg flag <region> <flag> <value>  — встановити прапор
/hg flag <region> <flag> --reset  — скинути прапор до дефолту
/hg flags <region>                — список всіх прапорів

/hg member add <region> <player> [role]   — додати учасника
/hg member remove <region> <player>       — видалити учасника  
/hg member role <region> <player> <role>  — змінити роль
/hg member list <region>                  — список учасників

/hg tp <name>                     — телепортуватись в регіон (до spawn point або центру)
/hg setspawn <name>               — встановити точку входу там де стоїш
/hg priority <region> <number>    — встановити пріоритет

/hg bypass [on|off]               — toggle bypass режиму (потрібні права адміна)
/hg debug pos                     — показати всі регіони в поточній позиції

/hg reload                        — перезавантажити конфіг (адмін)
/hg save                          — примусово зберегти всі регіони (адмін)
/hg import <format>               — імпорт з WorldGuard/інших форматів (адмін, опціонально)
/hg backup                        — створити ручний бекап (адмін)

/hg help [command]                — допомога
```

---

## Фаза 10 — Конфігурація

### config.json — повний список налаштувань:
```json
{
  "schemaVersion": 1,
  "general": {
    "wandItemId": "auto",
    "selectionRefreshMillis": 900,
    "showEnterExitMessages": true,
    "bypassPermission": "hyguard.bypass",
    "adminPermission": "hyguard.admin"
  },
  "limits": {
    "maxRegionsPerPlayer": 10,
    "maxRegionVolumeBlocks": 1000000,
    "maxRegionSideLength": 1000,
    "minRegionSideLength": 1,
    "allowOverlap": false,
    "allowOverlapSameOwner": true
  },
  "defaults": {
    "flags": {
      "BLOCK_BREAK": "DENY",
      "BLOCK_PLACE": "DENY",
      "PVP": "DENY",
      "MOB_SPAWN_HOSTILE": "ALLOW",
      "FIRE_SPREAD": "DENY",
      "EXPLOSION_BLOCK_DAMAGE": "DENY"
    }
  },
  "globalRegion": {
    "enabled": true,
    "flags": {
      "BLOCK_BREAK": "ALLOW",
      "BLOCK_PLACE": "ALLOW",
      "PVP": "ALLOW"
    }
  },
  "storage": {
    "saveIntervalSeconds": 300,
    "autoBackupEnabled": true,
    "autoBackupIntervalHours": 24,
    "autoBackupKeepDays": 7,
    "asyncSave": true
  },
  "messages": {
    "prefix": "§8[§aHyGuard§8]§r ",
    "noPermission": "§cУ вас немає прав для цієї дії.",
    "regionCreated": "§aРегіон §e{name} §aуспішно створено!",
    "protectionDenied": "§cЦя зона захищена.",
    "entryDenied": "§cВхід заборонено."
  }
}
```

---

## Фаза 11 — Вимоги до якості

### Обов'язково:
- **Thread safety**: всі операції з RegionCache — через concurrent структури або synchronized блоки. Saves — асинхронні. Reads з main thread — тільки через кеш
- **Performance**: ProtectionEngine має працювати за O(log n) або краще. Chunk indexing обов'язковий
- **Error handling**: будь-який IOException при збереженні — логувати + спробувати fallback. Ніколи не крашити сервер через помилку мода
- **Null safety**: перевіряти всі параметри на null з чіткими повідомленнями
- **Validation**: валідувати всі вхідні дані команд (назва регіону: тільки [a-zA-Z0-9_-], довжина 3-32 символи)
- **Logging**: використати стандартний логер. Рівні: DEBUG для детальних операцій, INFO для важливих подій, WARN для потенційних проблем, ERROR для критичних помилок

### Тестування (якщо Hytale підтримує JUnit в моді):
- Unit tests для ProtectionEngine (мок-регіони, мок-гравці)
- Unit tests для RegionSerializer (серіалізація/десеріалізація)
- Unit tests для GeometryUtils (overlap detection)
- Unit tests для команд (парсинг аргументів)

---

## Фаза 12 — Порядок реалізації

Виконувати **строго в цьому порядку**, не переходити до наступного кроку поки поточний не компілюється і не є функціональним:

1. **Дослідження** (Фаза 1) — задокументувати у RESEARCH_NOTES.md
2. **Базова структура мода** — entrypoint, gradle, package структура
3. **Моделі даних** — Region, RegionFlag, RegionMember, RegionRole (без логіки)
4. **RegionRepository + серіалізація** — зберігати та завантажувати регіони
5. **WandItem + SelectionSession** — вибір двох точок, без візуалізації
6. **Команди create/delete/info** — мінімальний робочий функціонал
7. **ProtectionEngine** — базова перевірка block break/place
8. **Event listeners** — підключити ProtectionEngine до подій гри
9. **SelectionVisualizer** — viewer-only debug-shape preview виділення
10. **EnterExitMessageRenderer** — відображення повідомлень при вході
11. **Повний список команд**
12. **GUI екрани**
13. **Повний список прапорів**
14. **Overlap detection + пріоритети**
15. **Глобальний регіон**
16. **Конфігурація**
17. **Автобекапи**
18. **Тести**
19. **Фінальний рефакторинг** — прибрати TODO, перевірити null safety, логування

---

## Фаза 13 — Додаткові фічі (реалізувати після основного функціоналу)

- **Оренда регіонів** — OWNER може здавати регіон в оренду на час (з автовидаленням прав після закінчення)
- **Регіон-магазин** — знак/блок всередині регіону що дозволяє купувати регіон (економіка-інтеграція)
- **Візуальні межі** — постійне відображення меж регіону для власника (toggle)
- **Webhook/API** — HTTP endpoint для серверних панелей (перелік регіонів, статистика)
- **Статистика** — скільки регіонів на сервері, топ по розміру, тощо
- **Регіони-шаблони** — зберегти набір прапорів як шаблон і застосовувати до нових регіонів

---

## Що ЗАБОРОНЕНО робити:

- Не використовувати `Thread.sleep()` в main thread
- Не зберігати дані в NBT гравця (нестабільно) — тільки у власних файлах
- Не копіювати код з MyMods дослівно без розуміння — адаптувати під нову архітектуру
- Не ігнорувати IOException при роботі з файлами
- Не робити синхронні disk I/O операції на main thread
- Не використовувати `instanceof` каскади там де можна використати поліморфізм
- Не хардкодити рядки повідомлень — тільки через MessageConfig

---

## Фінальна перевірка перед завершенням:

- [ ] Мод компілюється без помилок і warnings
- [ ] `/hg wand` видає предмет, ЛКМ/ПКМ встановлюють точки
- [ ] `/hg create test` створює регіон і зберігає у файл
- [ ] Після рестарту регіон завантажується назад
- [ ] Сторонній гравець не може зламати блок у регіоні
- [ ] Власник може зламати блок у своєму регіоні
- [ ] GUI відкривається і відображає актуальні дані
- [ ] `/hg flag test PVP ALLOW` змінює прапор і зберігається
- [ ] Перетин двох регіонів — вищий пріоритет перемагає
- [ ] Глобальний регіон застосовується де немає інших регіонів
- [ ] Логи не містять stack traces при нормальній роботі
- [ ] Немає memory leaks (SelectionSession очищається при дісконекті гравця)
