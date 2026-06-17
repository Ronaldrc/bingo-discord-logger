package bingodiscordlogger;

import com.google.common.base.Strings;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Maintains the set of item IDs that count as bingo drops, sourced from a remote
 * published-Google-Sheet CSV ({@code bingoListUrl}) and polled periodically.
 *
 * <p>Threading: {@link #itemIds} is {@code volatile} because it is written from the
 * okhttp / executor threads and read from the client thread (in {@code onLootReceived}
 * via {@link #getItemIds()}). The executor is RuneLite's shared pool, so {@link #stop()}
 * only cancels our task - it never shuts the pool down.
 */
@Slf4j
@Singleton
public class BingoItemList
{
    private static final long REFRESH_MINUTES = 15;

    private final BingoDiscordLoggerConfig config;
    private final OkHttpClient okHttpClient;
    private final ScheduledExecutorService executor;

    // Item IDs matched against loot, fetched from the remote list.
    private volatile Set<Integer> itemIds = Collections.emptySet();

    // Periodic poll of the remote list. Cancelled in stop().
    private ScheduledFuture<?> refreshFuture;

    @Inject
    BingoItemList(BingoDiscordLoggerConfig config, OkHttpClient okHttpClient, ScheduledExecutorService executor)
    {
        this.config = config;
        this.okHttpClient = okHttpClient;
        this.executor = executor;
    }

    void start()
    {
        scheduleRemoteRefresh();
    }

    void stop()
    {
        cancelRefresh();
        itemIds = Collections.emptySet();
    }

    /** The effective match set. Safe to read from the client thread. */
    Set<Integer> getItemIds()
    {
        return itemIds;
    }

    // ---------- Remote bingo list (published Google Sheet CSV) ----------

    void scheduleRemoteRefresh()
    {
        cancelRefresh();
        if (Strings.isNullOrEmpty(config.bingoListUrl()))
        {
            // No URL configured: drop any previously-fetched IDs.
            itemIds = Collections.emptySet();
            return;
        }
        // Fire immediately (initialDelay 0) then poll. Runs on the executor thread;
        // the fetch itself is an async okhttp call, so nothing blocks.
        refreshFuture = executor.scheduleWithFixedDelay(
            this::fetchRemoteList, 0, REFRESH_MINUTES, TimeUnit.MINUTES);
    }

    private void cancelRefresh()
    {
        if (refreshFuture != null)
        {
            refreshFuture.cancel(false);
            refreshFuture = null;
        }
    }

    private void fetchRemoteList()
    {
        String url = config.bingoListUrl();
        if (Strings.isNullOrEmpty(url))
        {
            return;
        }
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null)
        {
            log.debug("Malformed bingo list URL: {}", url);
            return;
        }

        Request request = new Request.Builder().url(parsed).build();
        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to fetch bingo list", e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response r = response)
                {
                    if (!r.isSuccessful() || r.body() == null)
                    {
                        log.debug("Bingo list fetch returned HTTP {}", r.code());
                        return;
                    }
                    Set<Integer> ids = Utils.parseCsv(r.body().string());
                    itemIds = ids.isEmpty()
                            ? Collections.emptySet()
                            : Collections.unmodifiableSet(ids);
                    log.debug("Loaded {} bingo item IDs", itemIds.size());
                }
                catch (IOException e)
                {
                    log.debug("Error reading bingo list response", e);
                }
            }
        });
    }
}
