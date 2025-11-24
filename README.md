# Room-Ruster

Manage weekly work rotations for 8 roommates and post schedules to a Discord channel.

## Requirements
- **Java 17+**
- **Maven 3.8+**
- A **Discord Webhook URL** for your server's `#general` channel (or any channel you choose)

## Setup
- Create a Discord webhook in your server and copy its URL.
- Set environment variables:
```
export DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..."
# Optional: anchor Monday for week 1 (defaults to next Monday from today)
export START_DATE="2025-01-06"
# Optional: daemon post time (HH:mm, default 08:00)
export POST_TIME="08:00"
```

## Build
```
mvn -q -DskipTests package
```
This produces a fat JAR at `target/room-ruster-0.1.0.jar`.

## Usage
- **Print N weeks from the anchor Monday**
```
java -jar target/room-ruster-0.1.0.jar --print 12 --start 2025-01-06
```
- **Send the current week's schedule to Discord**
```
java -jar target/room-ruster-0.1.0.jar --send --start 2025-01-06
```
- **Run as a weekly daemon (posts every Monday at 08:00 by default)**
```
java -jar target/room-ruster-0.1.0.jar --daemon --start 2025-01-06 --time 09:30
```

## Rotation Rules Implemented
- **Dish Washing (2 fixed):** `Maxwell`, `Prosper` (never rotate)
- **Shoe Washing (3 each week):** first 3 of the fixed rotation order
- **Free (1 each week):** 4th in order
- **Room Care (1 each week):** 5th in order, except every 8th week
- **8th Week Rule:** `Frank` takes Room Care; the person who would have taken Room Care gets a free week (2 free people total)
- **Rotation order (fixed and cyclic):** `Nathan`, `Onel`, `Derick`, `Severian`, `Nine`

## How week index is computed
- `week 1` starts at the anchor Monday (`--start` or `START_DATE`).
- Current week for `--send`/`--daemon` is computed based on whole weeks since the anchor Monday.

## Notes
- No external libraries are required for HTTP; uses Java's built-in `HttpClient`.
- The app prints the schedule text before sending when in `--send`/`--daemon` (visible in logs).

## Example output
```
Room-Ruster — Week 1 (starting 2025-01-06)
Dish Washing (2): Maxwell, Prosper
Room Care (1): Nine
Shoe Washing (3): Nathan, Onel, Derick
Free (1): Severian
```
