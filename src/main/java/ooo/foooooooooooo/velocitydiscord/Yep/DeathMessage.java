package ooo.foooooooooooo.velocitydiscord.Yep;

public class DeathMessage implements IYepMessage {
    public final String message;

    public DeathMessage(String message) {
        this.message = message;
    }

    public static DeathMessage fromString(String str) {
        return new DeathMessage(str);
    }

    @Override
    public MessageType getType() {
        return MessageType.DEATH;
    }

    @Override
    public String toString() {
        return message;
    }
}
