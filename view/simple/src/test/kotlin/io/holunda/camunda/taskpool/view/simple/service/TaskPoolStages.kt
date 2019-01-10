package io.holunda.camunda.taskpool.view.simple.service

import com.tngtech.jgiven.Stage
import com.tngtech.jgiven.annotation.*
import io.holunda.camunda.taskpool.api.task.ProcessReference
import io.holunda.camunda.taskpool.api.task.TaskAssignedEngineEvent
import io.holunda.camunda.taskpool.api.task.TaskCreatedEngineEvent
import io.holunda.camunda.taskpool.view.Task
import io.holunda.camunda.taskpool.view.TaskWithDataEntries
import io.holunda.camunda.taskpool.view.auth.User
import io.holunda.camunda.taskpool.view.query.TaskForIdQuery
import io.holunda.camunda.taskpool.view.query.TasksWithDataEntriesForUserQuery
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.config.EventProcessingConfiguration
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.camunda.bpm.engine.variable.VariableMap
import org.mockito.Mockito

open class TaskPoolStage<SELF : TaskPoolStage<SELF>> : Stage<SELF>() {

  @ScenarioState
  lateinit var testee: TaskPoolService

  @BeforeScenario
  fun init() {
    testee = TaskPoolService(Mockito.mock(QueryUpdateEmitter::class.java), Mockito.mock(EventProcessingConfiguration::class.java))
  }

  open fun task_created_event_is_received(event: TaskCreatedEngineEvent): SELF {
    testee.on(event)
    return self()
  }

  open fun task_assign_event_is_received(event: TaskAssignedEngineEvent): SELF {
    testee.on(event)
    return self()
  }

}

open class TaskPoolGivenStage<SELF : TaskPoolGivenStage<SELF>> : TaskPoolStage<SELF>() {

  @ProvidedScenarioState(resolution = ScenarioState.Resolution.NAME)
  private var tasks : List<TaskWithDataEntries> = listOf()

  private val procRef = ProcessReference("instance1", "exec1", "def1", "def-key", "proce1", "app")

  private fun task(i: Int) = TaskWithDataEntries(Task("id$i", procRef, "task-key-$i", businessKey = "BUS-$i"))

  open fun no_task_exists(): SELF {
    tasks = listOf()
    return self()
  }

  @As("$ tasks exist")
  open fun tasks_exist(numTasks: Int): SELF {
    tasks = (0 until numTasks).map { task(it) }
    return self()
  }

}

open class TaskPoolWhenStage<SELF : TaskPoolWhenStage<SELF>> : TaskPoolStage<SELF>() {

  @ExpectedScenarioState(resolution = ScenarioState.Resolution.NAME, required = true)
  private lateinit var tasks : List<TaskWithDataEntries>

  @ProvidedScenarioState(resolution = ScenarioState.Resolution.NAME)
  private var queriedTasks : MutableList<TaskWithDataEntries> = mutableListOf()

  private fun query(page: Int, size: Int) = TasksWithDataEntriesForUserQuery(User("kermit", setOf()), page, size)

  @As("Page $ is queried with a page size of $")
  open fun tasks_queried(page: Int, size: Int): SELF {
    queriedTasks.addAll(testee.slice(tasks, query(page, size)).tasksWithDataEntries)
    return self()
  }

}

open class TaskPoolThenStage<SELF : TaskPoolThenStage<SELF>> : TaskPoolStage<SELF>() {

  @ExpectedScenarioState(resolution = ScenarioState.Resolution.NAME, required = true)
  private lateinit var tasks : List<TaskWithDataEntries>

  @ExpectedScenarioState(resolution = ScenarioState.Resolution.NAME, required = true)
  private lateinit var queriedTasks : List<TaskWithDataEntries>

  @As("$ tasks are returned")
  open fun num_tasks_are_returned(numTasks: Int): SELF {
    assertThat(queriedTasks.size).isEqualTo(numTasks)
    return self()
  }

  @As("all tasks are returned once")
  open fun all_tasks_are_returned(): SELF {
    assertThat(queriedTasks).isEqualTo(tasks)
    return self()
  }

  open fun task_is_created(task: Task): SELF {
    assertThat(testee.query(TaskForIdQuery(task.id))).isEqualTo(task)
    return self()
  }

  @As("task with id $ is assigned to $")
  open fun task_is_assigned_to(taskId: String, assignee: String?): SELF {
    assertThat(testee.query(TaskForIdQuery(taskId))?.assignee).isEqualTo(assignee)
    return self()
  }

  open fun task_payload_matches(taskId: String, payload: VariableMap): SELF {
    assertThat(testee.query(TaskForIdQuery(taskId))?.payload).isEqualTo(payload)
    return self()
  }

  open fun task_correlations_match(taskId: String, correlations: VariableMap): SELF {
    assertThat(testee.query(TaskForIdQuery(taskId))?.correlations).isEqualTo(correlations)
    return self()
  }

}