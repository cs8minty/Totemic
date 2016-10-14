package totemic_commons.pokefenn.tileentity.totem;

import static totemic_commons.pokefenn.Totemic.logger;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import totemic_commons.pokefenn.Totemic;
import totemic_commons.pokefenn.api.music.MusicAcceptor;
import totemic_commons.pokefenn.api.music.MusicInstrument;
import totemic_commons.pokefenn.api.totem.TotemBase;
import totemic_commons.pokefenn.api.totem.TotemEffect;
import totemic_commons.pokefenn.block.totem.BlockTotemBase;
import totemic_commons.pokefenn.lib.WoodVariant;
import totemic_commons.pokefenn.tileentity.TileTotemic;

public class TileTotemBase extends TileTotemic implements MusicAcceptor, TotemBase, ITickable
{
    public static final int MAX_HEIGHT = 5;
    public static final int MAX_EFFECT_MUSIC = 128;

    private boolean firstTick = true;

    private TotemState state = new StateTotemEffect(this);

    private final List<TotemEffect> totemEffectList = new ArrayList<>(MAX_HEIGHT);
    private final Multiset<TotemEffect> totemEffects = HashMultiset.create(Totemic.api.registry().getTotems().size());

    @Override
    public void update()
    {
        if(firstTick)
        {
            calculateTotemEffects();
            firstTick = false;
        }

        state.update();
    }

    private void calculateTotemEffects()
    {
        totemEffectList.clear();
        totemEffects.clear();

        for(int i = 0; i < MAX_HEIGHT; i++)
        {
            TileEntity tile = worldObj.getTileEntity(pos.up(i + 1));
            if(tile instanceof TileTotemPole)
            {
                TotemEffect effect = ((TileTotemPole) tile).getEffect();
                totemEffectList.add(effect);
                if(effect != null)
                    totemEffects.add(effect);
            }
            else
                break;
        }
    }

    public void onPoleChange()
    {
        calculateTotemEffects();
        logger.trace("Totem pole changed at {}", pos);
    }

    public TotemState getState()
    {
        return state;
    }

    public void setState(TotemState state)
    {
        if(state != this.state)
        {
            this.state = state;
            markForUpdate();
            markDirty();
        }
    }

    public WoodVariant getWoodType()
    {
        return worldObj.getBlockState(pos).getValue(BlockTotemBase.WOOD);
    }

    public Multiset<TotemEffect> getTotemEffectSet()
    {
        return totemEffects;
    }

    @Override
    public int getTotemEffectMusic()
    {
        if(state instanceof StateTotemEffect)
            return ((StateTotemEffect) state).getMusicAmount();
        else
            return 0;
    }

    @Override
    public int getPoleSize()
    {
        return totemEffectList.size();
    }

    @Override
    public int getRepetition(TotemEffect effect)
    {
        return totemEffects.count(effect);
    }

    @Override
    public TotemEffect[] getEffects()
    {
        return totemEffectList.toArray(new TotemEffect[0]);
    }

    @Override
    public int getWoodBonus()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean addMusic(MusicInstrument instr, int amount)
    {
        boolean added = state.addMusic(instr, amount);
        if(added)
            markDirty();
        return added;
    }

    public boolean canSelect()
    {
        return state.canSelect();
    }

    public void addSelector(MusicInstrument instr)
    {
        state.addSelector(instr);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);

        tag.setByte("state", (byte) state.getID());
        state.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        if(tag.hasKey("state", 99))
            state = TotemState.fromID(tag.getByte("state"), this);
        else
            state = new StateTotemEffect(this);

        state.readFromNBT(tag);
    }
}
