# Android Compose Architecture Starter

This project demonstrates a modular, feature-first architecture for Jetpack Compose apps. It splits functionality into coarse **modules**, separates concerns by **layers**, and wires everything together through **Hilt**.

## Module structure
- **app** – Application module hosting the navigation graph and providing Hilt bindings for presenters and the app scope.
- **core**
  - `core:designsystem` – Compose UI theme and design components.
  - `core:common` – Shared utilities including presenter infrastructure and app-wide classes.
- **feature** – Each feature is composed of three modules:
  - `feature:*:api` – Public contracts (routes, state, presenter interfaces).
  - `feature:*:ui` – Pure Compose screens depending only on the API and core modules.
  - `feature:*:impl` – Implementation with ViewModels, repositories and Hilt bindings.

## Layer structure
Each feature is separated into layers that match the modules above:

| Layer | Responsibility | Module example |
|-------|----------------|----------------|
| **API** | Defines navigation destinations and presenter contracts | `feature/catalog/api` |
| **UI** | UI built with Compose, retrieves a presenter via `rememberPresenter` | `feature/catalog/ui` |
| **Impl** | ViewModels and data sources backing the feature | `feature/catalog/impl` |

The `core:common` module provides `PresenterResolver` and helpers such as `rememberPresenter` used by UI modules to obtain their presenters.

## Hilt structure
- `@HiltAndroidApp` `MyApp` is the entry point for dependency injection.
- A custom `AppComponent` and `AppScopeManager` create an application-scoped component that holds an `App` object with navigation actions.
- `HiltPresenterResolver` is injected into the `MainActivity` and uses multibindings to map presenter interfaces to their `@HiltViewModel` implementations.
- Each feature implementation module contributes to that map and provides its own repository bindings (e.g. `CatalogBindings`, `DetailBindings`).

This structure allows UI modules to remain free of Hilt while still obtaining their presenters through the shared `PresenterResolver`, keeping feature APIs clean and implementations encapsulated.

## Network layer
Feature implementation modules own their network and persistence code. Retrofit and OkHttp service interfaces (e.g., `WikipediaService`, `SummarizerService`, `TranslatorService`, and `DictionaryService`) live beside Room entities and DAOs. Hilt modules provide these services and compose them into repositories, such as `ArticleRepository`. These repositories expose `Flow`-based APIs to the rest of the app. This keeps networking concerns isolated within the `impl` layer.

## Adding a feature
1. Create three modules under `feature/<name>/` (`api`, `ui`, `impl`) and include them in `settings.gradle.kts`.
2. In `feature/<name>/api`, declare the route and presenter contract:
```kotlin
@Serializable data object Foo

data class FooState(val text: String = "")
interface FooPresenter {
  val state: StateFlow<FooState>
  fun onAction()
}
```
3. In `feature/<name>/ui`, build the Compose screen and obtain the presenter:
```kotlin
@Composable
fun FooScreen(p: FooPresenter? = null) {
  val presenter = p ?: rememberPresenter<FooPresenter, Unit>()
  val state by presenter.state.collectAsStateWithLifecycle()
  Text(state.text, Modifier.clickable { presenter.onAction() })
}
```
4. In `feature/<name>/impl`, provide the presenter implementation and Hilt bindings:
```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(...) : ViewModel(), FooPresenter { /*...*/ }

@Module
@InstallIn(SingletonComponent::class)
object FooBindings {
  @Provides @IntoMap @ClassKey(FooPresenter::class)
  fun bindFooPresenter(): Class<out ViewModel> = FooViewModel::class.java
}
```
5. Wire the feature into navigation by updating `NavigationActions` if needed.

## Release
To publish a release APK through GitHub Actions, create and push an annotated tag:

```bash
git tag -a v0.1.0 -m "Release 0.1.0"
git push origin v0.1.0
```

The CI workflow will build and upload the release APK for that tag.
