package minefantasy.mfr.util;

import com.sun.istack.internal.Nullable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

public class Utils {

    public static <T> T nullValue() {
        return null;
    }

    public static boolean doesMatch(ItemStack item1, ItemStack item2) {
        return item2.getItem() == item1.getItem() && (item2.getItemDamage() == OreDictionary.WILDCARD_VALUE
                || item2.getItemDamage() == item1.getItemDamage());
    }

    public static TileEntityChest getOtherDoubleChest(TileEntity inv) {
        if (inv instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) inv;

            TileEntityChest adjacent = null;

            if (chest.adjacentChestXNeg != null) {
                adjacent = chest.adjacentChestXNeg;
            }

            if (chest.adjacentChestXPos != null) {
                adjacent = chest.adjacentChestXPos;
            }

            if (chest.adjacentChestZNeg != null) {
                adjacent = chest.adjacentChestZNeg;
            }

            if (chest.adjacentChestZPos != null) {
                adjacent = chest.adjacentChestZPos;
            }

            return adjacent;
        }
        return null;
    }

    public interface IItemPropertyGetterFix extends IItemPropertyGetter {
        float applyPropertyGetter(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn);

        static IItemPropertyGetterFix create(final IItemPropertyGetterFix lambda) {
            return lambda;
        }

        @Override
        @SideOnly(Side.CLIENT)
        default float apply(final ItemStack stack, @Nullable final World worldIn, @Nullable final EntityLivingBase entityIn) {
            return applyPropertyGetter(stack, worldIn, entityIn);
        }
    }

}
