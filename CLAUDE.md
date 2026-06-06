# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

`AGENTS.md` (imported above) holds the general RuneLite-plugin rules — threading, HTTP/JSON, config, packaging, and the Jagex/RuneLite hub restrictions that get plugins rejected. Read it; it governs how code here must be written. The notes below cover only what is specific to *this* plugin.

## Architecture

A single-purpose plugin: it watches loot drops, keeps the ones on a configured "bingo" item list, and POSTs them to one or more Discord webhooks.

**Data flow** (`BingoDiscordLoggerPlugin`):
1. `@Subscribe onLootReceived` fires on RuneLite's `LootReceived` event. PvP (`LootRecordType.PLAYER`) loot is dropped; everything else is checked against the configured item set.
2. `processLoot` matches each `ItemStack` against the bingo set (using `itemManager.canonicalize(id)` so noted/placeholder variants resolve to the canonical ID), and builds a `WebhookBody`.
3. `DiscordWebhookClient.send` POSTs as `multipart/form-data` via the injected `OkHttpClient` (async `enqueue`), once per webhook URL.

**Other files**
1. `parseCsv` in Utils extracts item_id from an CSV file containing item_id and item names
2. `Class BingoItemList` handles fetching and caching the bingo drop item IDs from a remote Google Sheet CSV (`config.bingoListUrl()`), polling every 15 minutes on RuneLite's shared `ScheduledExecutorService`. Stores results in a `volatile Set<Integer>` so the client thread can safely read it from `onLootReceived`. HTTP fetches are async via OkHttp. `start()` schedules the refresh; `stop()` cancels the task (without shutting down the shared pool) and clears the set. Empty/malformed URLs result in an empty match set.

**`WebhookBody` dual purpose** — serialized once into the `payload_json` part. It carries both Discord's rendered `embeds` *and* a custom `bingo` (`Payload`) object with structured drop data. Discord ignores unknown top-level fields, so the same POST renders a rich embed for humans **and** feeds a downstream bot the machine-readable payload. Keep both in sync when changing the schema. JSON is produced with `RuneLiteAPI.GSON` (do not build a Gson).

**Screenshots** — when `sendScreenshot` is on, `DrawManager.requestNextFrameListener` captures the next frame; the PNG is added to the multipart body as the `file` part named `image.png`, and the embed references it via `attachment://image.png`.

**Bingo item set** (`BingoItemList`) — the matched item IDs come from a published Google Sheet (CSV) at `config.bingoListUrl()`. `BingoItemList` polls it every 15 minutes via the shared `ScheduledExecutorService` (an async `OkHttpClient` fetch; `parseCsv` takes each row's first column as an item ID). The result is held in a `volatile Set<Integer>` — `volatile` because it is written on the okhttp/executor threads and read on the client thread in `onLootReceived`. `onConfigChanged` reschedules on the `bingoListUrl` key. The scheduled task is cancelled in `shutDown()`; the shared executor is never shut down. Config group is `"bingodiscordlogger"` (`BingoDiscordLoggerConfig.GROUP`).

**Multiple webhooks** — `config.webhook()` may contain several URLs separated by newlines; each is parsed/validated with `HttpUrl.parse` and sent independently.

