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
import okhttp3.*;

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
                log.debug("Body of WebhookBody is \n{}", body);
            });
        }
        else
        {
            send(body, null);
        }
    }

    private void send(WebhookBody body, byte[] screenshot)
    {
        String configUrl = config.webhook();
        HttpUrl url = HttpUrl.parse(configUrl);
        if (url == null)
        {
            log.warn("Malformed webhook URL: {}", configUrl);
            return;
        }
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload_json", GSON.toJson(body));
        if (screenshot != null)
        {
            requestBodyBuilder.addFormDataPart("file", "image.png",
                RequestBody.create(MediaType.parse("image/png"), screenshot));
        }


        Request request = new Request.Builder()
            .url(url)
            .post(requestBodyBuilder.build())
            .build();

        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.warn("Error submitting webhook: ", e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (ResponseBody responseBody = response.body())
                {
                    if (!response.isSuccessful())
                    {
                        String bodyText = responseBody != null ? responseBody.string() : "<no body>";
                        log.warn("Webhook returned {}: {}", response.code(), bodyText);
                    }
                }
                catch (IOException e)
                {
                    log.warn("Failed to read webhook response body", e);
                }
                response.close();
            }
        });
    }

    private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", out);
        return out.toByteArray();
    }
}
