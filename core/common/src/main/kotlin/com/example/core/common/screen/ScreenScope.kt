package com.example.core.common.screen

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.savedstate.SavedStateRegistryOwner
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.DefineComponent
import dagger.hilt.EntryPoints
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Scope
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@Scope
@Retention(AnnotationRetention.BINARY)
annotation class ScreenScope

@DefineComponent(parent = ActivityRetainedComponent::class)
@ScreenScope
interface ScreenComponent {
  @DefineComponent.Builder
  interface Builder { fun build(): ScreenComponent }
}

@Scope
@Retention(AnnotationRetention.BINARY)
annotation class SubscreenScope

@DefineComponent(parent = ScreenComponent::class)
@SubscreenScope
interface SubscreenComponent {
  @DefineComponent.Builder
  interface Builder { fun build(): SubscreenComponent }
}

@Module
@InstallIn(ScreenComponent::class)
object ScreenModule {
  @Provides
  @ScreenScope
  fun provideScreenBus() = ScreenBus()
}

class ScreenBus @Inject constructor() {
  val text = MutableStateFlow("hi")
  fun send(s: String) { text.value = s }
}

@Module
@InstallIn(SubscreenComponent::class)
object SubscreenModule {
  @Provides
  @SubscreenScope
  fun provideNestedThing() = NestedThing()
}

class NestedThing @Inject constructor()

interface AssistedVmFactory<T : ViewModel> { fun create(handle: SavedStateHandle): T }

@MapKey
@Target(AnnotationTarget.FUNCTION)
annotation class VmKey(val value: KClass<out ViewModel>)

@Module
@InstallIn(ScreenComponent::class)
abstract class VmBindingModule {
  @Binds
  @IntoMap
  @VmKey(LeftVm::class)
  abstract fun bindLeftVm(f: LeftVm.Factory): AssistedVmFactory<out ViewModel>

  @Binds
  @IntoMap
  @VmKey(RightVm::class)
  abstract fun bindRightVm(f: RightVm.Factory): AssistedVmFactory<out ViewModel>
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint { fun screenBuilder(): ScreenComponent.Builder }

@EntryPoint
@InstallIn(ScreenComponent::class)
interface SubscreenBuilderEntryPoint { fun subBuilder(): SubscreenComponent.Builder }

@EntryPoint
@InstallIn(ScreenComponent::class)
interface VmMapEntryPoint {
  fun vmFactories(): Map<Class<out ViewModel>, @JvmSuppressWildcards AssistedVmFactory<out ViewModel>>
}

@PublishedApi
internal val LocalScreenComponent = staticCompositionLocalOf<Any?> { null }

@PublishedApi
internal class ScreenVmFactory(
  owner: SavedStateRegistryOwner,
  defaultArgs: Bundle?,
  private val component: Any
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

  private val map by lazy {
    val screen: ScreenComponent = when (component) {
      is ScreenComponent -> component
      is SubscreenComponent -> EntryPoints.get(component, ParentScreenEntryPoint::class.java).parent()
      else -> error("Unsupported component type")
    }
    EntryPoints.get(screen, VmMapEntryPoint::class.java).vmFactories()
  }

  @EntryPoint
  @InstallIn(SubscreenComponent::class)
  interface ParentScreenEntryPoint { fun parent(): ScreenComponent }

  override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
    val raw = map[modelClass] ?: error("No AssistedVmFactory bound for ${'$'}{modelClass.name}")
    @Suppress("UNCHECKED_CAST")
    return (raw as AssistedVmFactory<T>).create(handle)
  }
}

@Composable
fun ScreenScope(
  nested: Boolean = false,
  content: @Composable () -> Unit
) {
  val app = LocalContext.current.applicationContext
  val screenBuilder = remember { EntryPoints.get(app, AppEntryPoint::class.java).screenBuilder() }
  val parentAny = LocalScreenComponent.current
  val provided: Any = remember(parentAny, nested) {
    when {
      parentAny == null -> screenBuilder.build()
      nested -> {
        val parentScreen: ScreenComponent = when (parentAny) {
          is ScreenComponent -> parentAny
          is SubscreenComponent -> EntryPoints.get(parentAny, ScreenVmFactory.ParentScreenEntryPoint::class.java).parent()
          else -> error("Unsupported parent type")
        }
        val subBuilder = EntryPoints.get(parentScreen, SubscreenBuilderEntryPoint::class.java).subBuilder()
        subBuilder.build()
      }
      else -> parentAny
    }
  }
  DisposableEffect(provided) { onDispose { } }
  CompositionLocalProvider(LocalScreenComponent provides provided) { content() }
}

@Composable
inline fun <reified VM : ViewModel> magicViewModel(): VM {
  val owner = requireNotNull(LocalViewModelStoreOwner.current) {
    "magicViewModel() must be called where a ViewModelStoreOwner exists"
  }
  val savedOwner = owner as? SavedStateRegistryOwner
    ?: error("Owner must implement SavedStateRegistryOwner")
  val component = LocalScreenComponent.current
    ?: error("No screen/subscreen component in scope. Wrap with ScreenScope { ... }")
  val defaultArgs = (owner as? NavBackStackEntry)?.arguments
  val factory = remember(component, owner, defaultArgs) {
    ScreenVmFactory(savedOwner, defaultArgs, component)
  }
  return viewModel(viewModelStoreOwner = owner, factory = factory)
}

class LeftVm @AssistedInject constructor(
  private val bus: ScreenBus,
  private val analytics: Analytics,
  @Assisted private val handle: SavedStateHandle
) : ViewModel() {
  val text = bus.text.stateIn(viewModelScope, SharingStarted.Eagerly, bus.text.value)
  fun push() = bus.send("Left " + System.currentTimeMillis())
  @AssistedFactory interface Factory : AssistedVmFactory<LeftVm>
}

class RightVm @AssistedInject constructor(
  private val bus: ScreenBus,
  private val repo: FeatureRepo,
  @Assisted private val handle: SavedStateHandle
) : ViewModel() {
  val text = bus.text.stateIn(viewModelScope, SharingStarted.Eagerly, bus.text.value)
  fun push() = bus.send("Right " + System.currentTimeMillis())
  @AssistedFactory interface Factory : AssistedVmFactory<RightVm>
}

class Analytics @Inject constructor()
class FeatureRepo @Inject constructor()
