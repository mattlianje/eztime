package eztime

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.util.{Try, Success, Failure}
import scala.util.matching._

sealed abstract case class EzTime private (zdt: ZonedDateTime) {
  def inZone(zoneId: String): Option[EzTime] = {
    Try(ZoneId.of(zoneId)).toOption.map(zone =>
      EzTime.unsafeCreate(zdt.withZoneSameInstant(zone))
    )
  }

  def asZone(zoneId: String): Option[EzTime] = {
    Try(ZoneId.of(zoneId)).toOption.map(zone =>
      EzTime.unsafeCreate(zdt.withZoneSameLocal(zone))
    )
  }

  def inZoneOrThrow(zoneId: String): EzTime = {
    inZone(zoneId).getOrElse(
      throw new IllegalArgumentException(s"Invalid zone ID: $zoneId")
    )
  }

  def asZoneOrThrow(zoneId: String): EzTime = {
    asZone(zoneId).getOrElse(
      throw new IllegalArgumentException(s"Invalid zone ID: $zoneId")
    )
  }

  def between(other: EzTime, unit: ChronoUnit = ChronoUnit.NANOS): Long = {
    unit.between(other.zdt, zdt)
  }

  override def toString: String =
    zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)

  def toString(pattern: String): Option[String] = {
    Try(DateTimeFormatter.ofPattern(pattern).format(zdt)).toOption
  }

  def toStringOrThrow(pattern: String): String = {
    toString(pattern).getOrElse(
      throw new IllegalArgumentException(s"Invalid pattern: $pattern")
    )
  }

  def toZdt: ZonedDateTime = zdt
}

object EzTime {
  def unsafeCreate(zdt: ZonedDateTime): EzTime = new EzTime(zdt) {}
  private val defaultFormatters: List[DateTimeFormatter] = List(
    DateTimeFormatter.ISO_ZONED_DATE_TIME,
    DateTimeFormatter.ISO_DATE_TIME,
    DateTimeFormatter.ISO_INSTANT,
    DateTimeFormatter.ofPattern(
      "yyyy-MM-dd[ ]['T'][ ]HH:mm:ss[.SSS][ ][z][ ][Z][ ][XXX][ ][VV]"
    ),
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
  )

  def fromString(s: String)(implicit
      formatter: DateTimeFormatter = null,
      formatters: Seq[DateTimeFormatter] = Seq.empty
  ): Option[EzTime] = {

    Option(s).filter(_.nonEmpty).flatMap { str =>
      val normalized = str.trim
        .replaceAll("\\s*T\\s*", "T")
        .replaceAll("\\s+Z", "Z")

      val allFormatters =
        Option(formatter).toSeq ++ formatters ++ defaultFormatters

      allFormatters.foldLeft[Option[EzTime]](None) { (acc, fmt) =>
        acc.orElse {
          Try {
            val temporal = fmt.parse(normalized)
            val zdt = Try(ZonedDateTime.from(temporal))
              .getOrElse {
                Try(LocalDateTime.from(temporal))
                  .getOrElse(LocalDate.from(temporal).atStartOfDay)
                  .atZone(ZoneOffset.UTC)
              }
            unsafeCreate(zdt)
          }.toOption
        }
      }
    }
  }

  def fromStringOrThrow(s: String)(implicit
      formatter: DateTimeFormatter = null,
      formatters: Seq[DateTimeFormatter] = Seq.empty
  ): EzTime = {
    fromString(s).getOrElse(
      throw new IllegalArgumentException(s"Invalid datetime string: $s")
    )
  }

  def now: EzTime = unsafeCreate(ZonedDateTime.now(ZoneOffset.UTC))

  implicit def toEzTime(zdt: ZonedDateTime): EzTime = unsafeCreate(zdt)
}

object EzTimeDuration {
  implicit class DurationInt(n: Int) {
    private def toDuration(unit: ChronoUnit) = Duration.of(n.toLong, unit)
    private def toPeriod(n: Int) = Period.of(0, 0, n * 7)

    def nano = toDuration(ChronoUnit.NANOS)
    def nanos = nano
    def nanosecond = nano
    def nanoseconds = nano

    def micro = toDuration(ChronoUnit.MICROS)
    def micros = micro
    def microsecond = micro
    def microseconds = micro

    def milli = toDuration(ChronoUnit.MILLIS)
    def millis = milli
    def millisecond = milli
    def milliseconds = milli

    def second = toDuration(ChronoUnit.SECONDS)
    def seconds = second
    def sec = second
    def secs = second

    def minute = toDuration(ChronoUnit.MINUTES)
    def minutes = minute
    def min = minute
    def mins = minute

    def hour = toDuration(ChronoUnit.HOURS)
    def hours = hour

    def day = toDuration(ChronoUnit.DAYS)
    def days = day

    /*
    def week = toDuration(ChronoUnit.WEEKS)
    def weeks = week

    def month = Duration.of(n.toLong, ChronoUnit.MONTHS)
    def months = month

    def year = Duration.of(n.toLong, ChronoUnit.YEARS)
    def years = year
     */

    def week = toPeriod(n)
    def weeks = week

    def month = Period.ofMonths(n)
    def months = month

    def year = Period.ofYears(n)
    def years = year
  }

  implicit class EzTimeWithDuration(ezTime: EzTime) {
    def +(duration: Duration): EzTime =
      EzTime.unsafeCreate(ezTime.zdt.plus(duration))
    def -(duration: Duration): EzTime =
      EzTime.unsafeCreate(ezTime.zdt.minus(duration))

    def +(period: Period): EzTime =
      EzTime.unsafeCreate(ezTime.zdt.plus(period))
    def -(period: Period): EzTime =
      EzTime.unsafeCreate(ezTime.zdt.minus(period))
  }
}
