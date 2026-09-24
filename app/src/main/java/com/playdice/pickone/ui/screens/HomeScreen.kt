package com.playdice.pickone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playdice.pickone.R
import com.playdice.pickone.model.HomeLayout
import com.playdice.pickone.model.Scene
import com.playdice.pickone.ui.components.NeuButton
import com.playdice.pickone.ui.components.NeuCard
import com.playdice.pickone.ui.components.NeuDialog
import com.playdice.pickone.ui.components.NeuDepth
import com.playdice.pickone.ui.components.NeuDropdownMenu
import com.playdice.pickone.ui.components.NeuIconButton
import com.playdice.pickone.ui.components.selectorIcon
import com.playdice.pickone.ui.components.selectorLabel
import com.playdice.pickone.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onPlay: (String) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var moreOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Scene?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(.78f),
                drawerContainerColor = MaterialTheme.colorScheme.background,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeuCard(
                        modifier = Modifier.size(48.dp),
                        depth = NeuDepth.SHALLOW,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(selectorIcon(com.playdice.pickone.model.SelectorType.DICE), null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.made_for), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                DrawerItem(
                    icon = Icons.Rounded.Home,
                    label = stringResource(R.string.home),
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                )
                DrawerItem(
                    icon = Icons.Rounded.Settings,
                    label = stringResource(R.string.settings),
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onSettings() },
                )
                DrawerItem(
                    icon = Icons.Rounded.Info,
                    label = stringResource(R.string.about),
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onAbout() },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        NeuIconButton(
                            Icons.Rounded.Menu,
                            stringResource(R.string.menu),
                            { scope.launch { drawerState.open() } },
                            modifier = Modifier.padding(start = 8.dp).size(42.dp),
                        )
                    },
                    actions = {
                        NeuIconButton(Icons.Rounded.Add, stringResource(R.string.add_scene), onAdd, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.size(8.dp))
                        Box {
                            NeuIconButton(Icons.Rounded.MoreVert, stringResource(R.string.more), { moreOpen = true }, modifier = Modifier.size(42.dp))
                            NeuDropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.list_mode)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.List, null) },
                                    trailingIcon = { if (settings.homeLayout == HomeLayout.LIST) Icon(Icons.Rounded.Check, stringResource(R.string.selected_check)) },
                                    onClick = { viewModel.setLayout(HomeLayout.LIST); moreOpen = false },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.card_mode)) },
                                    leadingIcon = { Icon(Icons.Rounded.Apps, null) },
                                    trailingIcon = { if (settings.homeLayout == HomeLayout.CARD) Icon(Icons.Rounded.Check, stringResource(R.string.selected_check)) },
                                    onClick = { viewModel.setLayout(HomeLayout.CARD); moreOpen = false },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { padding ->
            if (scenes.isEmpty()) {
                EmptyHome(onAdd, Modifier.padding(padding))
            } else if (settings.homeLayout == HomeLayout.CARD) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(166.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(scenes, key = Scene::id) { scene ->
                        SceneCard(
                            scene = scene,
                            compact = false,
                            onPlay = { onPlay(scene.id) },
                            onEdit = { onEdit(scene.id) },
                            onDuplicate = { viewModel.duplicate(scene.id) },
                            onDelete = { deleteTarget = scene },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(scenes, key = Scene::id) { scene ->
                        SceneCard(
                            scene = scene,
                            compact = true,
                            onPlay = { onPlay(scene.id) },
                            onEdit = { onEdit(scene.id) },
                            onDuplicate = { viewModel.duplicate(scene.id) },
                            onDelete = { deleteTarget = scene },
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { scene ->
        NeuDialog(
            title = stringResource(R.string.delete_scene_title, scene.name),
            text = stringResource(R.string.delete_scene_body),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = { viewModel.delete(scene.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
            destructive = true,
        )
    }
}

@Composable
private fun EmptyHome(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NeuCard(depth = NeuDepth.SHALLOW, shape = CircleShape, contentPadding = PaddingValues(28.dp)) {
            Icon(Icons.Rounded.Apps, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.no_scenes_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.no_scenes_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        NeuButton(stringResource(R.string.create_first_scene), onAdd, icon = Icons.Rounded.Add)
    }
}

@Composable
private fun SceneCard(
    scene: Scene,
    compact: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    NeuCard(modifier = Modifier.fillMaxWidth(), onClick = onPlay) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SceneBadge(scene)
                Spacer(Modifier.size(14.dp))
                SceneInfo(scene, Modifier.weight(1f))
                SceneMenu(menuOpen, { menuOpen = it }, onEdit, onDuplicate, onDelete)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    SceneBadge(scene)
                    Spacer(Modifier.weight(1f))
                    SceneMenu(menuOpen, { menuOpen = it }, onEdit, onDuplicate, onDelete)
                }
                Spacer(Modifier.height(18.dp))
                SceneInfo(scene)
            }
        }
    }
}

@Composable
private fun SceneBadge(scene: Scene) {
    NeuCard(
        modifier = Modifier.size(50.dp),
        depth = NeuDepth.FLAT,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(selectorIcon(scene.selectorType), null, tint = Color(scene.color), modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SceneInfo(scene: Scene, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(scene.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(
            "${selectorLabel(scene.selectorType)} · ${pluralStringResource(R.plurals.item_count, scene.choices.size, scene.choices.size)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SceneMenu(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Box {
        NeuIconButton(Icons.Rounded.MoreVert, stringResource(R.string.more), onClick = { onOpenChange(true) }, modifier = Modifier.size(44.dp))
        NeuDropdownMenu(expanded = open, onDismissRequest = { onOpenChange(false) }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { onOpenChange(false); onEdit() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                onClick = { onOpenChange(false); onDuplicate() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { onOpenChange(false); onDelete() },
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NeuCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        onClick = onClick,
        selected = selected,
        depth = NeuDepth.SHALLOW,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
