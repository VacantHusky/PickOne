package com.playdice.pickone.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.playdice.pickone.model.Scene
import com.playdice.pickone.model.ChoiceItem
import com.playdice.pickone.model.SelectorType
import com.playdice.pickone.model.SoundChoice
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scenes")
data class SceneEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "selector_type") val selectorType: String,
    val color: Long,
    val sound: String,
    @ColumnInfo(name = "animation_duration_ms") val animationDurationMs: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "choices",
    foreignKeys = [
        ForeignKey(
            entity = SceneEntity::class,
            parentColumns = ["id"],
            childColumns = ["scene_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scene_id")],
)
data class ChoiceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "scene_id") val sceneId: String,
    val name: String,
    val weight: Int,
    val position: Int,
)

data class SceneWithChoices(
    @Embedded val scene: SceneEntity,
    @Relation(parentColumn = "id", entityColumn = "scene_id")
    val choices: List<ChoiceEntity>,
)

@Dao
abstract class SceneDao {
    @Transaction
    @Query("SELECT * FROM scenes ORDER BY updated_at DESC")
    abstract fun observeAll(): Flow<List<SceneWithChoices>>

    @Transaction
    @Query("SELECT * FROM scenes WHERE id = :id")
    abstract fun observe(id: String): Flow<SceneWithChoices?>

    @Transaction
    @Query("SELECT * FROM scenes WHERE id = :id")
    abstract suspend fun get(id: String): SceneWithChoices?

    @Query("SELECT COUNT(*) FROM scenes")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertScene(scene: SceneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertChoices(choices: List<ChoiceEntity>)

    @Query("DELETE FROM choices WHERE scene_id = :sceneId")
    protected abstract suspend fun deleteChoices(sceneId: String)

    @Query("DELETE FROM scenes WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Transaction
    open suspend fun save(scene: Scene) {
        insertScene(scene.toEntity())
        deleteChoices(scene.id)
        insertChoices(scene.choices.mapIndexed { index, item -> item.toEntity(scene.id, index) })
    }
}

@Database(
    entities = [SceneEntity::class, ChoiceEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sceneDao(): SceneDao
}

fun SceneWithChoices.toModel(): Scene = Scene(
    id = scene.id,
    name = scene.name,
    selectorType = scene.selectorType.enumOrDefault(SelectorType.WHEEL),
    color = scene.color,
    sound = scene.sound.enumOrDefault(SoundChoice.CRISP),
    animationDurationMs = scene.animationDurationMs,
    choices = choices.sortedBy { it.position }.map {
        ChoiceItem(id = it.id, name = it.name, weight = it.weight, position = it.position)
    },
    createdAt = scene.createdAt,
    updatedAt = scene.updatedAt,
)

private fun Scene.toEntity() = SceneEntity(
    id = id,
    name = name,
    selectorType = selectorType.name,
    color = color,
    sound = sound.name,
    animationDurationMs = animationDurationMs,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ChoiceItem.toEntity(sceneId: String, position: Int) = ChoiceEntity(
    id = id,
    sceneId = sceneId,
    name = name,
    weight = weight,
    position = position,
)
