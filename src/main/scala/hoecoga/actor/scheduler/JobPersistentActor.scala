package hoecoga.actor.scheduler

import akka.actor.Props
import akka.persistence.{PersistentActor, RecoveryCompleted, RecoveryFailure, SnapshotOffer}
import hoecoga.actor.scheduler.JobPersistentActor._
import hoecoga.scheduler.JobData
import hoecoga.slack.SlackChannel

/**
 * A persistent actor for [[JobData]].
 */
class JobPersistentActor(settings: JobPersistentActorSettings) extends PersistentActor {
  import settings._

  private[this] var state: State = State(List.empty)

  private[this] def insert(event: InsertEvent) = state = state.copy(jobs = event.data :: state.jobs)

  private[this] def delete(event: DeleteEvent) = state = state.copy(
    jobs = state.jobs.filterNot(job => job.id == event.id && job.channel == event.channel))

  override def receiveRecover: Receive = {
    case RecoveryCompleted => onRecoveryCompleted(state.jobs)

    case RecoveryFailure => onRecoveryFailure()

    case e: InsertEvent => insert(e)

    case e: DeleteEvent => delete(e)

    case SnapshotOffer(meta, snapshot: State) => state = snapshot
  }

  override def receiveCommand: Receive = {
    case Insert(job) =>
      persist(InsertEvent(job))(insert)

    case Delete(channel, jobId) =>
      persist(DeleteEvent(channel, jobId))(delete)

    case SaveSnapshot => saveSnapshot(state)

    case Query(channel) =>
      sender() ! QueryResult(channel, state.jobs.filter(_.channel == channel))
  }

  override def persistenceId: String = id
}

object JobPersistentActor {
  private case class State(jobs: List[JobData])

  /**
   * An incoming order to insert `data` into the state of [[JobPersistentActor]].
   */
  case class Insert(data: JobData)

  private case class InsertEvent(data: JobData)

  /**
   * An incoming order to delete [[JobData]] from the state of [[JobPersistentActor]].
   */
  case class Delete(channel: SlackChannel, id: String)

  private case class DeleteEvent(channel: SlackChannel, id: String)

  /**
   * An incoming order to save a snapshot.
   */
  case object SaveSnapshot

  /**
   * An incoming order to retrieve all [[JobData]] on `channel`.
   */
  case class Query(channel: SlackChannel)

  /**
   * A result of [[Query]].
   */
  case class QueryResult(channel: SlackChannel, jobs: List[JobData])

  /**
   * @param id the persistence id.
   */
  case class JobPersistentActorSettings(id: String, onRecoveryCompleted: List[JobData] => Unit, onRecoveryFailure: () => Unit)

  def props(settings: JobPersistentActorSettings): Props = Props(new JobPersistentActor(settings))
}
