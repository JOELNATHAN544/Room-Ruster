# Room-Ruster

Manage weekly work rotations for 8 roommates and post schedules to a Discord channel.

## Requirements

### Option 1: Traditional Setup
- **Java 17+**
- **Maven 3.8+**
- A **Discord Webhook URL** for your server's `#general` channel (or any channel you choose)

### Option 2: Docker (Recommended)
- **Docker** (any recent version)
- A **Discord Webhook URL** for your server's `#general` channel (or any channel you choose)

## Setup

Create a Discord webhook in your server:
1. Go to your Discord server settings → Integrations → Webhooks
2. Create a new webhook and copy its URL
3. Use it in the commands below

## Docker Usage (Recommended - No Java/Maven Required)

### Quick Start: Send Schedule to Discord

If you only have Docker installed (no Java or Maven needed):

```bash
docker run --rm \
  -e DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/YOUR_WEBHOOK_URL" \
  ghcr.io/JOELNATHAN544/room-ruster:latest
```

That's it! The container will automatically send the current week's schedule to Discord.

### Build Docker Image Locally

```bash
docker build -t room-ruster:latest .
```

Then run:
```bash
docker run --rm \
  -e DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/YOUR_WEBHOOK_URL" \
  -v room-ruster-state:/app/state \
  room-ruster:latest
```

## Traditional Setup (Requires Java 17+ and Maven 3.8+)

### Build
```bash
mvn -q -DskipTests package
```
This produces a fat JAR at `target/room-ruster-0.1.0.jar`.

### Usage

**Send the current week's schedule to Discord:**
```bash
export DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/YOUR_WEBHOOK_URL"
java -jar target/room-ruster-0.1.0.jar --send
```

**Print N weeks of schedules:**
```bash
java -jar target/room-ruster-0.1.0.jar --print 12
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

## Why Docker?

- ✅ **No Java/Maven installation** — Docker includes everything
- ✅ **Full isolation** — Runs in a containerized environment
- ✅ **Optimized size** — Final image is ~180MB (multi-stage build with Alpine JRE 17)
  - Build stage (Maven + JDK): discarded after compilation
  - Runtime stage: only contains JRE + JAR (~180MB is the minimum for Java 17)
- ✅ **Persistent state** — Volume mounts preserve `last-posted-week.txt` across restarts
- ✅ **Easy deployment** — Works on any system with Docker installed
- ✅ **Automated CI/CD** — GitHub Actions automatically builds and pushes images

## Example output
```
Room-Ruster — Week 1 (starting 2025-01-06)
Dish Washing (2): Maxwell, Prosper
Room Care (1): Nine
Shoe Washing (3): Nathan, Onel, Derick
Free (1): Severian
```
