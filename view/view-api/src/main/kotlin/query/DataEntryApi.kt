package io.holunda.camunda.taskpool.view.query

import io.holunda.camunda.taskpool.view.query.data.DataEntriesForUserQuery
import io.holunda.camunda.taskpool.view.query.data.DataEntryForIdentityQuery
import io.holunda.camunda.taskpool.view.query.data.DataEntriesQueryResult

/**
 * Defines an API interface for Data Entry Queries.
 */
interface DataEntryApi {

  /**
   * Query data entries for given id.
   * @param query object
   * @return query result.
   */
  fun query(query: DataEntryForIdentityQuery): DataEntriesQueryResult
  /**
   * Query data entries for provided user.
   * @param query object
   * @return query result.
   */
  fun query(query: DataEntriesForUserQuery): DataEntriesQueryResult
}
