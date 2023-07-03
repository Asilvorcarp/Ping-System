package com.asilvorcarp;

import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.util.math.Vec3d;

public class PingPoint {
    public Vec3d pos;
    public String owner;
    public Color4f color;

    public PingPoint(Vec3d pos, String owner) {
        this.pos = pos;
        this.owner = owner;
        // default color
        this.color = new Color4f(247 / 256f, 175 / 256f, 53 / 256f);
    }

    public PingPoint(Vec3d pos, String owner, Color4f color) {
        this.pos = pos;
        this.owner = owner;
        this.color = color;
    }
}
