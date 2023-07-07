package com.asilvorcarp;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class PingPoint implements Serializable {
    public UUID id;
    public Vec3d pos;
    public String owner;
    // the highlight color
    public Color color;
    // the sound index
    public byte sound;
    public LocalDateTime createTime;

    public PingPoint(Vec3d pos, String owner, Color color, byte soundIdx) {
        this.id = UUID.randomUUID();
        this.pos = pos;
        this.owner = owner;
        this.color = color;
        this.sound = soundIdx;
        this.createTime = LocalDateTime.now();
    }

    public boolean shouldVanish(long SecondsToVanish) {
        if(SecondsToVanish == 0){
            return false;
        }
        return LocalDateTime.now().minusSeconds(SecondsToVanish).isAfter(createTime);
    }

    // for Vec3d is not serializable
    @Serial
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(id);
        stream.writeDouble(pos.x);
        stream.writeDouble(pos.y);
        stream.writeDouble(pos.z);
        stream.writeObject(owner);
        stream.writeObject(color);
        stream.writeByte(sound);
        stream.writeObject(createTime);
    }

    // for Vec3d is not serializable
    @Serial
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        id = (UUID) stream.readObject();
        double x = stream.readDouble();
        double y = stream.readDouble();
        double z = stream.readDouble();
        pos = new Vec3d(x, y, z);
        owner = (String) stream.readObject();
        color = (Color) stream.readObject();
        sound = stream.readByte();
        createTime = (LocalDateTime) stream.readObject();
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
