# <img src="pix/eztime.png" width="50"> eztime

**Time, made simple**

A minimalist wrapper around ZonedDateTime making time-based logic in Scala 🧈✨ smooth like butter.


## Features
- Force correct time handling with type-safe constructors
- Beautiful duration arithmetic that reads like English
- Effortlessly extensible with custom parsers and business rules
- Prevent timezone bugs forever: assume Zulu time, require IANA zones (no raw offsets allowed)
- Drop **EzTime.scala** into your project like a header-file
- Zero dependencies, just pure ZonedDateTime done right

To get started:

```scala
import eztime._
```


## Core Concepts
#### `EzTime`  
A wrapper around ZonedDateTime. You must instantiate an **EzTime** with the `fromString` or `fromStringOrThrow` smart constructors. This prevents invalidate date strings from ever entering your domain.
```scala
val myTime = EzTime.fromString("2024-01-01")
```

#### `EzTimeDuration`
Natural duration syntax, with no headscratching or thinking about pulling in ChronoUnits. You can use singular or plural of all units from `nano(s)` to `year(s)`
```scala
val laterTime = myTime + 3.hours + (30.minutes - 20.seconds)
```

## Timezone Operations
**EzTime** provides 2 distinct ways to handle timezones:
1. `toZone`: Preserves the instant in time, adjusts the timezone

```scala
/* It's 2 PM in London */
val londonTime = EzTime.fromString("2024-03-21T14:00:00+00:00[Europe/London]").get

/* Shows as 3 PM in Paris */
val parisSameInstant = londonTime.toZone("Europe/Paris")
```

2. `atZone`: Changes the wall time to the new timezone
```scala
/* It's 2 PM in London */
val londonTime = EzTime.fromString("2024-03-21T14:00:00+00:00[Europe/London]").get

/* Shows as 2 PM Paris time */
val parisWallTime = londonTime.atZone("Europe/Paris")
```

## Of note
Time is a deceptively complex domain. Java's ZonedDateTime is an excellent foundation - it's well-designed, battle-tested, and handles the complexities of calendars, leap years, and DST. 

Ultimately, **EzTime** is just a zdt wrapper to force teams to code correctly and make working with time logic beautiful.

- **Forced Correctness**: EzTime's smart constructors ensure that invalid timestamps never enter your system. This isn't just about convenience - it's about making invalid states unrepresentable at the type level. The library enforces a powerful 2-step system that eliminates a whole class of timezone bugs
    1. All timestamps are UTC/Zulu unless explicitly specified
    2. Non-UTC times must use IANA identifiers (e.g., "America/New_York") rather than raw offsets

- **Business Logic as Types**: Rather than spreading time-related business logic throughout your codebase, EzTime encourages encapsulating it in type-safe extensions. This means your domain rules about time become part of your type system.

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

### Custom Business Logic

EzTime's extension system lets you encapsulate your domain specific time logic

```scala
object BusinessRules {
  implicit class TradingHours(val time: EzTime) {
    def isWeekend: Boolean = {
      val day = time.zdt.getDayOfWeek
      day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
    }
    
    def isNyseHours: Boolean = {
      if (isWeekend) false 
      else {
        val nyTime = time.toZone("America/New_York")
        val hour = nyTime.zdt.getHour
        hour >= 9 && hour < 16
      }
    }
    
    def nextBusinessDay: EzTime = 
      LazyList.iterate(time + 1.day)(_.plus(1.day))
        .dropWhile(_.isWeekend)
        .head
  }

  implicit class MultiTimeRules(val times: (EzTime, EzTime)) {
    def areOnSameBusinessDay: Boolean = {
      val (t1, t2) = times
      !t1.isWeekend && !t2.isWeekend &&
      t1.toZone("America/New_York").zdt.toLocalDate == 
      t2.toZone("America/New_York").zdt.toLocalDate
    }
  }
}
```

Then just use your business logic naturally as if it were baked into **EzTime**
```scala
import eztime._
import BusinessRules._

val now = EzTime.fromString("2024-03-21T12:30:00Z").get

if (!now.isWeekend && now.isNyseHours) {
  /* Do your logic */
}

val nextDay = now.nextBusinessDay

/* Multi-time logic */
val orderTime = EzTime.fromString("2024-03-21T13:45:00Z").get
val fillTime = EzTime.fromString("2024-03-21T14:30:00Z").get

if ((orderTime, fillTime).areOnSameBusinessDay) {
  /* Do your logic */
}
```

Parse custom date formats by pulling (Seq) of DateTimeFormatter(s) into implicit scope:
```scala
/* Default EzTime can't parse this format */
val chineseTime = "2024年03月21日 15:30"

EzTime.fromString(chineseTime)  /* Returns None */

/* Add custom formatters */
implicit val formatters: Seq[DateTimeFormatter] = Seq(
 DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
)

/* Now they all parse successfully! */
val anotherChineseTime = EzTime.fromString("2024年03月21日 15:30")   /* Some(EzTime(...)) */
```

