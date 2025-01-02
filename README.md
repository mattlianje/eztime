<p align="center">
  <img src="pix/eztime-demo.png" width="700">
</p>

# <img src="pix/eztime.png" width="50"> eztime

**Time, made simple**

A minimalist, zero-dependency wrapper around ZonedDateTime making time-based logic in Scala 🧈✨ smooth like butter.


## Features
- Forces correct time handling with type-safe constructors
- Duration arithmetic that reads like English
- Effortlessly extensible with custom parsers and business rules
- Prevent timezone bugs forever: assume Zulu time, require IANA zones
- Drop **EzTime.scala** into your project like a header-file

**EzTime** is on MavenCentral
```scala
"xyz.matthieucourt" % "eztime_2.13" % "0.0.3"
```

Try it in your repl:
```bash
scala-cli repl --dep xyz.matthieucourt:eztime_2.13:0.0.3
```

To get started:

```scala
import eztime._
```

And to use the **EzTime** duration implicits:
```scala
import eztime.EzTimeDuration._
```


## Core Concepts
#### `EzTime`  
A wrapper around ZonedDateTime. You must instantiate an **EzTime** with the `fromString` or `fromStringOrThrow` smart constructors. This prevents invalidate date strings from ever entering your domain.
```scala
val myTime = EzTime.fromString("2024-01-01")
```
`now` always gives the current UTC time (no faffing about with server/local times):
```scala
EzTime.now
```

#### `EzTimeDuration`
Natural duration syntax, with no headscratching or thinking about pulling in ChronoUnits. You can use singular or plural of all units from `nano(s)` to `year(s)`
```scala
val laterTime = myTime + 3.days + 9.secs - 4.nanos
```

EzTime supports these duration units:

| Unit          | Variations                        | Example Usage |
|--------------|-----------------------------------|---------------|
| Nanoseconds  | nano, nanos, nanosecond(s)       | `5.nanos`     |
| Microseconds | micro, micros, microsecond(s)    | `10.micros`   |
| Milliseconds | milli, millis, millisecond(s)    | `100.millis`  |
| Seconds      | second, seconds, sec, secs       | `30.seconds`  |
| Minutes      | minute, minutes, min, mins       | `5.minutes`   |
| Hours        | hour, hours                      | `2.hours`     |
| Days         | day, days                        | `7.days`      |
| Weeks        | week, weeks                      | `2.weeks`     |
| Months       | month, months                    | `3.months`    |
| Years        | year, years                      | `1.year`      |

## Timezone Operations
**EzTime** provides 2 distinct ways to handle timezones:

1. `inZone`: Changes the wall time to the new timezone
```scala
/* It's 2 PM in London */
val londonTime = EzTime.fromString("2024-03-21T14:00:00+00:00[Europe/London]").get

/* Shows as 3 PM Paris time */
val parisWallTime = londonTime.inZone("Europe/Paris")
```

2. `asZone`: Preserves the instant in time, adjusts the timezone
```scala
/* It's 2 PM in London */
val londonTime = EzTime.fromString("2024-03-21T14:00:00+00:00[Europe/London]").get

/* Shows as 2 PM in Paris */
val parisSameInstant = londonTime.asZone("Europe/Paris")
```

## Adding Business Logic and Formatters
**EzTime**'s extension system lets you encapsulate your domain specific time logic

```scala
object BusinessRules {
  implicit class TradingHours(val time: EzTime) {
    import eztime.EzTimeDuration._
    import java.time._

    def isWeekend: Boolean = {
      val day = time.zdt.getDayOfWeek
      day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
    }
    
    def isNyseHours: Boolean = {
      if (isWeekend) false 
      else {
        val nyTime = time.toZoneOrThrow("America/New_York")
        val hour = nyTime.zdt.getHour
        hour >= 9 && hour < 16
      }
    }
    
    def nextBusinessDay: EzTime = 
      LazyList.iterate(time + 1.day)(_ + 1.day)
        .dropWhile(_.isWeekend)
        .head
  }
}
```

Then just use your business logic naturally as if it were baked into **EzTime**
```scala
import BusinessRules._

val now = EzTime.fromString("2024-03-21T12:30:00Z").get

if (!now.isWeekend && now.isNyseHours) {
  println("Doing my business logic")
}

val nextDay = now.nextBusinessDay + 1.day
println(s"next-day + 1: ${nextDay} - the power of EzTime + EzTimeDuration!")
```

Add custom formats that `fromString` will handle:
```scala
object MyEzTimeExtensions {
    import java.time.format._

    implicit val myFormatters: Seq[DateTimeFormatter] = Seq(
     DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
    )
}
```
Initially this will give None:
```scala
val chineseTime = "2024年03月21日 15:30"
EzTime.fromString(chineseTime) /* None */
```
But import your **EzTime** extensions and tada!
```scala
import MyEzTimeExtensions._

EzTime.fromString(chineseTime) /* Some(2024-03-21T15:30:00Z) */
```

You can get a formatted string of your **EzTime** with toString / toStringOrThrow:
```scala
val myEzt = EzTime.fromString("2024-01-10").get

myEzt.toString                             /* Returns default: 2024-01-10T00:00:00Z */
val formattedString: String =
    myEzt.toStringOrThrow("yyyy年MM月dd日") /* Returns: 2024年01月10日 */
```

## Date String Formats
EzTime supports parsing a variety of date-time formats out of the box:

| Format | Example | Notes |
|--------|---------|-------|
| Full ISO with zone | `2024-03-21T15:30:00+01:00[Europe/Paris]` | Preserves timezone |
| ISO with offset | `2024-03-21T15:30:00+01:00` | Preserves offset |
| UTC/Zulu time | `2024-03-21T15:30:00Z` | |
| ISO without zone | `2024-03-21T15:30:00` | Assumes UTC |
| With milliseconds | `2024-03-21T15:30:00.123Z` | |
| With ms and offset | `2024-03-21T15:30:00.123+01:00` | |
| Space separator | `2024-03-21 15:30:00` | Space instead of T |
| Date only | `2024-03-21` | Assumes 00:00:00 UTC |
| Flexible spacing | `2024-03-21  T  15:30:00Z` | Extra spaces allowed |

All formats without an explicit timezone default to UTC. Custom formats can be added via implicit formatters.

## Of note
Time is a deceptively complex domain. Java's ZonedDateTime is an excellent foundation - it's well-designed, battle-tested, and handles the complexities of calendars, leap years, and DST. 

Ultimately, **EzTime** is just a zdt wrapper to force teams to code correctly and make working with time logic beautiful.

- **Forced Correctness**: EzTime's smart constructors ensure that invalid timestamps never enter your system. This isn't just about convenience - it's about making invalid states unrepresentable at the type level. The library enforces a powerful 2-step system that eliminates a whole class of timezone bugs
    1. All timestamps are UTC/Zulu unless explicitly specified
    2. Non-UTC times must use IANA identifiers (e.g., "America/New_York") rather than raw offsets

- **Business Logic as Types**: Rather than spreading time-related business logic throughout your codebase, **EzTime** encourages encapsulating it in type-safe extensions. This means your domain rules about time become part of your type system.

Consider this common bug:
```scala
/* Without EzTime - Subtle bug! */
val timestamp = "2024-03-21 15:30"                  /* Which timezone? Server time? UTC? User's local time */
val dateTime = LocalDateTime.parse(timestamp)       /* Silent assumption about format */
val zoned = dateTime.atZone(ZoneId.systemDefault()) /* Dangerous implicit conversion */

/* With EzTime - Explicit and safe */
val time = EzTime.fromStringOrThrow("2024-03-21 15:30")
val nyTime = time.toZoneOrThrow("America/New_York") /* Explicit about our intentions */
```
