package io.holunda.camunda.taskpool.core.task

import io.holunda.camunda.taskpool.api.business.addCorrelation
import io.holunda.camunda.taskpool.api.business.newCorrelations
import io.holunda.camunda.taskpool.api.task.*
import org.axonframework.test.aggregate.AggregateTestFixture
import org.camunda.bpm.engine.variable.Variables
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Checks assignment engine commands and eventing of it.
 */
class TaskAssignmentChangeTest {

  private val fixture: AggregateTestFixture<TaskAggregate> = AggregateTestFixture<TaskAggregate>(TaskAggregate::class.java)
  private val processReference = ProcessReference(
    definitionKey = "process_key",
    instanceId = "0815",
    executionId = "12345",
    definitionId = "76543",
    name = "My process",
    applicationName = "myExample"
  )

  private lateinit var now: Date
  private lateinit var createdEvent: TaskCreatedEngineEvent
  private lateinit var deletedEvent: TaskDeletedEngineEvent
  private lateinit var completedEvent: TaskCompletedEngineEvent

  @Before
  fun setUp() {
    now = Date()

    createdEvent = TaskCreatedEngineEvent(
      id = "4711",
      name = "Foo",
      createTime = now,
      owner = "kermit",
      taskDefinitionKey = "foo",
      formKey = "some",
      businessKey = "business123",
      sourceReference = processReference,
      candidateUsers = listOf("kermit"),
      candidateGroups = listOf("muppetshow"),
      assignee = null,
      priority = 51,
      description = "Funky task",
      payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
      correlations = newCorrelations().addCorrelation("Request", "business123")
    )
    deletedEvent = TaskDeletedEngineEvent(
      id = "4711",
      name = "Foo",
      createTime = now,
      owner = "kermit",
      taskDefinitionKey = "foo",
      formKey = "some",
      businessKey = "business123",
      sourceReference = processReference,
      deleteReason = "Test delete"
    )
    completedEvent = TaskCompletedEngineEvent(
      id = "4711",
      name = "Foo",
      createTime = now,
      owner = "kermit",
      taskDefinitionKey = "foo",
      formKey = "some",
      businessKey = "business123",
      sourceReference = processReference
    )
  }

  @Test
  fun `should event added candidate group`() {
    fixture
      .given(createdEvent)
      .`when`(
        AddCandidateGroupCommand(
          id = "4711",
          groupId = "nasa"
        )
      ).expectEvents(
        TaskCandidateGroupChanged(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignmentUpdateType = CamundaTaskEvent.CANDIDATE_GROUP_ADD,
          groupId = "nasa"
        )
      )
  }

  @Test
  fun `should event added candidate user`() {
    fixture
      .given(createdEvent)
      .`when`(
        AddCandidateUserCommand(
          id = "4711",
          userId = "rocketman"
        )
      ).expectEvents(
        TaskCandidateUserChanged(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignmentUpdateType = CamundaTaskEvent.CANDIDATE_USER_ADD,
          userId = "rocketman"
        )
      )
  }

  @Test
  fun `should event removed candidate group`() {
    fixture
      .given(createdEvent)
      .`when`(
        DeleteCandidateGroupCommand(
          id = "4711",
          groupId = "muppetshow"
        )
      ).expectEvents(
        TaskCandidateGroupChanged(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignmentUpdateType = CamundaTaskEvent.CANDIDATE_GROUP_DELETE,
          groupId = "muppetshow"
        )
      )
  }

  @Test
  fun `should event removed candidate user`() {
    fixture
      .given(createdEvent)
      .`when`(
        DeleteCandidateUserCommand(
          id = "4711",
          userId = "kermit"
        )
      ).expectEvents(
        TaskCandidateUserChanged(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignmentUpdateType = CamundaTaskEvent.CANDIDATE_USER_DELETE,
          userId = "kermit"
        )
      )
  }

  @Test
  fun `should not event on completed task`() {
    fixture
      .given(createdEvent, completedEvent)
      .`when`(AddCandidateUserCommand(
        id = "4711",
        userId = "rocketman"
      )).expectNoEvents()
    fixture
      .given(createdEvent, completedEvent)
      .`when`(DeleteCandidateUserCommand(
        id = "4711",
        userId = "kermit"
      )).expectNoEvents()
    fixture
      .given(createdEvent, completedEvent)
      .`when`(DeleteCandidateGroupCommand(
        id = "4711",
        groupId = "muppetshow"
      )).expectNoEvents()
    fixture
      .given(createdEvent, completedEvent)
      .`when`(AddCandidateGroupCommand(
        id = "4711",
        groupId = "nasa"
      )).expectNoEvents()


  }

  @Test
  fun `should not event on deleted task`() {

    fixture
      .given(createdEvent, deletedEvent)
      .`when`(AddCandidateUserCommand(
        id = "4711",
        userId = "rocketman"
      )).expectNoEvents()
    fixture
      .given(createdEvent, deletedEvent)
      .`when`(DeleteCandidateUserCommand(
        id = "4711",
        userId = "kermit"
      )).expectNoEvents()
    fixture
      .given(createdEvent, deletedEvent)
      .`when`(DeleteCandidateGroupCommand(
        id = "4711",
        groupId = "muppetshow"
      )).expectNoEvents()
    fixture
      .given(createdEvent, deletedEvent)
      .`when`(AddCandidateGroupCommand(
        id = "4711",
        groupId = "nasa"
      )).expectNoEvents()

  }

}