package me.next.serverdriven.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.aakira.napier.Napier
import me.next.serverdriven.compose.provider.JsonFileNodeTypeProvider
import me.next.serverdriven.compose.provider.JsonNodeTypeProvider
import me.next.serverdriven.core.library.ActionHandler
import me.next.serverdriven.core.library.ComponentHandler
import me.next.serverdriven.core.library.SDLibrary
import me.next.serverdriven.core.library.layout.SDLayout
import me.next.serverdriven.core.library.navigation.SDNavigation
import me.next.serverdriven.core.method.registerBooleanMethods
import me.next.serverdriven.core.tree.IgnoredNode
import me.next.serverdriven.core.tree.ServerDrivenNode
import openUrl

@Composable
fun SDCLibrary(
    vararg libraries: SDLibrary,
    debug: Boolean = false,
    block: @Composable (SDCLibrary) -> Unit
) {
    DEBUG = debug
    block.invoke(SDCLibrary.instance.apply {
        for (library in libraries) {
            addLibrary(library)
        }
    })
}

var DEBUG: Boolean = false
val logger = Napier
typealias MethodHandler = (ServerDrivenNode, MutableMap<String, String>) -> String
typealias NodeProvider = (String) -> ServerDrivenNode

class SDCLibrary private constructor() {
    private val libraries: HashMap<String, SDLibrary> = HashMap()

    companion object {
        private val LocalMethods: HashMap<String, MethodHandler> = HashMap()
        private val LocalNodeTypeProviders: HashMap<String, NodeProvider> = HashMap()
        private val LocalLib = staticCompositionLocalOf {
            val defaultLibraries = listOf(
                SDLayout(),
                SDNavigation()
            )
            SDCLibrary().apply {
                registerBooleanMethods()
                registerNodeTypeProvider("json") { json ->
                    JsonNodeTypeProvider(json).node
                }
                registerNodeTypeProvider("file") { res ->
                    JsonFileNodeTypeProvider(res).node
                }
                registerNodeTypeProvider("link") { link ->
                    openUrl(link)
                    IgnoredNode
                }
                for (library in defaultLibraries) {
                    addLibrary(library)
                }
            }
        }
        val instance: SDCLibrary
            @Composable
            get() = LocalLib.current

        fun registerMethod(
            method: String,
            handler: MethodHandler
        ) {
            LocalMethods[method] = handler
        }

        fun loadMethod(method: String): MethodHandler {
            return LocalMethods[method] ?: error("No MethodHandler for method: $method")
        }

        fun registerNodeTypeProvider(
            nodeType: String,
            handler: NodeProvider
        ) {
            LocalNodeTypeProviders[nodeType] = handler
        }

        fun loadNodeTypeProvider(
            nodeType: String
        ): NodeProvider {
            return LocalNodeTypeProviders[nodeType] ?: error("No NodeProvider for type: $nodeType")
        }

        @Composable
        fun loadComponent(
            node: ServerDrivenNode,
            dataState: MutableMap<String, String>
        ) {
            if (node is IgnoredNode) return
            val nodeComponent = node.component
            val component = SDCLibrary.instance.getComponent(nodeComponent)
            if (component != null) {
                component.invoke(node, dataState)
            } else {
                Text(
                    text = "UNKNOWN SERVER DRIVEN COMPONENT\n${node.component}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Red)
                        .wrapContentSize()
                )
            }
        }

        @Composable
        fun loadAction(node: ServerDrivenNode?): ActionHandler {
            val nodeComponent = node?.component ?: return { _, _ -> }
            val action = SDCLibrary.instance.getAction(nodeComponent)
            if (action != null) {
                return action
            } else {
                Text(
                    text = "UNKNOWN SERVER DRIVEN ACTION\n${node.component}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Red)
                        .wrapContentSize()
                )
                return { _, _ -> }
            }
        }

        @Composable
        fun loadActions(nodes: ArrayList<ServerDrivenNode>): ActionHandler {
            val actions = ArrayList<Pair<ServerDrivenNode, ActionHandler>>()
            for (node in nodes) {
                actions.add(Pair(node, loadAction(node)))
            }
            return { _, map ->
                for (action in actions) {
                    action.second.invoke(action.first, map)
                }
            }
        }
    }

    fun addLibrary(library: SDLibrary): SDCLibrary {
        if (libraries[library.namespace] == null) {
            libraries[library.namespace] = library
        } else {
            libraries[library.namespace]?.merge(library)
        }
        return this
    }

    fun getAction(
        nodeAction: String
    ): ActionHandler? {
        val split = nodeAction.split(':')
        val libraryNamespace = split[0]
        val componentNamespace = split[1]
        val library = libraries[libraryNamespace]
        return library?.getAction(componentNamespace)
    }

    fun getComponent(nodeComponent: String): ComponentHandler? {
        val split = nodeComponent.split(':')
        val libraryNamespace = split[0]
        val componentNamespace = split[1]
        val library = libraries[libraryNamespace]
        return library?.getComponent(componentNamespace)
    }
}

