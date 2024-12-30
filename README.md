# <img src="pix/eztime.png" width="50"> eztime

**Time, made simple**

**EzTime** is a simple, powerful, zero-dependency wrapper around [ZonedDateTime](https://docs.oracle.com/javase/8/docs/api/java/time/ZonedDateTime.html) to make working with your time-based logic in Scala 🧈✨ smooth like butter

## Features
- Smart constructor with `fromString`
- Effortlessly add new formats to be parsed with `fromString`
- Never worry about faffing around with DateTimeFormatters
- Durations are like beautiful arithmetic

To get started:

```scala
import eztime._
```


## Core Concepts

#### `EzTime`
A wrapper around ZonedDateTime. You must instantiate an **EzTime** with the `fromString` or `fromStringOrThrow`
smart constructors. This prevents invalidate date strings from ever entering your domain.

```scala
val myTime = EzTime.fromString("2024-01-01")
```

#### `EzTimeDuration`
Natural duration syntax, with no headscratching or thinking about pulling in ChronoUnits
:

## Of note
Time is a deceptively complex domain. Java's ZonedDateTime is an excellent foundation - it's well-designed, battle-tested, and handles the complexities of calendars, leap years, and DST. Ultimately **EzTime** is just a zdt wrapper to stop
teams from foot-gunning themselves and making a disgraceful timestamp mess.  

- **Forced Correctness**: EzTime's smart constructors ensure that invalid timestamps never enter your system. This isn't just about convenience - it's about making invalid states unrepresentable at the type level. The library enforces a powerful 2-step system that eliminates a whole class of timezone bugs:
   - (Step 1/2) All timestamps are assumed to be UTC/Zulu time unless explicitly specified otherwise ...
   - (Step 2/2) When specified otherwise, you must use IANA timezone identifiers (like "America/New_York") rather than raw offsets.

- **Business Logic as Types**: Rather than spreading time-related business logic throughout your codebase, EzTime encourages encapsulating it in type-safe extensions. This means your domain rules about time (trading hours, business days, etc.) become part of your type system.

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
    def isTradingHour: Boolean = {
      if (time.isWeekend) false 
      else {
        val nyTime = time.toZone("America/New_York")
        val hour = nyTime.zdt.getHour
        hour >= 9 && hour < 16 /* NYSE hours */
      }
    }
    
    def isMarketOpen: Boolean = {
      if (!isTradingHour) false
      else !isHoliday
    }
    
    def nextTradingDay: EzTime = {
      var next = time + 1.day
      while (next.isWeekend || isHoliday(next)) {
        next = next + 1.day
      }
      next
    }
  }
}
```

Then just use your business logic as if it were baked into **EzTime**



### Examples

Smart parsing that just works:
```scala
val time = EzTime.fromString("2024-03-21T15:30:00+01:00[Europe/Paris]")
val simpleTime = EzTime.fromString("2024-03-21 15:30")
```

Effortlessly handle timezones:
```scala
val nyTime = EzTime.fromString("2024-03-21T10:00:00-04:00[America/New_York]").get
val tokyoTime = nyTime.toZoneOrThrow("Asia/Tokyo")     /* Same instant, Tokyo timezone */
val londonTime = nyTime.atZoneOrThrow("Europe/London") /* Convert wall time to London */

val laterInNY = nyTime + 3.hours
val evenLater = laterInNY + 30.minutes 
```
