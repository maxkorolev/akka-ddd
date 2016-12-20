package pl.newicom.dddd

import com.trueaccord.scalapb.TypeMapper
import org.joda.time.DateTime

package object messaging {

  implicit val dateTimeMapper: TypeMapper[Long, DateTime] = TypeMapper(applyDateTime)(unapplyDateTime)

  private def applyDateTime(millis: Long): DateTime = new DateTime(millis)
  private def unapplyDateTime(dt: DateTime): Long = dt.getMillis
}
