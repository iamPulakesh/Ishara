package com.isharaai.isl.core.tutorial

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot

//Shared map of tutorial target keys → screen-space bounds.
val tutorialTargets = mutableStateMapOf<String, Rect>()

//Call from Modifier.onGloballyPositioned to register a UI element as a tutorial target.
fun registerTarget(key: String, coords: LayoutCoordinates) {
    tutorialTargets[key] = coords.boundsInRoot()
}
