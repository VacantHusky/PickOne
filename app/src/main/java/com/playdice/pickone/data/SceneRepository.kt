package com.playdice.pickone.data

import com.playdice.pickone.model.ChoiceItem
import com.playdice.pickone.model.Scene
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneRepository @Inject constructor(
    private val dao: SceneDao,
) {
    val scenes: Flow<List<Scene>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    fun observeScene(id: String): Flow<Scene?> = dao.observe(id).map { it?.toModel() }

    suspend fun getScene(id: String): Scene? = dao.get(id)?.toModel()

    suspend fun save(scene: Scene) = dao.save(
        scene.copy(
            name = scene.name.trim(),
            choices = scene.choices.mapIndexed { index, choice ->
                choice.copy(name = choice.name.trim(), position = index)
            },
            updatedAt = System.currentTimeMillis(),
        ),
    )

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun duplicate(id: String) {
        val source = getScene(id) ?: return
        val now = System.currentTimeMillis()
        save(
            source.copy(
                id = UUID.randomUUID().toString(),
                name = duplicateName(source.name),
                choices = source.choices.map { it.copy(id = UUID.randomUUID().toString()) },
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun seedIfEmpty(samples: List<Scene>) {
        if (dao.count() == 0) samples.forEach { dao.save(it) }
    }
}

internal fun duplicateName(source: String): String {
    val suffix = " · 2"
    return source.trim().take((40 - suffix.length).coerceAtLeast(0)) + suffix
}
