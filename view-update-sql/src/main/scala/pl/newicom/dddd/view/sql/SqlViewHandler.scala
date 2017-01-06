package pl.newicom.dddd.view.sql

import akka.Done
import com.typesafe.config.Config
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.view.ViewHandler
import slick.dbio.DBIOAction.sequence
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class SqlViewHandler(override val config: Config, override val vuConfig: SqlViewUpdateConfig)
                    (implicit val profile: JdbcProfile, ex: ExecutionContext)
  extends ViewHandler(vuConfig) with SqlViewStoreConfiguration with FutureHelpers {

  private lazy val viewMetadataDao = new ViewMetadataDao

  def viewMetadataId = ViewMetadataId(viewName, vuConfig.office.id)

  def handle(eventMessage: EventMessage, eventNumber: Long): Future[Done] =
    viewStore.run {
      sequence(vuConfig.projections.map(_.consume(eventMessage))) >>
      viewMetadataDao.insertOrUpdate(viewMetadataId, eventNumber)
    }.mapToDone

  def lastEventNumber: Future[Option[Long]] =
    viewStore.run {
      viewMetadataDao.lastEventNr(viewMetadataId)
    }

}
