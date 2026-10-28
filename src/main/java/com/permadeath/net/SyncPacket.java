package com.permadeath.net;

import com.permadeath.Change;
import com.permadeath.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Lista de cambios activos para que el libro del cliente los muestre. */
public class SyncPacket {
    private final List<Integer> ids;

    public SyncPacket(List<Integer> ids) {
        this.ids = ids;
    }

    public static SyncPacket of(List<Change> changes) {
        List<Integer> ids = new ArrayList<>();
        for (Change c : changes) ids.add(c.ordinal());
        return new SyncPacket(ids);
    }

    public static void encode(SyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ids.size());
        for (int id : msg.ids) buf.writeVarInt(id);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) ids.add(buf.readVarInt());
        return new SyncPacket(ids);
    }

    public static void handle(SyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHooks.sync(msg.ids)));
        ctx.setPacketHandled(true);
    }
}
