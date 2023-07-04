package com.asilvorcarp;

import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.util.math.Vec3d;

import java.time.LocalDateTime;

public class PingPoint {
    // TODO config for this
    public static long SecondsToDisappear = 10;
    public Vec3d pos;
    public String owner;
    public Color4f color;
    public LocalDateTime createTime;

    public PingPoint(Vec3d pos, String owner) {
        this.pos = pos;
        this.owner = owner;
        // default color
        this.color = new Color4f(247 / 256f, 175 / 256f, 53 / 256f);
        this.createTime = LocalDateTime.now();
    }

    public PingPoint(Vec3d pos, String owner, Color4f color) {
        this.pos = pos;
        this.owner = owner;
        this.color = color;
        this.createTime = LocalDateTime.now();
    }

    public boolean isDead(){
        return LocalDateTime.now().minusSeconds(SecondsToDisappear).compareTo(createTime)>0;
    }
}
