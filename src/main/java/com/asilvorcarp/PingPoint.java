package com.asilvorcarp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;

import static com.asilvorcarp.ApexMC.LOGGER;

public class PingPoint implements Serializable {
    // TODO config for this
    public static long SecondsToVanish = 10;
    public Vec3d pos;
    public String owner;
    public Color color;
    public LocalDateTime createTime;

    public PingPoint(Vec3d pos, String owner) {
        this.pos = pos;
        this.owner = owner;
        // default color
        this.color = new Color(247 / 256f, 175 / 256f, 53 / 256f);
        this.createTime = LocalDateTime.now();
    }

    public PingPoint(Vec3d pos, String owner, Color color) {
        this.pos = pos;
        this.owner = owner;
        this.color = color;
        this.createTime = LocalDateTime.now();
    }

    public boolean shouldVanish() {
        return LocalDateTime.now().minusSeconds(SecondsToVanish).isAfter(createTime);
    }

    // Vec3d is not serializable
    @Serial
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeDouble(pos.x);
        stream.writeDouble(pos.y);
        stream.writeDouble(pos.z);
        stream.writeObject(owner);
        stream.writeObject(color);
        stream.writeObject(createTime);
    }

    // Vec3d is not serializable
    @Serial
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        double x = stream.readDouble();
        double y = stream.readDouble();
        double z = stream.readDouble();
        pos = new Vec3d(x, y, z);
        owner = (String) stream.readObject();
        color = (Color) stream.readObject();
        createTime = (LocalDateTime) stream.readObject();
    }

    public ByteBuf toByteBuf() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(this);
        oos.flush();

        // get byte array
        byte[] serializedData = bos.toByteArray();

        ByteBuf buffer = Unpooled.buffer(serializedData.length);
        buffer.writeBytes(serializedData);

        oos.close();
        bos.close();

        return buffer;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(this);
        oos.flush();

        // get byte array
        byte[] serializedData = bos.toByteArray();

        oos.close();
        bos.close();

        return serializedData;
    }

    public PacketByteBuf toPacketByteBuf() throws IOException {
        var buf = PacketByteBufs.create();
        buf.writeBytes(this.toByteArray());
//        buf.writeVector3f(Vec3dToV3f(pos));
        return buf;
    }

    public static PingPoint fromPacketByteBuf(PacketByteBuf buf) throws IOException, ClassNotFoundException {
        var serializedData = buf.getWrittenBytes();

        ByteArrayInputStream bis = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois = new ObjectInputStream(bis);

        // deserialize
        PingPoint p = (PingPoint) ois.readObject();

        ois.close();
        bis.close();

        return p;
    }
}
