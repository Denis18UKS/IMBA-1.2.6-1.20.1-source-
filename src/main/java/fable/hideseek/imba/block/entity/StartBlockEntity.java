package fable.hideseek.imba.block.entity;

import fable.hideseek.imba.ImbaMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class StartBlockEntity extends BlockEntity {
    private String title = "Начать";

    public StartBlockEntity(BlockPos pos, BlockState state) {
        super(ImbaMod.START_BLOCK_ENTITY, pos, state);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        String cleaned = value == null ? "" : value.strip();
        title = cleaned.isEmpty() ? "Начать" : cleaned.substring(0, Math.min(32, cleaned.length()));
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Title", title);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("Title")) title = nbt.getString("Title");
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
