package thut.core.common.world.mobs.data;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import thut.api.world.mobs.data.Data;
import thut.api.world.mobs.data.DataSync;
import thut.core.common.ThutCore;
import thut.core.common.network.Packet;

public class PacketDataSync extends Packet
{
    public static void sync(Entity tracked, DataSync data, int entity_id, boolean all)
    {
        final List<Data<?>> list = all ? data.getAll() : data.getDirty();
        // Nothing to sync.
        if (list == null || tracked == null) return;
        final PacketDataSync packet = new PacketDataSync();
        packet.data = list;
        packet.id = entity_id;
        ThutCore.packets.sendToTracking(packet, tracked);
        if (tracked instanceof ServerPlayerEntity) ThutCore.packets.sendTo(packet, (ServerPlayerEntity) tracked);
    }

    public static void sync(ServerPlayerEntity syncTo, DataSync data, int entity_id, boolean all)
    {
        final List<Data<?>> list = all ? data.getAll() : data.getDirty();
        // Nothing to sync.
        if (list == null) return;
        final PacketDataSync packet = new PacketDataSync();
        packet.data = list;
        packet.id = entity_id;
        ThutCore.packets.sendTo(packet, syncTo);
    }

    public int id;

    public List<Data<?>> data = Lists.newArrayList();

    public PacketDataSync()
    {
        super(null);
    }

    public PacketDataSync(PacketBuffer buf)
    {
        super(buf);
        this.id = buf.readInt();
        final byte num = buf.readByte();
        if (num > 0) for (int i = 0; i < num; i++)
        {
            final int uid = buf.readInt();
            try
            {
                final Data<?> val = DataSync_Impl.makeData(uid);
                val.read(buf);
                this.data.add(val);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleClient()
    {
        PlayerEntity player;
        player = ThutCore.proxy.getPlayer();
        final World world = player.getEntityWorld();
        final Entity mob = world.getEntityByID(this.id);
        if (mob == null) return;
        final DataSync sync = SyncHandler.getData(mob);
        if (sync == null) return;
        sync.update(this.data);
        return;
    }

    @Override
    public void write(PacketBuffer buf)
    {
        buf.writeInt(this.id);
        final byte num = (byte) this.data.size();
        buf.writeByte(num);
        for (int i = 0; i < num; i++)
        {
            final Data<?> val = this.data.get(i);
            buf.writeInt(val.getUID());
            val.write(buf);
        }
    }
}
