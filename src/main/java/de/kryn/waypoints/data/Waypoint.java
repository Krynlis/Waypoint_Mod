package de.kryn.waypoints.data;

public class Waypoint {
    public String name = "";
    public int x;
    public int y;
    public int z;
    /** Dimension-ID, z.B. "minecraft:overworld". */
    public String dimension = "minecraft:overworld";
    public String icon = WaypointIcon.HOUSE.name();
    /** RGB ohne Alpha. */
    public int color = 0xFFFFFF;
    public boolean visible = true;

    public WaypointIcon getIcon() {
        return WaypointIcon.byName(icon);
    }

    public int argb() {
        return 0xFF000000 | color;
    }
}
