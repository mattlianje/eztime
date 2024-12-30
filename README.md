# <img src="pix/eztime.png" width="50"> eztime

**Time, made simple**

**EzTime** is a simple, zero-dependency, waffer-thin wrapper around JVM ZonedDateTime to make working with your time-based logic in Scala a beauty and pleasure.

## Features
- Smart constructor with `fromString`
- Effortlessly add new formats to be parsed with `fromString`
- Never worry about faffing around with DateTimeFormatters
- Durations are like beautiful arithmetic

To get started:

```scala
import eztime._
```


### Core Concepts

#### `EzTime`
A wrapper around ZonedDateTime. You must instantiate an **EzTime** with the `fromString` or `fromStringOrThrow`
smart constructors. This prevents invalidate date strings from ever entering your domain.

```scala
val myTime = EzTime.fromString("2024-01-01")
```

#### `EzTimeDuration`
Natural duration syntax, with no headscratching or thinking about pulling in ChronoUnits

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
val tokyoTime = nyTime.toZoneOrThrow("Asia/Tokyo") // Same instant, Tokyo timezone
val londonTime = nyTime.atZoneOrThrow("Europe/London") // Convert wall time to London

val laterInNY = nyTime + 3.hours
val evenLater = laterInNY + 30.minutes 
```
