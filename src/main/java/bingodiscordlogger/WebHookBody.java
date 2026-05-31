package bingodiscordlogger;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
class WebhookBody
{
    // What Discord renders in the channel for human eyeballing.
    private String content;
    // Structured data the bot will parse out of payload_json. Discord ignores
    // unknown top-level fields, so this rides along untouched.
    private Payload bingo;

    @Data
    static class Payload
    {
        private String eventId;
        private String player;
        private String source;       // NPC name, "Chambers of Xeric", "Clue scroll (medium)", etc.
        private String sourceType;   // NPC / EVENT / PICKPOCKET / UNKNOWN
        private int combatLevel;     // -1 for non-NPC sources
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