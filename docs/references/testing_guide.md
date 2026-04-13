# Testing Guide

This document covers how tests are structured in the Settlements project, including the test Dagger graph and Mockito
integration. For the production Dagger setup, see [Dagger Guide](dagger_guide.md).

---

## Test Dagger Graph

The test source set has its own Dagger component and modules, separate from the production graph. This allows tests to
use real object graphs without depending on NeoForge runtime infrastructure like `ConfigFactory`.

### TestSettlementsComponent

**File:** `src/test/java/.../di/TestSettlementsComponent.java`

```
@Singleton
@Component(modules = {
    TestConfigModule.class,
    TestDataManagerModule.class,
})
public interface TestSettlementsComponent {
    GenerationPipeline generationPipeline();
    GenerationDataValidator generationDataValidator();
    BuildingDefinitionDataManager buildingDefinitionDataManager();
    BiomeSurveyDataManager biomeSurveyDataManager();
    // ... other data managers
}
```

- Scoped `@Singleton` like the production component.
- Uses test-specific modules instead of `ConfigModule`, `DataManagerModule`, and `GenerationModule`.
- Dagger generates `DaggerTestSettlementsComponent` at compile time.

### TestConfigModule

**File:** `src/test/java/.../di/TestConfigModule.java`

Currently empty — a placeholder for future test-only config bindings. The existing test graph focuses on
generation/data-validation, which does not need behavior config records.

### TestDataManagerModule

**File:** `src/test/java/.../di/TestDataManagerModule.java`

Provides real data manager instances and domain registry bindings. Mirrors the production `DataManagerModule` +
`GenerationModule` but without NeoForge dependencies like `ConfigFactory` or `EnchantmentEngine`.

Data managers have a `loadForTest(Map<ResourceLocation, JsonElement>)` method that allows tests to inject JSON data
directly without needing resource pack loading.

---

## Testing Patterns

### Pattern 1: Dagger test component (integration tests)

Use when testing interactions between multiple real objects (e.g., data validation across managers).

```
class GenerationDataValidatorTest {

    private final TestSettlementsComponent component = DaggerTestSettlementsComponent.create();
    private final TraitDefinitionDataManager traitManager = this.component.traitDefinitionDataManager();
    private final TraitScorerDataManager scorerManager = this.component.traitScorerDataManager();
    private final GenerationDataValidator validator = this.component.generationDataValidator();

    @BeforeEach
    void resetManagers() {
        this.traitManager.loadForTest(Map.of(
            resource("settlements:traits/definitions/farming"), JsonParser.parseString("""
                { "id": "settlements:settlement_traits/farming", ... }
            """)
        ));
    }

    @Test
    void unknown_scorer_trait_is_excluded() {
        this.scorerManager.loadForTest(Map.of(...));
        this.validator.validateAndApply(this.traitManager, this.scorerManager, this.buildingManager);
        assertEquals(1, this.scorerManager.allScorers().size());
    }
}
```

**When to use:** Testing wiring correctness, registry interactions, pipeline integration, or any scenario where you
need multiple collaborating objects with real behavior.

### Pattern 2: Mockito (unit tests with mocked dependencies)

Use when isolating a single class and mocking its collaborators.

```
@ExtendWith(MockitoExtension.class)
class GenerationDataValidatorMockitoTest {

    private final TraitScorerDataManager scorerManager = new TraitScorerDataManager();
    private final GenerationDataValidator validator = new GenerationDataValidator();

    @Mock
    private TraitRegistry traitRegistry;

    @Test
    void excludes_unknown_scorers_using_mocked_trait_registry() {
        when(this.traitRegistry.allTraitIds()).thenReturn(Set.of(TraitId.of("settlements:settlement_traits/farming")));
        this.validator.validateAndApply(this.traitRegistry, this.scorerManager, this.buildingManager);
        assertEquals(1, this.scorerManager.allScorers().size());
    }
}
```

**When to use:** Testing a class in isolation, verifying behavior with controlled inputs, or when the real dependency
is expensive or unavailable (e.g., NeoForge runtime objects).

### Pattern 3: Plain unit tests (no DI, no mocks)

Many classes — especially domain models, value objects, codecs, and utility classes — are tested with plain JUnit
without any DI or mocking. These are the simplest and fastest tests.

```
class BiomeIdTest {
    @Test
    void testSomething() {
        // Direct construction, no DI needed
    }
}
```

---

## Choosing a Pattern

| Scenario                                       | Pattern                                 | Why                                        |
|------------------------------------------------|-----------------------------------------|--------------------------------------------|
| Testing interactions between 2+ real objects   | Dagger test component                   | Real wiring, catches integration issues    |
| Isolating one class, controlling collaborators | Mockito `@Mock`                         | Fast, focused, no side effects             |
| Pure logic, no dependencies                    | Plain JUnit                             | Simplest possible test                     |
| Testing domain models, value objects           | Plain JUnit                             | Domain layer has no DI dependencies        |
| Testing data manager parsing                   | Dagger or plain `new` + `loadForTest()` | `loadForTest()` avoids resource pack infra |

---

## Adding Test Bindings

To add a new dependency to the test Dagger graph:

1. Add a `@Provides @Singleton` method to `TestDataManagerModule` (for data managers/services) or `TestConfigModule`
   (for config records).
2. Add an accessor method to `TestSettlementsComponent`.
3. Rebuild — Dagger generates the updated `DaggerTestSettlementsComponent`.

The test component does not include `ServerComponent` or `ClientComponent` subcomponents. If you need server-scoped or
client-scoped services in tests, either construct them directly or use Mockito.
