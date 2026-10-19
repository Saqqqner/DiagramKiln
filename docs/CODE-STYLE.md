# Code Style

## Java

### Feature package organization

Keep package-by-feature: implementation stays inside its owning feature. When a
feature contains several responsibilities, group its classes in responsibility
subpackages, following existing modules:

- `dto`: immutable request, response and protocol records.
- `config` or the established feature's `policy`: validated configuration.
- `repository`: persistence and bounded database projections.
- `service`: use cases, orchestration and pure domain calculations.
- `job`: scheduled/background entry points.
- `api`: controllers when that feature exposes HTTP operations.

Do not mix these implementation roles in one large flat package. Create only the
subpackages actually needed; small public feature boundaries may remain at the
feature root. Test packages should follow the production responsibility where
this preserves package-private access. Package reorganization must preserve
configuration prefixes, HTTP/JSON contracts, transactions and numerical behavior.

Use explicit imports for actual dependencies; do not add broad wildcard package
imports or imports of the same package. Shared data records belong in `dto` when
both services and repositories consume them; do not make a repository depend on
a service class solely to obtain its record type. Keep package-private members
unless an actual production boundary requires broader access.

### Tools
- **IntelliJ IDEA** — primary editor
- **Project settings:** verify the settings below

### Indentation and formatting
- **Indent:** 2 spaces (continuations: 4)
- **Right margin:** 100 characters
- **Line wrapping:**
  - Put `else`, `while`, `catch`, and `finally` on a new line
  - Put array braces `{` and `}` on new lines
  - Put binary/ternary operators (`<`, `>`, `?`, `:`, `=`) on a new line
- **Arrangement:**
  1. Group getters and setters
  2. Group overridden methods
  3. Order dependent methods breadth-first

### Static methods

- Use ordinary private instance methods for helpers called from instance methods or constructors.
- Keep helpers static when they are called from static methods.
- Preserve public static factory methods, such as `builder()` and `create()`, and constants.

### Injection
- Use **constructor injection only**.
- Do not put `@Autowired` on fields.

### DTO
- Use **`record`** for DTO classes.

### Logging
- Use `@Slf4j` only.

### Money and time
- Use `BigDecimal` for money.
- Use `Instant` for time.

### Threads
- Set `spring.threads.virtual.enabled=true` for background tasks.

### JavaDoc and comments

Add JavaDoc or focused comments to public or nontrivial APIs when they clarify a caller-visible contract. Cover relevant role, preconditions, side effects, failure behavior, transaction/idempotency guarantees, or thread/resource ownership. Do not add boilerplate to trivial accessors or repeat a contract in another language.

## Python

### Naming conventions
- **snake_case** for variables, functions, methods, and modules
- **PascalCase** for classes
- **UPPER_CASE** for constants

### Pydantic models for REST API
- **Field names:** `camelCase` in serialized JSON (REST convention)
- **Model attributes:** `snake_case` in Python, with `model_config = ConfigDict(populate_by_name=True)`
- **Example:**
  ```python
  class PredictionRequest(BaseModel):
      model_config = ConfigDict(populate_by_name=True)

      symbol: str = "BTCUSDT"
      timeframe: int = 15
  ```
- **Important:** `populate_by_name=True` lets FastAPI accept both `snake_case` (Python) and `camelCase` (JSON/REST), while model attributes remain `snake_case`.

### Style
- PEP 8
- Line length ≤ 88 characters

### Docstrings
- Add docstrings to public or nontrivial functions and classes when they clarify the caller-visible contract.
- Describe relevant role, preconditions, side effects, failures, idempotency, or resource/thread ownership.
- Do not add boilerplate for trivial accessors or duplicate the same contract in another language.
- Use Google or NumPy style.

## Git

### Commits
- Conventional Commits
- Format: `<type>: <description>`
- Types: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`

### Branch naming
- `main` — primary branch
- `dev` — working branch
- Feature: `feat/<name>`
- Hotfix: `hotfix/<name>`
- For hotfixes, reference the phase in the commit description.

## Java code examples

### Entity
```java
@Data
@Entity
@Table(name = "trading_pairs")
public class TradingPair {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 32)
  private String symbol;

  // Other fields and lifecycle methods omitted.
}
```

### Service (constructor injection)
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {
  private final MlClient mlClient;
  private final PredictionRepository predictionRepository;

  // Methods omitted.
}
```

### DTO (record)
```java
public record PredictionDto(
  String symbol,
  Long timeframe,
  Instant predictedAt,
  Instant targetTime,
  Direction direction,
  double probability,
  String confidence
) {}
```

## Additional resources

- `CLAUDE.md` — general project context
- `docs/DB-CONVENTIONS.md` — Liquibase migrations
