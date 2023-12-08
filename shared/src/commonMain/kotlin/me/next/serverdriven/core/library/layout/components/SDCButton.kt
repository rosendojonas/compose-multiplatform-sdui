package me.next.serverdriven.core.library.layout.components

import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.next.serverdriven.compose.SDCLibrary
import me.next.serverdriven.core.library.interfaces.Layout
import me.next.serverdriven.core.tree.ServerDrivenNode

class SDCButton(val node: ServerDrivenNode, val state: MutableMap<String, String>) : Layout {
    private var modifier = Modifier.fromNode(node)
    private val actions = node.propertyNodes("onClick")
    private val loadChildren: @Composable () -> Unit? = {
        node.children?.let {
            for (serverDrivenNode in it) {
                SDCLibrary.loadComponent(node = serverDrivenNode, dataState = state)
            }
        }
    }

    @Composable
    override fun Content() {
        val action = SDCLibrary.loadActions(actions)
        Button(modifier = modifier, onClick = {
            action.invoke(node, state)
        }) {
            loadChildren.invoke()
        }
    }
}
