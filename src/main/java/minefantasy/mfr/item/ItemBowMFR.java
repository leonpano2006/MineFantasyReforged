package minefantasy.mfr.item;

import minefantasy.mfr.MineFantasyReforged;
import minefantasy.mfr.api.archery.IAmmo;
import minefantasy.mfr.api.archery.IDisplayMFRAmmo;
import minefantasy.mfr.api.archery.IFirearm;
import minefantasy.mfr.api.archery.ISpecialBow;
import minefantasy.mfr.api.weapon.IRackItem;
import minefantasy.mfr.client.render.item.RenderBow;
import minefantasy.mfr.init.MineFantasySounds;
import minefantasy.mfr.init.MineFantasyTabs;
import minefantasy.mfr.material.CustomMaterial;
import minefantasy.mfr.mechanics.AmmoMechanics;
import minefantasy.mfr.network.NetworkHandler;
import minefantasy.mfr.proxy.IClientRegister;
import minefantasy.mfr.tile.TileEntityRack;
import minefantasy.mfr.util.CustomToolHelper;
import minefantasy.mfr.util.ModelLoaderHelper;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ItemBowMFR extends ItemBow implements ISpecialBow, IDisplayMFRAmmo, IFirearm, IRackItem, IClientRegister {
	public static final DecimalFormat decimal_format = new DecimalFormat("#.##");
	private final EnumBowType model;
	private final int itemRarity;
	private float baseDamage = 1.0F;
	/**
	 * Return the enchantability factor of the item, most of the time is based on
	 * material.
	 */
	private int enchantmentLvl = 1;
	// ===================================================== CUSTOM START
	// =============================================================\\
	private boolean isCustom = false;
	private String designType = "standard";

	public ItemBowMFR(String name, EnumBowType type) {
		this(name, ToolMaterial.WOOD, type, 0);
	}

	public ItemBowMFR(String name, ToolMaterial mat, EnumBowType type, int rarity) {
		this(name, (int) (mat.getMaxUses() * type.durabilityModifier), type, mat.getAttackDamage(), rarity);
		this.enchantmentLvl = mat.getEnchantability();
	}

	private ItemBowMFR(String name, int dura, EnumBowType type, float damage, int rarity) {
		this.baseDamage = damage;
		model = type;
		this.maxStackSize = 1;
		this.setMaxDamage(dura);
		itemRarity = rarity;
		setRegistryName(name);
		setTranslationKey(name);
		setCreativeTab(MineFantasyTabs.tabOldTools);

		MineFantasyReforged.PROXY.addClientRegister(this);
		this.addPropertyOverride(new ResourceLocation("pull"), new IItemPropertyGetter() {
			@SideOnly(Side.CLIENT)
			public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
				if (entityIn == null) {
					return 0.0F;
				} else {
					return !(entityIn.getActiveItemStack().getItem() instanceof ItemBow) ? 0.0F : (float) ((stack.getMaxItemUseDuration()) - entityIn.getItemInUseCount()) / getDrawbackTime(stack);

				}
			}
		});
		this.addPropertyOverride(new ResourceLocation("pulling"), new IItemPropertyGetter() {
			@SideOnly(Side.CLIENT)
			public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
				return entityIn != null && entityIn.isHandActive() && entityIn.getActiveItemStack() == stack ? 1.0F : 0.0F;
			}
		});
	}

	@Override
	@SideOnly(Side.CLIENT)
	/**
	 * Returns True is the item is renderer in full 3D when hold.
	 */
	public boolean isFull3D() {
		return true;
	}

	/**
	 * called when the player releases the use item button. Args: itemstack, world,
	 * entityplayer, itemInUseCount
	 */
	@Override
	public void onPlayerStoppedUsing(final ItemStack bow, final World world, final EntityLivingBase shooter, final int timeLeft) {
		int charge = this.getMaxItemUseDuration(bow) - timeLeft;
		if (!(shooter instanceof EntityPlayer))
			return;

		final EntityPlayer player = (EntityPlayer) shooter;
		final boolean ammoRequired = isAmmoRequired(bow, player);
		ItemStack ammoStack = AmmoMechanics.getAmmo(bow);

		charge = ForgeEventFactory.onArrowLoose(bow, world, player, charge, !ammoStack.isEmpty() || !ammoRequired);
		if (charge < 0)
			return;

		if (!ammoStack.isEmpty() || !ammoRequired) {
			if (ammoStack.isEmpty()) {
				ammoStack = new ItemStack(Items.ARROW);
			}

			final float arrowVelocity = getArrowVelocity(charge);

			if (arrowVelocity >= 0.1) {
				final boolean isInfinite = player.capabilities.isCreativeMode || ammoStack.getItem() instanceof ItemArrow && ((ItemArrow) ammoStack.getItem()).isInfinite(ammoStack, bow, player);

				if (!world.isRemote) {
					final ItemArrow itemArrow = (ItemArrow) (ammoStack.getItem() instanceof ItemArrow ? ammoStack.getItem() : Items.ARROW);
					EntityArrow entityArrow = itemArrow.createArrow(world, ammoStack, player);
					entityArrow.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, arrowVelocity * 3.0F, 1.0F);

					float firepower = arrowVelocity / model.chargeTime;

					if (firepower < 0.1D) {
						return;
					}
					if (firepower > 1.0F) {
						firepower = 1.0F;
					}

					if (firepower == 1.0f) {
						entityArrow.setIsCritical(true);
					}

					final int powerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, bow);
					if (powerLevel > 0) {
						entityArrow.setDamage(entityArrow.getDamage() + (double) powerLevel * 0.5D + 0.5D);
					}

					final int punchLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, bow);
					if (punchLevel > 0) {
						entityArrow.setKnockbackStrength(punchLevel);
					}

					if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, bow) > 0) {
						entityArrow.setFire(100);
					}

					AmmoMechanics.damageContainer(bow, player, 1);
					world.playSound(player.posX, player.posY, player.posZ, MineFantasySounds.BOW_FIRE, SoundCategory.NEUTRAL, 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + firepower * 0.5F, true);

					if (isInfinite) {
						entityArrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
					}

					entityArrow = (EntityArrow) modifyArrow(bow, entityArrow);

					world.spawnEntity(entityArrow);
				}

				if (!isInfinite && ammoStack.getCount() > 0) {
					ammoStack.shrink(1);
				}

				player.addStat(StatList.getObjectUseStats(this));
			}
		}
	}

	public static int getDrawbackTime(ItemStack stack) {
		return (int) (((ItemBowMFR)stack.getItem()).model.chargeTime + (5 * (CustomToolHelper.getCustomPrimaryMaterial(stack).resistance / 25)));
	}

	/**
	 * How long it takes to use or consume an item
	 */
	@Override
	public int getMaxItemUseDuration(ItemStack item) {
		return getDrawbackTime(item) * 20;
	}

	/**
	 * returns the action that specifies what animation to play when the items is
	 * being used
	 */
	@Override
	public EnumAction getItemUseAction(ItemStack item) {
		return EnumAction.BOW;
	}

	@Override
	public void addInformation(ItemStack item, World world, List<String> list, ITooltipFlag flag) {
		super.addInformation(item, world, list, flag);

		CustomToolHelper.addBowInformation(item, list);
		ItemStack ammo = AmmoMechanics.getAmmo(item);
		if (!ammo.isEmpty()) {
			list.add(TextFormatting.DARK_GRAY + ammo.getDisplayName() + " x" + ammo.getCount());
		}

		list.add(TextFormatting.BLUE + I18n.format("attribute.bowPower.name", decimal_format.format(getBowDamage(item))));
		list.add(TextFormatting.GREEN + I18n.format("attribute.bowDrawbackSpeed.name", decimal_format.format(getDrawbackTime(item) / 20F)));
	}

	/**
	 * Called whenever this item is equipped and the right mouse button is pressed.
	 * Args: itemStack, world, entityPlayer
	 */
	@Override
	public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
		ItemStack bow = player.getHeldItem(hand);
		final boolean hasAmmo = !AmmoMechanics.isDepleted(bow);

		if (AmmoMechanics.isDepleted(bow)) {
			reloadFirearm(player);
			return ActionResult.newResult(EnumActionResult.FAIL, bow);
		}

		final ActionResult<ItemStack> ret = ForgeEventFactory.onArrowNock(bow, world, player, hand, hasAmmo);
		if (ret != null){
			return ret;
		}

		if (isAmmoRequired(bow, player) && !hasAmmo) {
			return new ActionResult<>(EnumActionResult.FAIL, bow);
		} else {
			player.setActiveHand(hand);
			return new ActionResult<>(EnumActionResult.SUCCESS, bow);
		}
	}

	public boolean canAccept(ItemStack ammo) {
		String ammoType = "null";
		ItemStack weapon = new ItemStack(this);
		if (!ammo.isEmpty() && ammo.getItem() instanceof IAmmo) {
			ammoType = ((IAmmo) ammo.getItem()).getAmmoType(ammo);
		}

		if (!weapon.isEmpty() && weapon.getItem() instanceof IFirearm) {
			return ((IFirearm) weapon.getItem()).canAcceptAmmo(weapon, ammoType);
		}

		return ammoType.equalsIgnoreCase("arrow");
	}

	@Override
	public void reloadFirearm(EntityPlayer player) {
		player.openGui(MineFantasyReforged.MOD_ID, NetworkHandler.GUI_RELOAD, player.world, 1, 0, 0);
	}

	@Override
	public int getItemEnchantability() {
		return enchantmentLvl;
	}

	@Override
	public void onUpdate(ItemStack item, World world, Entity entity, int i, boolean b) {
		super.onUpdate(item, world, entity, i, b);
		if (!item.hasTagCompound()) {
			item.setTagCompound(new NBTTagCompound());
			item.getTagCompound().setInteger("Use", i);
		}
	}

	@Override
	public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
		// Ignore durability changes
		if (ItemStack.areItemsEqualIgnoreDurability(oldStack, newStack))
			return true;
		return super.canContinueUsing(oldStack, newStack);
	}

	/**
	 * Is ammunition required to fire this bow?
	 *
	 * @param bow     The bow
	 * @param shooter The shooter
	 * @return Is ammunition required?
	 */
	protected boolean isAmmoRequired(final ItemStack bow, final EntityPlayer shooter) {
		return !shooter.capabilities.isCreativeMode && EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, bow) == 0;
	}

	@Override
	public EnumRarity getRarity(ItemStack item) {
		int lvl = itemRarity;

		EnumRarity[] rarity = new EnumRarity[] {EnumRarity.COMMON, EnumRarity.UNCOMMON, EnumRarity.RARE, EnumRarity.EPIC};
		if (item.isItemEnchanted()) {
			if (lvl == 0) {
				lvl++;
			}
			lvl++;
		}
		if (lvl >= rarity.length) {
			lvl = rarity.length - 1;
		}
		return rarity[lvl];
	}

	@Override
	public Entity modifyArrow(ItemStack bow, Entity arrow) {
		if (this.isCustom) {
			CustomMaterial custom = CustomToolHelper.getCustomPrimaryMaterial(bow);
			if (custom != CustomMaterial.NONE) {
				if (custom.name.equalsIgnoreCase("silver")) {
					arrow.getEntityData().setBoolean("MF_Silverbow", true);
				}
			}
		}

		float dam = getBowDamage(bow);

		arrow.getEntityData().setFloat("MF_Bow_Damage", dam);
		arrow.getEntityData().setString("Design", designType);

		return arrow;
	}

	@Override
	public boolean canAcceptAmmo(ItemStack weapon, String ammo) {
		return ammo.equalsIgnoreCase("arrow");
	}

	@Override
	public int getAmmoCapacity(ItemStack item) {
		return 1;
	}

	public ItemBowMFR setCustom(String designType) {
		canRepair = false;
		isCustom = true;
		this.designType = designType;
		return this;
	}

	public ItemStack construct(String main, String haft) {
		return CustomToolHelper.construct(this, main, haft);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getItemStackDisplayName(ItemStack item) {
		String unlocalName = this.getUnlocalizedNameInefficiently(item) + ".name";
		return CustomToolHelper.getSecondaryLocalisedName(item, unlocalName);
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		return CustomToolHelper.getMaxDamage(stack, super.getMaxDamage(stack));
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (!isInCreativeTab(tab)) {
			return;
		}
		if (isCustom) {
			ArrayList<CustomMaterial> wood = CustomMaterial.getList("wood");
			for (CustomMaterial customMat : wood) {
				if (MineFantasyReforged.isDebug() || !customMat.getItemStack().isEmpty()) {
					items.add(this.construct("iron", customMat.name));
				}
			}
		}
	}

	public float getBowDamage(ItemStack item) {
		return CustomToolHelper.getBowDamage(item, baseDamage) * model.damageModifier;
	}

	@Override
	public float getVelocity(ItemStack item) {
		return model.velocity;
	}

	@Override
	public float getSpread(ItemStack item) {
		return model.spread;
	}

	// ====================================================== CUSTOM END
	// ==============================================================\\
	@Override
	public float getMaxCharge() {
		return model.chargeTime;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerClient() {
		ModelResourceLocation modelLocation = new ModelResourceLocation(getRegistryName(), "normal");
		ModelLoaderHelper.registerWrappedItemModel(this, new RenderBow(() -> modelLocation), modelLocation);
	}

	@Override
	public float getScale(ItemStack itemstack) {
		return 1F;
	}

	@Override
	public float getOffsetX(ItemStack itemstack) {
		return 1.2F;
	}

	@Override
	public float getOffsetY(ItemStack itemstack) {
		return 1F;
	}

	@Override
	public float getOffsetZ(ItemStack itemstack) {
		return 0.4F;
	}

	@Override
	public float getRotationOffset(ItemStack itemstack) {
		return 90;
	}

	@Override
	public boolean canHang(TileEntityRack rack, ItemStack item, int slot) {
		return true;
	}

	@Override
	public boolean flip(ItemStack itemStack) {
		return true;
	}
}
