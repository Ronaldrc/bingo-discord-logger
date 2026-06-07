<p style="center">
  <img src="img.png" alt="Lil Brozzer mug shot"/>
</p>

# Bingo Discord Logger

Sends bingo-relevant drops straight to a Discord channel via webhook. When you receive
loot containing an item on your configured bingo list, the plugin posts a rich embed —
with an optional in-game screenshot — to one Discord webhook.

Built for clan bingo events: point everyone at the same shared item list, and drops are
logged automatically as they happen.

## Features

- **Automatic drop logging** — watches loot events and posts only the items on your bingo list.
- **Shared, centrally-managed item list** — link a published Google Sheet (CSV) and everyone
  using the same link tracks the same items. Update the sheet and all clients pick it up
  (refreshed every 15 minutes).
- **Optional screenshots** — attach a screenshot of the moment the drop landed.

Noted and placeholder item variants are resolved to their canonical IDs, so you only need to
list the base item. PvP (player-kill) loot is ignored.

## Setup

1. In Discord: **Server Settings → Integrations → Webhooks → New Webhook**, then **Copy Webhook URL**.
2. In RuneLite, open the **Bingo Discord Logger** plugin settings.
3. Paste the webhook URL into **Discord Webhook URL**. To post to multiple channels, put each
   webhook URL on its own line.
4. Configure which items count as bingo drops (see below).

### Configuration

| Setting | Description |
| --- | --- |
| **Send Screenshot** | Attach a screenshot of the next frame when a drop is logged. On by default. |
| **Discord Webhook URL** | One or more Discord webhook URLs, one per line. |
| **Bingo List URL** | Link to a published Google Sheet (CSV) whose first column is item IDs. Shared across everyone using the same link. Leave blank to disable. |

#### Using a Google Sheet

1. Put the item IDs in the **first column**, one per row. Other columns (names, notes) are ignored.
   Header rows and non-numeric rows are skipped automatically.
2. **File → Share → Publish to web**, choose the sheet, and select **Comma-separated values (.csv)**.
3. Paste the published CSV link into **Bingo List URL**.

> **Privacy note:** the Bingo List URL feature fetches from a third-party server (e.g. Google),
> which submits your IP address to a server not controlled or verified by the RuneLite developers.
> It is opt-in and only active when a URL is set.

Item IDs can be looked up on the [OSRS Wiki](https://oldschool.runescape.wiki/) or via RuneLite's
item search.

## How it works

When a matching drop is received, the plugin builds a single `multipart/form-data` POST containing:

- a Discord **embed** (item list, source, source type, player, timestamp, optional screenshot), and
- a machine-readable `bingo` payload (player, structured item data),

so the same message renders nicely for people **and** can be consumed by a downstream bot.

## Development

See [`CLAUDE.md`](CLAUDE.md) and [`AGENTS.md`](AGENTS.md) for build commands and the RuneLite
plugin development rules this project follows.

## License

BSD 2-Clause. See [`LICENSE`](LICENSE).
