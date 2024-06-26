/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.services.ui.common

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.adobe.marketing.mobile.services.ui.Presentable

/**
 * A state manager that tracks the visibility state of the presentable.
 */
internal class PresentationStateManager {
    private val _presentableState = mutableStateOf(Presentable.State.DETACHED)
    val presentableState: State<Presentable.State> = _presentableState
    val visibilityState = MutableTransitionState(false)

    /**
     * Transition the presentable state to [Presentable.State.VISIBLE] and set the visibility state to true.
     * This is to be called to trigger the presentable to show itself.
     */
    fun onShown() {
        _presentableState.value = Presentable.State.VISIBLE
        visibilityState.targetState = true
    }

    /**
     * Transition the presentable state to [Presentable.State.HIDDEN] and set the visibility state to false.
     * This is to be called to trigger the presentable to hide itself.
     */
    fun onHidden() {
        _presentableState.value = Presentable.State.HIDDEN
        visibilityState.targetState = false
    }

    /**
     * Transition the presentable state to [Presentable.State.DETACHED] and set the visibility state to false.
     * This is to be called to trigger the presentable to detach itself.
     */
    fun onDetached() {
        _presentableState.value = Presentable.State.DETACHED
        visibilityState.targetState = false
    }
}
