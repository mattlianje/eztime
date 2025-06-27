/*
 * +==========================================================================+
 * |                                EzTime                                    |
 * |              Simple, intuitive datetime handling for Scala               |
 * |                 Compatible with Scala 2.12, 2.13, and 3                  |
 * |                                                                          |
 * | Apache License 2.0                                                       |
 * |                                                                          |
 * | A zero-dependency datetime library that makes working with time zones,   |
 * | parsing, and formatting as easy as possible. Built on Java Time API.     |
 * |                                                                          |
 * | Usage: import eztime._                                                   |
 * +==========================================================================+
 */
package object eztime {
  import java.time._
  import java.time.format.DateTimeFormatter
  import java.time.temporal.ChronoUnit
  import scala.util.{Try, Success, Failure}
  import scala.util.matching._

  sealed abstract case class EzTime private (zdt: ZonedDateTime) {

    // ===== Zone Conversion Methods =====

    /** Converts this time to a different timezone (same instant, different zone
      * display). Use this when you want to see what time it was/will be in
      * another timezone.
      *
      * Example: 2024-01-01T12:00:00-08:00 (PST) -> toZone("UTC") ->
      * 2024-01-01T20:00:00Z (UTC) The actual moment in time is preserved, just
      * displayed in a different timezone.
      */
    def toZone(zoneId: String): Option[EzTime] = {
      Try(ZoneId.of(zoneId)).toOption.map(zone =>
        EzTime.unsafeCreate(zdt.withZoneSameInstant(zone))
      )
    }

    /** Treats this time as if it were in a different timezone (same local time,
      * different zone). Use this when you want to reinterpret the local time as
      * being in a different timezone.
      *
      * Example: 2024-01-01T12:00:00-08:00 (PST) -> atZone("UTC") ->
      * 2024-01-01T12:00:00Z (UTC) The local time stays the same (12:00), but
      * now it's interpreted as UTC instead of PST.
      */
    def atZone(zoneId: String): Option[EzTime] = {
      Try(ZoneId.of(zoneId)).toOption.map(zone =>
        EzTime.unsafeCreate(zdt.withZoneSameLocal(zone))
      )
    }

    def toZoneOrThrow(zoneId: String): EzTime = {
      toZone(zoneId).getOrElse(
        throw new IllegalArgumentException(s"Invalid zone ID: $zoneId")
      )
    }

    def atZoneOrThrow(zoneId: String): EzTime = {
      atZone(zoneId).getOrElse(
        throw new IllegalArgumentException(s"Invalid zone ID: $zoneId")
      )
    }

    // ===== Duration and Comparison Methods =====

    def between(other: EzTime, unit: ChronoUnit = ChronoUnit.NANOS): Long = {
      unit.between(other.zdt, zdt)
    }

    // ===== Format Getters =====

    /** Default ISO format with timezone:
      * "2024-03-21T15:30:00+01:00[Europe/Paris]"
      */
    override def toString: String =
      zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)

    /** Custom pattern formatting with error handling
      */
    def toString(pattern: String): Option[String] = {
      Try(DateTimeFormatter.ofPattern(pattern).format(zdt)).toOption
    }

    def toStringOrThrow(pattern: String): String = {
      toString(pattern).getOrElse(
        throw new IllegalArgumentException(s"Invalid pattern: $pattern")
      )
    }

    /** Date only: "2024-03-21"
      */
    def getYmdString: String =
      zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    /** Date and time without timezone: "2024-03-21 15:30:00"
      */
    def getYmdHmsString: String =
      zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    /** Date and time with timezone offset: "2024-03-21 15:30:00 +01:00"
      */
    def getYmdHmsTzString: String =
      zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX"))

    /** ISO format with timezone: "2024-03-21T15:30:00+01:00"
      */
    def getIsoTzString: String =
      zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))

    /** ISO format without timezone: "2024-03-21T15:30:00"
      */
    def getIsoString: String =
      zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

    /** ISO format with microseconds and timezone:
      * "2024-03-21T15:30:00.123456+01:00"
      */
    def getIsoMicrosecondsTzString: String =
      zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"))

    /** ISO format with microseconds: "2024-03-21T15:30:00.123456"
      */
    def getIsoMicrosecondsString: String =
      zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"))

    /** Short time format: "15:30"
      */
    def getShortTimeString: String =
      zdt.format(DateTimeFormatter.ofPattern("HH:mm"))

    /** Short time with seconds: "15:30:45"
      */
    def getTimeString: String =
      zdt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    /** Year only: "2024"
      */
    def getYearString: String = zdt.format(DateTimeFormatter.ofPattern("yyyy"))

    /** Month only: "03"
      */
    def getMonthString: String = zdt.format(DateTimeFormatter.ofPattern("MM"))

    /** Day only: "21"
      */
    def getDayString: String = zdt.format(DateTimeFormatter.ofPattern("dd"))

    // ===== Component Access =====

    def getYear: Int = zdt.getYear
    def getMonth: Int = zdt.getMonthValue
    def getDay: Int = zdt.getDayOfMonth
    def getHour: Int = zdt.getHour
    def getMinute: Int = zdt.getMinute
    def getSecond: Int = zdt.getSecond

    // ===== Conversion Methods =====

    def toZdt: ZonedDateTime = zdt
    def toLocalDate: LocalDate = zdt.toLocalDate
    def toLocalDateTime: LocalDateTime = zdt.toLocalDateTime
    def toInstant: Instant = zdt.toInstant
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

  // ===== Duration DSL - Available automatically when importing eztime._ =====

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
