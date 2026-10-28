package com.permadeath.net;

import com.permadeath.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le dice a cada cliente que muestre la animacion de cambio (type 0) o de muerte (type 1). */
public class AnimationPacket {
    public static final byte CHANGE = 0;
    public static final byte DEATH = 1;

    private final byte type;
    private final String text;

    public AnimationPacket(byte type, String text) {
        this.type = type;
        this.text = text;
    }

    public static void encode(AnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.type);
        buf.writeUtf(msg.text, 256);
    }

    public static AnimationPacket decode(FriendlyByteBuf buf) {
        byte type = buf.readByte();
        String text = buf.readUtf(256);
        return new AnimationPacket(type, text);
    }

    public static void handle(AnimationPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHooks.animation(msg.type, msg.text)));
        ctx.setPacketHandled(true);
    }
}
