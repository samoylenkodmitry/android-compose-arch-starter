package com.archstarter.feature.settings.impl

import com.archstarter.feature.settings.impl.data.SettingsDataBindings
import com.archstarter.feature.settings.impl.language.LanguageChooserPresenterBindings
import com.archstarter.feature.settings.impl.language.LanguageChooserScreenBindings

interface SettingsAppGraphBindings :
    SettingsDataBindings,
    SettingsPresenterBindings,
    LanguageChooserPresenterBindings

interface SettingsScreenGraphBindings :
    SettingsScreenBindings,
    LanguageChooserScreenBindings
