# Android Compose Architecture Starter

This project demonstrates a modular, feature-first architecture for Jetpack Compose apps. It splits functionality into coarse **modules**, separates concerns by **layers**, and wires everything together through **Koin**.

## Module structure
- **app** – Application module hosting the navigation graph and providing Koin bindings for presenters and the app scope.
- **core**
  - `core:designsystem` – Compose UI theme and design components.
  - `core:common` – Shared utilities including presenter infrastructure and app-wide classes.
- **feature** – Each feature is composed of three modules:
  - `feature:*:api` – Public contracts (routes, state, presenter interfaces).
  - `feature:*:ui` – Pure Compose screens depending only on the API and core modules.
  - `feature:*:impl` – Implementation with ViewModels, repositories and Koin modules.

## Layer structure
Each feature is separated into layers that match the modules above:

| Layer | Responsibility | Module example |
|-------|----------------|----------------|
| **API** | Defines navigation destinations and presenter contracts | `feature/catalog/api` |
| **UI** | UI built with Compose, retrieves a presenter via `rememberPresenter` | `feature/catalog/ui` |
| **Impl** | ViewModels and data sources backing the feature | `feature/catalog/impl` |

The `core:common` module provides `PresenterResolver` and helpers such as `rememberPresenter` used by UI modules to obtain their presenters.

## Koin structure
- `MyApp` starts Koin and registers feature modules.
- `KoinPresenterResolver` resolves presenters from Koin in the `MainActivity`.
- Each feature implementation module exposes its own module with repository and ViewModel bindings (e.g. `catalogModule`, `detailModule`).

This structure allows UI modules to remain free of Koin while still obtaining their presenters through the shared `PresenterResolver`, keeping feature APIs clean and implementations encapsulated.

## Network layer
Feature implementation modules own their network and persistence code. Retrofit and OkHttp service interfaces (e.g., `WikipediaService`, `SummarizerService`, `TranslatorService`, and `DictionaryService`) live beside Room entities and DAOs. Koin modules provide these services and compose them into repositories, such as `ArticleRepository`. These repositories expose `Flow`-based APIs to the rest of the app. This keeps networking concerns isolated within the `impl` layer.

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
4. In `feature/<name>/impl`, provide the presenter implementation and Koin bindings:
```kotlin
class FooViewModel(...) : ViewModel(), FooPresenter { /*...*/ }

val fooModule = module {
  viewModel { FooViewModel(get()) }
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
