package bingodiscordlogger;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.DrawManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static net.runelite.http.api.RuneLiteAPI.GSON;

/**
 * Sends a {@link WebhookBody} to one or more Discord webhooks as multipart/form-data.
 *
 * <p>When screenshots are enabled the next rendered frame is grabbed via
 * {@link DrawManager} and attached as the {@code file} part named {@code image.png},
 * which the embed references via {@code attachment://image.png}. The configured
 * webhook string may contain several newline-separated URLs; each is validated and
 * sent independently.
 */
@Slf4j
@Singleton
public class DiscordWebhookClient
{
    private final BingoDiscordLoggerConfig config;
    private final OkHttpClient okHttpClient;
    private final DrawManager drawManager;

    @Inject
    DiscordWebhookClient(BingoDiscordLoggerConfig config, OkHttpClient okHttpClient, DrawManager drawManager)
    {
        this.config = config;
        this.okHttpClient = okHttpClient;
        this.drawManager = drawManager;
    }

    void send(WebhookBody body)
    {
        String configUrls = config.webhook();
        if (Strings.isNullOrEmpty(configUrls))
        {
            log.debug("Webhook URL not configured, skipping send");
            return;
        }

        if (config.sendScreenshot())
        {
            drawManager.requestNextFrameListener(image ->
            {
                BufferedImage bufferedImage = (BufferedImage) image;
                byte[] imageBytes = null;
                try
                {
                    imageBytes = convertImageToByteArray(bufferedImage);
                }
                catch (IOException e)
                {
                    log.error("Error converting image to byte array", e);
                }
                send(body, imageBytes);
            });
        }
        else
        {
            send(body, null);
        }
    }

    private void send(WebhookBody body, byte[] screenshot)
    {
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload_json", GSON.toJson(body));

        if (screenshot != null)
        {
            requestBodyBuilder.addFormDataPart("file", "image.png",
                    RequestBody.create(MediaType.parse("image/png"), screenshot));
        }

        MultipartBody requestBody = requestBodyBuilder.build();

        for (String url : Splitter.on('\n').omitEmptyStrings().trimResults().split(config.webhook()))
        {
            HttpUrl u = HttpUrl.parse(url);
            if (u == null)
            {
                log.warn("Malformed webhook URL: {}", url);
                continue;
            }

            Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

            okHttpClient.newCall(request).enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.warn("Error submitting webhook", e);
                }

                @Override
                public void onResponse(Call call, Response response)
                {
                    response.close();
                }
            });
        }
    }

    private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", out);
        return out.toByteArray();
    }
}
