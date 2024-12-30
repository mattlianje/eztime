import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TestExtensions {
    import java.time._
    import java.time.format.DateTimeFormatter
    import java.time.temporal.ChronoUnit

    implicit class TinyEzTime(val ezTime: EzTime) {
        def isWeekend: Boolean = {
        val day = ezTime.zdt.getDayOfWeek
        day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
        }

        def isAM: Boolean = ezTime.zdt.getHour < 12

        def shortTime: String = ezTime.zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}

class EzTimeTest extends munit.FunSuite {
  import EzTimeDuration._
  
  test("parse valid ISO-8601 datetime strings") {
    val formats = List(
      /* Full ISO formats */
      "2024-03-21T15:30:00+01:00[Europe/Paris]",
      "2024-03-21T15:30:00+01:00",
      "2024-03-21T15:30:00Z",
      "2024-03-21T15:30:00",
      /* With milliseconds ... TODO ... nanos and all other */
      "2024-03-21T15:30:00.123+01:00[Europe/Paris]",
      "2024-03-21T15:30:00.123Z",
      /* TODO handle these
        "2024-03-21 15:30:00+01:00[Europe/Paris]",
        "2024-03-21 15:30:00.123+01:00",
      */
      "2024-03-21 15:30:00",
      "2024-03-21"
    )

    formats.foreach { dt =>
      val parsed = EzTime.fromString(dt)
      assert(parsed.isDefined, s"Failed to parse: $dt")
      
      if (dt.contains("[")) { /* Hack to check that tz is preserved */
        assert(parsed.get.toString.contains(dt.split("\\[")(1).dropRight(1)))
      }
      if (dt.contains("Z")) { /* Verify zulu time is at UTC offset */
        assert(parsed.get.toString.contains("Z") || parsed.get.toString.contains("+00:00"))
      }
    }
  }

  test("handle whitespace variations 1/2") {
    val variations = List(
      "2024-03-21T15:30:00Z",
      "2024-03-21  T  15:30:00Z",
      "2024-03-21 T 15:30:00Z",
      "2024-03-21 15:30:00 Z"
    )

    val expected = EzTime.fromString(variations.head).get
    variations.foreach { dt =>
      val parsed = EzTime.fromString(dt).get
      assertEquals(parsed.toString, expected.toString)
    }
  }

  test("handle whitespace variations 2/2") {
     val variations = List(
       "2024-03-21T15:30:00Z",                  
       "2024-03-21  T  15:30:00Z",             
       "2024-03-21 T 15:30:00Z",              
       "2024-03-21 15:30:00 Z",              
       "2024-03-21T15:30:00 Z",             
       "2024-03-21 T 15:30:00 +01:00",     
       "2024-03-21T15:30:00+01:00[Europe/Paris]", 
       //"2024-03-21 15:30:00 +01:00[Europe/Paris]" 
     )

     variations.foreach { dt =>
       val parsed = EzTime.fromString(dt)
       assert(parsed.isDefined, s"Failed to parse: $dt")
     }

     val zVariations = variations.take(5)
     val first = EzTime.fromString(zVariations.head).get
     zVariations.foreach { dt =>
       val current = EzTime.fromString(dt).get
       assertEquals(current.toString, first.toString)
     }
   }
  test("parse with timezone preservation") {
    val parisTime = EzTime.fromString("2024-03-21T15:30:00+01:00[Europe/Paris]").get
    assert(parisTime.toString.contains("Europe/Paris"))
    
    val utcTime = EzTime.fromString("2024-03-21T15:30:00Z").get
    assert(utcTime.toString.contains("Z") || utcTime.toString.contains("+00:00"))
    
    val noZoneTime = EzTime.fromString("2024-03-21T15:30:00").get
    assert(utcTime.toString.contains("Z") || utcTime.toString.contains("+00:00"))
  }

  test("return None for invalid datetime strings") {
    val invalidFormats = List(
      "not a datetime",
      "",
      " ",
      null,
      "2024",
      "2024-13-21",
      "2024-03-32",
      "2024-03-21 25:00:00",
      "2024-03-21T15:30:00+25:00",
      "15:30:00"
    )

    invalidFormats.foreach { dt =>
      assertEquals(EzTime.fromString(dt), None, s"Should fail to parse: $dt")
    }
  }

  test("throw for invalid datetime strings when using OrThrow variant") {
    val invalidDateTime = "not a datetime"
    intercept[IllegalArgumentException] {
      EzTime.fromStringOrThrow(invalidDateTime)
    }
  }


  test("demonstrate difference between atZone and toZone") {
    val londonTime = EzTime.fromStringOrThrow("2024-03-21T14:00:00+00:00[Europe/London]")
    
    val parisTimeAtZone = londonTime.atZoneOrThrow("Europe/Paris")
    assertEquals(parisTimeAtZone.zdt.getHour, 15)
    
    val parisTimeToZone = londonTime.toZoneOrThrow("Europe/Paris")
    assertEquals(parisTimeToZone.zdt.getHour, 14)
  }

  test("handle various duration units") {
    val baseTime = EzTime.fromStringOrThrow("2024-03-21T15:30:00+01:00[Europe/Paris]")
    
    val withSeconds = baseTime + 30.seconds
    assertEquals(withSeconds.zdt.getSecond, (baseTime.zdt.getSecond + 30) % 60)
    
    val withMinutes = baseTime + 45.minutes
    assertEquals(withMinutes.zdt.getMinute, (baseTime.zdt.getMinute + 45) % 60)
    
    val withHours = baseTime + 2.hours
    assertEquals(withHours.zdt.getHour, (baseTime.zdt.getHour + 2) % 24)
    
    val withDays = baseTime + 5.days
    assertEquals(withDays.zdt.getDayOfMonth, baseTime.zdt.getDayOfMonth + 5)
  }

  test("support case-insensitive unit names") {
    val duration1 = 1.second
    val duration2 = 1.sec
    val duration3 = 1.seconds
    val duration4 = 1.secs
    assertEquals(duration1, duration2)
    assertEquals(duration2, duration3)
    assertEquals(duration3, duration4)
  }

  test("handle negative durations") {
    val baseTime = EzTime.fromStringOrThrow("2024-03-21T15:30:00+01:00[Europe/Paris]")
    val withNegative = baseTime - 30.minutes
    assertEquals(withNegative.zdt.getMinute, (baseTime.zdt.getMinute - 30 + 60) % 60)
  }

  test("mini extensions work") {
    import TestExtensions._
    
    val weekday = EzTime.fromString("2024-03-21T15:30:00Z").get /* Thursday */
    val weekend = EzTime.fromString("2024-03-23T15:30:00Z").get /* Saturday */
    val morning = EzTime.fromString("2024-03-21T09:30:00Z").get
    
    assert(!weekday.isWeekend)
    assert(weekend.isWeekend)
    assert(morning.isAM)
    assert(!weekday.isAM)
    assertEquals(morning.shortTime, "09:30")
  }

  test("use single implicit formatter") {
    implicit val americanFormat: DateTimeFormatter = 
      DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")

    val time = EzTime.fromString("03/21/2024 15:30")
    assert(time.isDefined)
  }

  test("use multiple implicit formatters") {
    implicit val formatters: Seq[DateTimeFormatter] = Seq(
      DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    )

    val times = List(
      "03/21/2024 15:30",
      "21-03-2024 15:30"
    )

    times.foreach { time =>
      assert(EzTime.fromString(time).isDefined, s"Failed to parse: $time")
    }
  }

  test("calculate time between in various units") {
    val time1 = EzTime.fromStringOrThrow("2024-03-21T14:30:00Z")
    val time2 = EzTime.fromStringOrThrow("2024-03-21T16:45:30Z")
    
    assertEquals(time2.between(time1, ChronoUnit.HOURS), 2L)
    assertEquals(time2.between(time1, ChronoUnit.MINUTES), 135L)
    assertEquals(time2.between(time1, ChronoUnit.SECONDS), 8130L)
    
    assertEquals(time1.between(time2, ChronoUnit.MINUTES), -135L)
  }

  test("toString with custom patterns") {
    val time = EzTime.fromStringOrThrow("2024-03-21T15:30:45Z")
    
    assertEquals(time.toString("yyyy年MM月dd日"), Some("2024年03月21日"))
    assertEquals(time.toString("HH時mm分ss秒"), Some("15時30分45秒"))
    assertEquals(time.toString("dd/MM/yyyy"), Some("21/03/2024"))
  }

  test("handle invalid patterns") {
    val time = EzTime.fromStringOrThrow("2024-03-21T15:30:45Z")
    
    assert(time.toString("invalid pattern").isEmpty)
    intercept[IllegalArgumentException] {
      time.toStringOrThrow("invalid pattern")
    }
  }

  test("convert between EzTime and ZonedDateTime") {
    val originalZdt = ZonedDateTime.parse("2024-03-21T15:30:00Z")
    
    val implicitEz: EzTime = originalZdt
    val explicitEz = EzTime.toEzTime(originalZdt)
    
    val backToZdt = explicitEz.toZdt
    
    assertEquals(implicitEz.toString, explicitEz.toString)
    assertEquals(backToZdt, originalZdt)
  }

  test("maintain timezone information during conversion") {
    val parisZdt = ZonedDateTime.parse("2024-03-21T15:30:00+01:00[Europe/Paris]")
    
    val implicitEz: EzTime = parisZdt
    val explicitEz = EzTime.toEzTime(parisZdt)
    
    assertEquals(implicitEz.toZdt.getZone.getId, "Europe/Paris")
    assertEquals(explicitEz.toZdt.getZone.getId, "Europe/Paris")
    assertEquals(explicitEz.toZdt.getOffset.toString, "+01:00")
  }
}
