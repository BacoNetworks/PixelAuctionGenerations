package baconetworks.pixelauctiongenerations.utils;

import java.util.UUID;

public class CommandCooldown {
    private UUID uuid;
    private long unixTime;

    public CommandCooldown(UUID uuid, long unixTime) {
        this.uuid = uuid;
        this.unixTime = unixTime;
    }


    public UUID getUuid() {
        return this.uuid;
    }


    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }


    public long getUnixTime() {
        return this.unixTime;
    }


    public void setUnixTime(long unixTime) {
        this.unixTime = unixTime;
    }
}
