package com.zenkaigym.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

/** Quantos nós exibem exatamente [text]; útil em waitUntil, onde não se pode falhar se o nó ainda não existe. */
fun ComposeTestRule.onAllNodesWithTextCount(text: String, substring: Boolean = false): Int =
    onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().size
