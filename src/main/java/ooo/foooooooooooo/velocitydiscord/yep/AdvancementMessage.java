package ooo.foooooooooooo.velocitydiscord.yep;

public class AdvancementMessage implements IYepMessage {
    public final String title;
    public final String description;

    public AdvancementMessage(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public static AdvancementMessage fromString(String str) {
        var parts = str.split("\\|");

        return new AdvancementMessage(parts[0], parts[1]);
    }

    @Override
    public MessageType getType() {
        return MessageType.ADVANCEMENT;
    }

    @Override
    public String toString() {
        return title + "|" + description;
    }
}
