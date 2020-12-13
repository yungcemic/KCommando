package me.koply.kcommando;

public class CProcessParameters {

    private final Author author;
    private final boolean webhookMessage;
    private final String rawCommand;
    private final String guildName;
    private final boolean fromGuild;
    private final Object event;

    public CProcessParameters(Author author, boolean webhookMessage, String rawCommand, String guildName, boolean fromGuild, final Object event) {
        this.author = author;
        this.webhookMessage = webhookMessage;
        this.rawCommand = rawCommand;
        this.guildName = guildName;
        this.fromGuild = fromGuild;
        this.event = event;
    }

    public Author getAuthor() {
        return author;
    }

    public boolean isWebhookMessage() {
        return webhookMessage;
    }

    public String getRawCommand() {
        return rawCommand;
    }

    public String getGuildName() {
        return guildName;
    }

    public boolean isFromGuild() {
        return fromGuild;
    }

    public Object getEvent() {
        return event;
    }

    public static final class Author {
        private final String name;
        private final long id;
        private final boolean bot;

        public Author(String name, long id, boolean bot) {
            this.name = name;
            this.id = id;
            this.bot = bot;
        }

        public String getName() {
            return name;
        }

        public long getId() {
            return id;
        }

        public boolean isBot() {
            return bot;
        }
    }
}