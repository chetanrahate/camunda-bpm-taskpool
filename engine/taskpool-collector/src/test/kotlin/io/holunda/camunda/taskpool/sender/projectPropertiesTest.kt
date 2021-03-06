@file:Suppress("UNREACHABLE_CODE", "UNCHECKED_CAST")

package io.holunda.camunda.taskpool.sender

import io.holunda.camunda.taskpool.sender.accumulator.PropertyOperation
import io.holunda.camunda.taskpool.sender.accumulator.projectProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*
import kotlin.reflect.KClass

class PropertiesProjectorTest {

  @Test
  fun `should return the command if details are empty`() {

    val model = model("Foo", "My foo model")
    val result = projectProperties(model)

    assertThat(result).isEqualTo(model)
  }

  @Test
  fun `should replace a detail`() {

    val model = model("Foo", "My foo model")
    val result = projectProperties(model, listOf(named(name = "new name", id = model.id)))

    assertThat(result).isEqualTo(model.copy(name = "new name"))
  }

  @Test
  fun `should replace a detail with the last value`() {

    val model = model("Foo", "My foo model")
    val result = projectProperties(model, listOf(
      named(name = "wrong name", id = model.id),
      named(name = "new name", id = model.id))
    )

    assertThat(result).isEqualTo(model.copy(name = "new name"))
  }

  @Test
  fun `should replace detail map`() {

    val model = model("Foo", "My foo model", payload = mutableMapOf("foo" to "bar"))
    val result = projectProperties(model, listOf(
      payload(payload = mutableMapOf("zee" to "test"), id = model.id),
      named(name = "new name", id = model.id))
    )

    assertThat(result).isEqualTo(model.copy(name = "new name", enriched = true, payload = mutableMapOf("zee" to "test")))
  }

  @Test
  fun `should add a detail to a map`() {

    val model = model("Foo", "My foo model", payload = mutableMapOf("foo" to "bar"))
    val result = projectProperties(model, listOf(
      payload(payload = mutableMapOf("zee" to "test"), id = model.id),
      named(name = "new name", id = model.id)),

      mutableMapOf<KClass<out Any>, PropertyOperation>(
        Payload::class to { map, key, value ->
          val originalValue = map[key]
          when (originalValue) {
            is MutableMap<*, *> -> originalValue.putAll(value as Map<Nothing, Nothing>)
            else -> map[key] = value
          }
        }
      )
    )

    assertThat(result).isEqualTo(model.copy(name = "new name", enriched = true, payload = mutableMapOf("foo" to "bar", "zee" to "test")))
  }


  @Test
  fun `should remove a detail from a map`() {

    val model = model("Foo", "My foo model", payload = mutableMapOf("foo" to "bar"), users = mutableListOf("kermit", "gonzo"))
    val result = projectProperties(model, listOf(

      // use payload
      payload(payload = mutableMapOf("foo" to "bar"), id = model.id, users = mutableListOf("gonzo")),

      named(name = "new name", id = model.id)),

      // to remove elements
      mutableMapOf<KClass<out Any>, PropertyOperation>(
        Payload::class to { map, key, value ->
          val originalValue = map[key]
          when (originalValue) {
            // remove only if the key and the value are equal
            is MutableMap<*, *> -> (value as Map<*, *>).entries.forEach { originalValue.remove(key = it.key, value = it.value) }
            is MutableCollection<*> -> (value as MutableCollection<*>).forEach { originalValue.remove(it) }
            else -> map[key] = value
          }
        }
      )
    )

    // map elements should vanish, kermit remains alone in the list
    assertThat(result).isEqualTo(model.copy(name = "new name", enriched = true, payload = mutableMapOf(), users = mutableListOf("kermit")))
  }

}

internal fun model(
  name: String, description: String = "",
  id: String = UUID.randomUUID().toString(),
  payload: MutableMap<String, Any> = mutableMapOf(),
  users: MutableList<String> = mutableListOf()
) = Model(
  id = id,
  name = name,
  description = description,
  payload = payload,
  users = users,
  enriched = !payload.isEmpty()
)

internal fun payload(payload: MutableMap<String, Any>, id: String = UUID.randomUUID().toString(), users: MutableList<String> = mutableListOf()) = Payload(
  id = id,
  payload = payload,
  users = users
)


internal fun named(name: String, id: String = UUID.randomUUID().toString()) = NamedWithId(id = id, name = name)


internal data class Model(
  override val id: String,
  val name: String,
  val description: String,
  override var enriched: Boolean = false,
  override val payload: MutableMap<String, Any> = mutableMapOf(),
  override val users: MutableList<String> = mutableListOf()
) : WithId, WithPayload, WithUsers

internal data class NamedWithId(
  override val id: String,
  val name: String
) : WithId

internal data class Payload(
  override val id: String,
  override var enriched: Boolean = true,
  override val payload: MutableMap<String, Any> = mutableMapOf(),
  override val users: MutableList<String> = mutableListOf()

) : WithId, WithPayload, WithUsers

internal interface WithId {
  val id: String
}

internal interface WithPayload {
  val enriched: Boolean
  val payload: MutableMap<String, Any>
}

internal interface WithUsers {
  val users: MutableList<String>
}
