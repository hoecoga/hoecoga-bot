package hoecoga.scheduler

import com.google.inject.Injector
import org.quartz.spi.{JobFactory, TriggerFiredBundle}
import org.quartz.{Job, Scheduler}

/**
 * [[SlackJob]] factory.
 * @param injector the injector to inject dependency to [[SlackJob]].
 */
class SlackJobFactory(injector: Injector) extends JobFactory {
  override def newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job = {
    val detail = bundle.getJobDetail
    val cls = detail.getJobClass
    injector.getInstance(cls)
  }
}
