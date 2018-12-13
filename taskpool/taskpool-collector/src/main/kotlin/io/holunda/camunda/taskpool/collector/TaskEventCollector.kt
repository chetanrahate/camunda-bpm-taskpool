package io.holunda.camunda.taskpool.collector

import io.holunda.camunda.taskpool.TaskCollectorProperties
import io.holunda.camunda.taskpool.api.task.*
import io.holunda.camunda.taskpool.extractKey
import io.holunda.camunda.taskpool.loadCaseName
import io.holunda.camunda.taskpool.loadProcessName
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.impl.history.event.HistoricIdentityLinkLogEventEntity
import org.camunda.bpm.engine.impl.history.event.HistoricTaskInstanceEventEntity
import org.camunda.bpm.engine.task.IdentityLinkType
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Collects Camunda events and Camunda historic events (event listener order is {@link TaskEventCollector#ORDER}) and emits Commands
 */
@Component
class TaskEventCollector(
  private val repositoryService: RepositoryService,
  private val collectorProperties: TaskCollectorProperties
) {

  private val logger = LoggerFactory.getLogger(TaskEventCollector::class.java)

  companion object {
    // high order to be later than all other listeners and work on changed entity
    const val ORDER = Integer.MAX_VALUE - 100
  }

  /**
   * Fires create command.
   */
  @Order(ORDER)
  @EventListener(condition = "#task.eventName.equals('create')")
  fun create(task: DelegateTask): CreateTaskCommand =
    CreateTaskCommand(
      id = task.id,
      assignee = task.assignee,
      candidateGroups = task.candidates.filter { it.groupId != null }.map { it.groupId },
      candidateUsers = task.candidates.filter { it.userId != null && it.type == IdentityLinkType.CANDIDATE }.map { it.userId },
      createTime = task.createTime,
      description = task.description,
      dueDate = task.dueDate,
      eventName = task.eventName,
      name = task.name,
      owner = task.owner,
      priority = task.priority,
      formKey = null, // loaded lazily
      taskDefinitionKey = task.taskDefinitionKey,
      businessKey = task.execution.businessKey,
      sourceReference = task.sourceReference(repositoryService, collectorProperties.enricher.applicationName)
    )

  /**
   * Fires complete.
   */
  @Order(ORDER)
  @EventListener(condition = "#task.eventName.equals('complete')")
  fun complete(task: DelegateTask) =
    CompleteTaskCommand(
      id = task.id,
      taskDefinitionKey = task.taskDefinitionKey,
      sourceReference = task.sourceReference(repositoryService, collectorProperties.enricher.applicationName),
      assignee = task.assignee,
      candidateGroups = task.candidates.filter { it.groupId != null }.map { it.groupId },
      candidateUsers = task.candidates.filter { it.userId != null && it.type == IdentityLinkType.CANDIDATE }.map { it.userId },
      createTime = task.createTime,
      description = task.description,
      dueDate = task.dueDate,
      eventName = task.eventName,
      name = task.name,
      owner = task.owner,
      priority = task.priority,
      businessKey = task.execution.businessKey
    )

  /**
   * Fires assign command.
   */
  @Order(ORDER)
  @EventListener(condition = "#task.eventName.equals('assignment')")
  fun assign(task: DelegateTask) =
    AssignTaskCommand(
      id = task.id,
      assignee = task.assignee,
      candidateGroups = task.candidates.filter { it.groupId != null }.map { it.groupId },
      candidateUsers = task.candidates.filter { it.userId != null && it.type == IdentityLinkType.CANDIDATE }.map { it.userId },
      createTime = task.createTime,
      description = task.description,
      dueDate = task.dueDate,
      eventName = task.eventName,
      name = task.name,
      owner = task.owner,
      priority = task.priority,
      taskDefinitionKey = task.taskDefinitionKey,
      formKey = null, // loaded lazily
      businessKey = task.execution.businessKey,
      sourceReference = task.sourceReference(repositoryService, collectorProperties.enricher.applicationName)
    )


  /**
   * Fires delete command.
   */
  @Order(ORDER)
  @EventListener(condition = "#task.eventName.equals('delete')")
  fun delete(task: DelegateTask) =
    DeleteTaskCommand(
      id = task.id,
      taskDefinitionKey = task.taskDefinitionKey,
      sourceReference = task.sourceReference(repositoryService, collectorProperties.enricher.applicationName),
      assignee = task.assignee,
      candidateGroups = task.candidates.filter { it.groupId != null }.map { it.groupId },
      candidateUsers = task.candidates.filter { it.userId != null && it.type == IdentityLinkType.CANDIDATE }.map { it.userId },
      createTime = task.createTime,
      deleteReason = task.deleteReason,
      description = task.description,
      dueDate = task.dueDate,
      eventName = task.eventName,
      name = task.name,
      owner = task.owner,
      priority = task.priority,
      businessKey = task.execution.businessKey
    )

  /**
   * Fires update command.
   * The following attributes of the update event are skipped:
   * <ul>
   *     <li>parentTaskId</li>
   * </ul>
   */
  @Order(ORDER)
  @EventListener
  fun update(changeEvent: HistoricTaskInstanceEventEntity): UpdateTaskCommand? =
    when (changeEvent.eventType) {
      "update" ->
        UpdateAttributeTaskCommand(
          id = changeEvent.taskId,
          taskDefinitionKey = changeEvent.taskDefinitionKey,
          sourceReference = changeEvent.sourceReference(repositoryService, collectorProperties.enricher.applicationName),
          assignee = changeEvent.assignee,
          description = changeEvent.description,
          dueDate = changeEvent.dueDate,
          followUpDate = changeEvent.followUpDate,
          name = changeEvent.name,
          owner = changeEvent.owner,
          priority = changeEvent.priority
        )
      else -> null
    }

  /**
   * Fires update assignment command.
   */
  @Order(ORDER)
  @EventListener
  fun update(changeEvent: HistoricIdentityLinkLogEventEntity): UpdateTaskCommand? =
    when (changeEvent.operationType) {
      "add" -> when {
        changeEvent.userId != null -> AddCandidateUserCommand(
          id = changeEvent.taskId,
          userId = changeEvent.userId)
        changeEvent.groupId != null -> AddCandidateGroupCommand(
          id = changeEvent.taskId,
          groupId = changeEvent.groupId)
        else -> {
          logger.warn("Received unexpected identity link historic update event ${changeEvent.type} ${changeEvent.operationType} ${changeEvent.eventType} on ${changeEvent.taskId}")
          null
        }
      }
      "delete" -> when {
        changeEvent.userId != null -> DeleteCandidateUserCommand(
          id = changeEvent.taskId,
          userId = changeEvent.userId)
        changeEvent.groupId != null -> DeleteCandidateGroupCommand(
          id = changeEvent.taskId,
          groupId = changeEvent.groupId)
        else -> {
          logger.warn("Received unexpected identity link historic update event ${changeEvent.type} ${changeEvent.operationType} ${changeEvent.eventType} on ${changeEvent.taskId}")
          null
        }
      }
      else -> {
        logger.warn("Received unexpected identity link historic update event ${changeEvent.type} ${changeEvent.operationType} ${changeEvent.eventType} on ${changeEvent.taskId}")
        null
      }
    }
}

fun HistoricTaskInstanceEventEntity.sourceReference(repositoryService: RepositoryService, applicationName: String): SourceReference =
  when {
    this.processDefinitionId != null -> ProcessReference(
      definitionId = this.processDefinitionId,
      instanceId = this.processInstanceId,
      executionId = this.executionId,
      definitionKey = this.processDefinitionKey(),
      name = this.processName(repositoryService),
      applicationName = applicationName,
      tenantId = this.tenantId
    )
    this.caseDefinitionId != null -> CaseReference(
      definitionId = this.caseDefinitionId,
      instanceId = this.caseInstanceId,
      executionId = this.caseExecutionId,
      definitionKey = this.caseDefinitionKey(),
      name = this.caseName(repositoryService),
      applicationName = applicationName,
      tenantId = this.tenantId
    )
    else -> throw IllegalArgumentException("No source reference found.")
  }


fun DelegateTask.sourceReference(repositoryService: RepositoryService, applicationName: String): SourceReference =
  when {
    this.processDefinitionId != null -> ProcessReference(
      definitionId = this.processDefinitionId,
      instanceId = this.processInstanceId,
      executionId = this.executionId,
      definitionKey = this.processDefinitionKey(),
      name = this.processName(repositoryService),
      applicationName = applicationName,
      tenantId = this.tenantId
    )
    this.caseDefinitionId != null -> CaseReference(
      definitionId = this.caseDefinitionId,
      instanceId = this.caseInstanceId,
      executionId = this.caseExecutionId,
      definitionKey = this.caseDefinitionKey(),
      name = this.caseName(repositoryService),
      applicationName = applicationName,
      tenantId = this.tenantId
    )
    else -> throw IllegalArgumentException("No source reference found.")
  }


fun DelegateTask.processDefinitionKey(): String = extractKey(this.processDefinitionId)
fun HistoricTaskInstanceEventEntity.processDefinitionKey(): String = extractKey(this.processDefinitionId)
fun DelegateTask.caseDefinitionKey(): String = extractKey(this.caseDefinitionId)
fun HistoricTaskInstanceEventEntity.caseDefinitionKey(): String = extractKey(this.caseDefinitionId)

fun DelegateTask.caseName(repositoryService: RepositoryService) = loadCaseName(this.caseDefinitionId, repositoryService)
fun HistoricTaskInstanceEventEntity.caseName(repositoryService: RepositoryService) = loadCaseName(this.caseDefinitionId, repositoryService)
fun DelegateTask.processName(repositoryService: RepositoryService) = loadProcessName(this.processDefinitionId, repositoryService)
fun HistoricTaskInstanceEventEntity.processName(repositoryService: RepositoryService) = loadProcessName(this.processDefinitionId, repositoryService)


