package bingodiscordlogger;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
class WebhookBody
{
    private String content;

    private List<Embed> embeds = new ArrayList<>();

    // Our structured bot payload. Discord ignores unknown top-level fields,
    // so this rides along untouched in payload_json.
    private Payload bingo;

    @Data
    static class Embed
    {
        private int color;
        private String title;
        private String description;
        private Author author;
        private List<Field> fields = new ArrayList<>();
        private Image image;
        private String timestamp;
    }

    @Data
    static class Author
    {
        private String name;
        // Optional - small avatar to the left of the name.
        @SerializedName("icon_url")
        private String iconUrl;
    }

    @Data
    static class Field
    {
        private String name;
        private String value;
        // Inline fields render side-by-side in columns rather than stacked.
        private boolean inline;
    }

    @Data
    static class Image
    {
        // Either a real URL or "attachment://filename.png" to reference an
        // uploaded file from the same multipart POST.
        private String url;
    }

    @Data
    static class Payload
    {
        private String player;
        private String source;
        private String sourceType;
        private int combatLevel;
        private long timestamp;
        private List<Item> items = new ArrayList<>();

        @Data
        static class Item
        {
            private int id;
            private String name;
            private int quantity;
        }
    }
}