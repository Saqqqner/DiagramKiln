# DiagramKiln

Java 17+ библиотека для генерации редактируемых диаграмм draw.io: Java DSL или ваши DTO →
иерархический граф ELK → готовый `.drawio` XML. Поддерживаются вложенные контейнеры,
межгрупповые связи и переиспользуемые типизированные стили.

Координаты релиза: **`io.github.saqqqner:diagram-kiln:1.0.0`**.
Релиз доступен локально после `mvnw.cmd install`; удалённая публикация не настроена.

Java-пакеты и имя автоматического модуля: `io.github.saqqqner.diagramkiln`.
Основные точки входа API — `Diagrams` и `DrawioExporter`.

Спецификации на английском находятся в [docs/specs](docs/specs/), планы — в
[docs/plans](docs/plans/).

## Сборка и подключение

Требуется JDK 17+. Maven Wrapper закрепляет Maven 3.9.11; установленный Maven 3.9.x тоже подходит.

```powershell
./mvnw.cmd clean install
```

Linux/macOS: `bash ./mvnw clean install`. Сборка выполняет unit-тесты, строгий Javadoc,
проверку форматирования и отдельный Maven-проект-потребитель. В CI заданы JDK 17 и 21.
Javadoc публичного API написан на английском.

После локальной установки:

```xml
<dependency>
  <groupId>io.github.saqqqner</groupId>
  <artifactId>diagram-kiln</artifactId>
  <version>1.0.0</version>
</dependency>
```

Результаты в `target/`: обычный JAR, `-sources.jar` и `-javadoc.jar`.
HTML API: `target/reports/apidocs/index.html`. Runtime-зависимости подключает Maven.

## Простой DSL

```java
import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

DiagramData diagram = Diagrams.diagram("Статусы заказа")
    .node("new", "Новый")
    .node("paid", "Оплачен")
    .edge("new", "paid", "Оплата")
    .build();

String xml = DrawioExporter.create().toXml(diagram);
Files.writeString(Path.of("order.drawio"), xml, StandardCharsets.UTF_8);
```

Файл открывается в draw.io через File → Open From → Device. ELK вычисляет координаты,
точки изгиба и привязки; узлы, группы и связи остаются редактируемыми.

## XML или сжатая строка для ссылки

```java
var exporter = DrawioExporter.create();

String xml = exporter.toXml(diagram);
String fragment = exporter.toDrawioFragment(diagram); // #R...
String url = "https://app.diagrams.net/?splash=0" + fragment;
```

Оба метода используют те же настройки и выполняют один расчёт ELK на вызов.
`toXml` возвращает обычный редактируемый `.drawio` XML; `toDrawioFragment` — готовый
URI-безопасный фрагмент `#R...`, содержащий полный документ `mxfile`.
Дополнительно кодировать fragment через URLEncoder не нужно.

Упаковка совместима с декодером draw.io: UTF-8 XML → URI-кодирование →
raw DEFLATE без zlib-заголовка → Base64 → URI-кодирование Base64. Используется стандартный
`URLEncoder`, пробелы внутри XML записываются как `%20`; дополнительные `%XX` для
некоторых знаков пунктуации корректно декодируются draw.io. Внутреннее URI-кодирование
сохраняет буквальные `%`, `+`, кириллицу и emoji при распаковке в draw.io.
Это строка для URL, а не XML-файл со сжатым содержимым `<diagram>`.
Ограничения длины URL зависят от браузера и приложения; для больших схем используйте XML-файл.
Генерация строки не делает сетевых запросов. Внутренний `DrawioXmlWriter` поддерживает
`write(...)` и `writeFragment(...)`; прикладной код должен использовать `DrawioExporter`.

Пример обоих результатов — `examples/DrawioFragmentExample.java`.
Формат сверён с [документацией draw.io](https://github.com/jgraph/drawio/wiki/File-Format#compression).

## Вложенные группы и стили

```java
import io.github.saqqqner.diagramkiln.style.Arrow;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

var operation = NodeStyle.builder()
    .fillColor("#E3F2FD").strokeColor("#1565C0").rounded(true).build();
var material = NodeStyle.builder()
    .shape(NodeShape.ELLIPSE).fillColor("#FFF3E0").build();
var section = GroupStyle.builder()
    .fillColor("#E8F5E9").headerHeight(36).bold(true).build();
var transfer = EdgeStyle.builder()
    .strokeColor("#EF6C00").dashed(true).endArrow(Arrow.OPEN).build();

DiagramData diagram = Diagrams.diagram("Крафт железной кирки")
    .group("mining", "Добыча руды", section, g -> g
        .node("drill", "Бурение", operation)
        .node("ore", "Железная руда", material)
        .edge("drill", "ore", "Добыть"))
    .group("smelting", "Переплавка", section, g -> g
        .node("furnace", "Печь", operation)
        .group("output", "Результат", h -> h
            .node("ingot", "Железный слиток", material))
        .edge("furnace", "ingot", "Переплавить"))
    .edge("ore", "furnace", "Передать руду", transfer)
    .edge("mining", "smelting", "Следующий этап")
    .build();
```

Вложенность не ограничена одним уровнем. Готовое описание можно вложить через
`group("id", nestedDiagram)` или `group("id", nestedDiagram, groupStyle)`.
Корневой `sectionName` — имя страницы. Для внешней рамки всего процесса добавьте одну
общую группу, как в `examples/NestedCraftingDiagram.java`.

Все `id` уникальны во всём дереве, включая группы. Концы связей могут указывать на обычные
узлы или группы любого уровня. Поддержаны циклы связей, петли, параллельные связи,
связи контейнера со своим потомком и пустые группы. Циклы владения контейнерами запрещены.

Callback группы выполняется сразу; его содержимое фиксируется после callback. Ссылки
разрешаются при внешнем `build()`, поэтому внутри callback можно ссылаться на соседние
группы, объявленные позднее. Самостоятельный `nestedBuilder.build()` проверяет ссылки
в контексте этой самостоятельной диаграммы.

Стили неизменяемы, их можно использовать многократно:

| Стиль | Свойства |
| --- | --- |
| `NodeStyle` | `shape`, `rounded`, `arcSize`, `fillColor`, `strokeColor`, `strokeWidth`, `dashed`, `fontColor`, `fontSize`, `bold` |
| `GroupStyle` | заливка заголовка, рамка и текст как у узла; дополнительно `headerHeight` |
| `EdgeStyle` | цвет/толщина/пунктир линии, цвет/размер/жирность подписи, `startArrow`, `endArrow`, `endSize`, `rounded`, `arcSize`, `jumpStyle`, `jumpSize` |

Скругление узлов и оформление переходов можно настроить отдельно:

```java
import io.github.saqqqner.diagramkiln.style.JumpStyle;

var status = NodeStyle.builder()
    .rounded(true).arcSize(12)
    .fillColor("#DEEBFF").strokeColor("#0052CC")
    .strokeWidth(2).fontSize(14).bold(true).build();
var transition = EdgeStyle.builder()
    .strokeColor("#34495E").strokeWidth(2)
    .endArrow(Arrow.CLASSIC).endSize(2)
    .rounded(true).arcSize(20)
    .jumpStyle(JumpStyle.GAP).jumpSize(6).build();
```

`NodeStyle.arcSize` — процент скругления прямоугольника (0–100, по умолчанию 15).
Радиус ограничен половиной меньшей стороны; точки подключения учитывают этот радиус.
`EdgeStyle.arcSize` — размер скругления изгиба в пикселях (по умолчанию 20).
Оба параметра работают при `rounded(true)`; для эллипсов и ромбов скругление узла не применяется.
`endSize` — размер конечной стрелки, по умолчанию 6; при `Arrow.NONE` игнорируется.
`JumpStyle`: `NONE`, `ARC`, `GAP`, `SHARP`. Перемычки вычисляет draw.io при пересечении
линий, с учётом порядка их отрисовки; они не меняют маршрутизацию ELK.
`jumpSize` по умолчанию 6, draw.io интерпретирует его в целых пикселях.
Размеры конечной стрелки, перемычки и скругления линии должны быть конечными и неотрицательными.

## DSL размещения

```java
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;

var layout = LayoutOptions.builder()
    .direction(Direction.RIGHT)
    .nodeSize(200, 80)
    .nodeSpacing(50)
    .layerSpacing(100)
    .groupPadding(24)
    .build();

var exporter = DrawioExporter.builder().layoutOptions(layout).build();
String xml = exporter.toXml(diagram);
```

Настройки применяются ко всем уровням вложенности. `nodeSpacing` — минимальный зазор между
узлами одного слоя, `layerSpacing` — между слоями по направлению потока. ELK может увеличить
зазоры для маршрутов. `groupPadding` — внутренний отступ непустых контейнеров; сверху к нему
добавляется высота заголовка. Пустая группа сохраняет размер узла плюс заголовок.
Все расстояния заданы в пикселях, допускается ноль; размеры узлов строго положительные.
По умолчанию: RIGHT, 160×60, зазоры 40/80, отступ 20.
Настройки — неизменяемый объект; билдер можно использовать повторно.

Старые `.direction(...)`, `.nodeSize(...)` и трёхаргументный конструктор `LayoutOptions`
сохранены. В билдере экспортёра последний вызов побеждает: `layoutOptions(...)` заменяет все
настройки, последующий `direction(...)` или `nodeSize(...)` меняет только соответствующие поля.

Семантика оформления сверена с [документацией draw.io](https://www.drawio.com/docs/manual/styles/connector-styles/)
и [mxGraph](https://jgraph.github.io/mxgraph/docs/js-api/files/util/mxConstants-js.html).

Формы: `RECTANGLE`, `ELLIPSE`, `DIAMOND`. Стрелки: `NONE`, `CLASSIC`, `BLOCK`, `OPEN`,
`DIAMOND`. Цвет — `#RRGGBB`; для заливки и обводки также допустим `none`. Размеры в
пикселях, размер шрифта — в пунктах. Значения должны быть конечными и положительными;
`headerHeight=0` разрешён. Без аргумента стиля используется `defaults()`.

## Свои DTO

```java
public record StatusNodeDto(String id, String label) implements DiagramNode {}

public record StatusTransitionDto(String sourceId, String targetId, String label)
    implements DiagramEdge {}

public record StatusDiagramData(
    String sectionName,
    List<StatusNodeDto> nodes,
    List<StatusTransitionDto> edges
) implements DiagramData {
  public StatusDiagramData {
    nodes = nodes == null ? List.of() : List.copyOf(nodes);
    edges = edges == null ? List.of() : List.copyOf(edges);
  }
}
```

Каждый public record — отдельный файл. Интерфейсы импортируются из `io.github.saqqqner.diagramkiln`,
`List` — из `java.util`. `DiagramData` возвращает списки `? extends DiagramNode` и
`? extends DiagramEdge`, поэтому ваши конкретные типы списков подходят напрямую.

Готовый `StatusDiagramData` передаётся сразу в `toXml(data)`; DSL не обязателен.
`DiagramNode.style()` и `DiagramEdge.style()` имеют реализацию по умолчанию. Чтобы задать
собственный стиль, можно переопределить этот метод или добавить компонент `style` в record.
Группа реализует `DiagramGroup`: `id()`, `diagram()` и при необходимости `groupStyle()`;
её подпись по умолчанию берётся из вложенного `sectionName()`.

`build()` и экспортёр снимают глубокий неизменяемый снимок значений. Не изменяйте DTO
одновременно с захватом. Последующие изменения исходных DTO/builder не меняют результат.

## Настройка и собственное размещение

```java
import io.github.saqqqner.diagramkiln.config.Direction;

var exporter = DrawioExporter.builder()
    .direction(Direction.DOWN)
    .nodeSize(200, 80)
    .build();
```

По умолчанию: ELK Layered, `RIGHT`, узлы `160 × 60`. Также доступны `DOWN`, `LEFT`, `UP`.
Размеры контейнеров рассчитываются по детям, отступам и заголовкам.

Свой `layout.LayoutEngine` получает `DiagramData` и `config.LayoutOptions`. Он возвращает
`dto.LayoutResult` с одним `PositionedNode` на каждый узел **и группу**, и одним `RoutedEdge`
на каждую связь. Координаты `Point` и прямоугольников абсолютные, в пикселях страницы.
В маршруте первый/последний элементы лежат на видимых границах концов, остальные — изгибы.
Индекс связи: сначала `edges()` корня, затем рекурсивно связи вложенных диаграмм в порядке
групп. Экспортёр проверяет геометрию и переводит её в локальные координаты ячеек draw.io.
Внутренние связи принадлежат общему контейнеру и перемещаются вместе с ним.

## Примеры и разработка

```powershell
./mvnw.cmd package dependency:copy-dependencies -DincludeScope=runtime
javac --release 17 -encoding UTF-8 -cp "target/diagram-kiln-1.0.0.jar;target/dependency/*" -d target/examples examples/NestedCraftingDiagram.java
java -cp "target/examples;target/diagram-kiln-1.0.0.jar;target/dependency/*" NestedCraftingDiagram
```

Для Linux/macOS замените `;` в classpath на `:`. Результат —
`examples/nested-crafting.drawio`; путь вывода можно передать первым аргументом.
Плоский пример — `examples/OrderStatusDiagram.java`.

Пример на 30 узлов — `examples/ThirtyNodeDiagram.java`; результат —
`examples/thirty-nodes.drawio`. Состав сценария и измерения приведены в
[docs/VALIDATION.md](docs/VALIDATION.md).

Стенды для проверки новых стилей в браузере — `examples/VisualStylesDiagram.java`:
`visual-layout.drawio` с реальным ELK и `visual-jumps.drawio` со сравнением перемычек
на одинаковой фиксированной геометрии. Результаты просмотра и снимки приведены в отчёте.

Форматирование: `./mvnw.cmd formatter:format`. Правила перенесены из omokane в
`docs/CODE-STYLE.md`; `verify` проверяет формат. Пакеты: корневой публичный API,
`config`, `dto`, `style`, `layout`; `internal` не предназначен для вызовов из приложения.
API-документация исключает внутреннюю реализацию.

## Ошибки и границы

`IllegalArgumentException`: null, пустой/повторяющийся id, неизвестный конец связи,
недопустимый XML 1.0 символ, неверный стиль/размер или некорректная геометрия пользовательского
движка. Пустые подписи разрешены. Системные ошибки XML оборачиваются в `IllegalStateException`.

Один экспорт — одна страница. Подписи — обычный текст с XML-экранированием; HTML не
исполняется. Размеры обычных узлов фиксированы: при длинном тексте/большом шрифте увеличьте
`nodeSize`. Автоизмерение текста и отдельная оптимизация пересечений подписей связей
не реализованы. Очень глубокие деревья ограничены ресурсами JVM. Экспортёр можно
переиспользовать; пользовательский движок определяет собственную потокобезопасность.

Подготовка будущей публикации описана в [docs/PUBLISHING.md](docs/PUBLISHING.md).
Лицензия, автор и SCM URL пока не выбраны; публикационный профиль и учётные данные не добавлены.

При реализации использован Context7 (`/eclipse-elk/elk`, `/websites/jgraph_github_io_mxgraph`,
`/websites/maven_apache`). Runtime: ELK 0.11.0, Xbase 2.36.0, Guava 33.4.8-jre;
тесты: JUnit 5.11.4. Xbase указан явно из-за пропуска в POM ELK, Guava закреплена вместо
открытого транзитивного диапазона.

Источники: [ELK Java layout](https://eclipse.dev/elk/documentation/tooldevelopers/usingplainjavalayout.html),
[иерархия ELK](https://eclipse.dev/elk/reference/options/org-eclipse-elk-hierarchyHandling.html),
[формат draw.io](https://github.com/jgraph/drawio/wiki/File-Format).
