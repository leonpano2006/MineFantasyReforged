package minefantasy.mfr.recipe;

import minefantasy.mfr.constants.Skill;
import minefantasy.mfr.util.CustomToolHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author AnonymousProductions
 */
public class CraftingManagerAnvil {
	/**
	 * The static instance of this class
	 */
	private static final CraftingManagerAnvil instance = new CraftingManagerAnvil();

	/**
	 * A list of all the recipes added
	 */
	public List recipes = new ArrayList<IRecipe>();
	public HashMap<String, Object> recipeMap = new HashMap<>();

	private CraftingManagerAnvil() {
		Collections.sort(this.recipes, new RecipeSorterAnvil(this));
	}

	/**
	 * Returns the static instance of this class
	 */
	public static CraftingManagerAnvil getInstance() {
		return instance;
	}

	public static IAnvilRecipe getRecipeByName(String name) {
		return (IAnvilRecipe) getInstance().recipeMap.get(name);
	}

	public static IAnvilRecipe[] getRecipeByName(String... name) {
		// using for each loop to display contents of a
		List<IAnvilRecipe> recipes = new ArrayList<>();
		for (String i : name) {
			recipes.add(getRecipeByName(i));
		}
		IAnvilRecipe[] array = new IAnvilRecipe[recipes.size()];
		recipes.toArray(array);
		return array;
	}

	public IAnvilRecipe addRecipe(ItemStack result, Skill skill, String research, boolean hot, String tool, int hammer, int anvil, int time, Object... input) {
		return null;
		//		return addRecipe(result, skill, research, hot, tool, hammer, anvil, time, (byte) 0, input);
	}

	public IAnvilRecipe addToolRecipe(ItemStack result, Skill skill, String research, boolean hot,
			String tool, int hammer, int anvil, int time, Object... input) {
		return null;
		//		return addRecipe(result, skill, research, hot, tool, hammer, anvil, time, (byte) 1, input);
	}

	/**
	 * Adds a recipe. See spreadsheet on first page for details.
	 */
	public IAnvilRecipe addRecipe(String name, ItemStack result, Skill skill, String research, boolean hot, String tool, int hammer, int anvil, int time, byte recipeType, Object... input) {
		String var3 = "";
		int var4 = 0;
		int var5 = 0;
		int var6 = 0;
		int var9;

		if (input[var4] instanceof String[]) {
			String[] var7 = ((String[]) input[var4++]);
			String[] var8 = var7;
			var9 = var7.length;

			for (int var10 = 0; var10 < var9; ++var10) {
				String var11 = var8[var10];
				++var6;
				var5 = var11.length();
				var3 = var3 + var11;
			}
		} else {
			while (input[var4] instanceof String) {
				String var13 = (String) input[var4++];
				++var6;
				var5 = var13.length();
				var3 = var3 + var13;
			}
		}

		HashMap var14;
		for (var14 = new HashMap(); var4 < input.length; var4 += 2) {

			Character var16 = (Character) input[var4];
			ItemStack var17 = null;
			ItemStack[] var18 = null;

			if (input[var4 + 1] instanceof Item) {
				var17 = new ItemStack((Item) input[var4 + 1], 1, 32767);
			} else if (input[var4 + 1] instanceof Block) {
				var17 = new ItemStack((Block) input[var4 + 1], 1, 32767);
			} else if (input[var4 + 1] instanceof ItemStack) {
				var17 = (ItemStack) input[var4 + 1];
			} else if (input[var4 + 1] instanceof ItemStack[]){
				var18 = ((ItemStack[]) input[var4 + 1]);
			}

			if (var17 != null){
				var14.put(var16, var17);
			}
			else {
				var14.put(var16, var18);
			}
		}

		ItemStack[] var15 = new ItemStack[var5 * var6];

		for (var9 = 0; var9 < var5 * var6; ++var9) {
			char var18 = var3.charAt(var9);

			if (var14.containsKey(Character.valueOf(var18))) {
				if (var14.get(Character.valueOf(var18)) instanceof ItemStack){
					var15[var9] = ((ItemStack) var14.get(Character.valueOf(var18))).copy();
				}
				else if (var14.get(Character.valueOf(var18)) instanceof ItemStack[]) {
					var15 = ((ItemStack[]) var14.get(Character.valueOf(var18)));
				}

			} else {
				var15[var9] = null;
			}
		}

		IAnvilRecipe recipe;
		if (recipeType == (byte) 1) {
			recipe = new CustomToolRecipeAnvil(var5, var6, var15, result, tool, time, hammer, anvil, hot, research, skill);
		} else {
			recipe = new ShapedAnvilRecipes(var5, var6, var15, result, tool, time, hammer, anvil, hot, research, skill);
		}
		this.recipes.add(recipe);
		this.recipeMap.put(name, recipe);
		return recipe;
	}

	private boolean canRepair(ItemStack item1, ItemStack item2) {
		if (item1.getItem() == item2.getItem() && item1.getCount() == 1 && item2.getCount() == 1
				&& item1.getItem().isRepairable()) {
			return true;
		}
		if (item1.getItem() == item2.getItem() && item1.getCount() == 1 && item2.getCount() == 1 && item1.isItemDamaged()
				&& CustomToolHelper.areToolsSame(item1, item2)) {
			return true;
		}
		return false;
	}

	public ItemStack findMatchingRecipe(IAnvil anvil, AnvilCraftMatrix matrix, World world) {
		int time;
		int anvilTier;
		boolean hot;
		int hammer;
		int matrixItemStackCount = 0;
		String toolType;
		SoundEvent sound;
		ItemStack stack0 = ItemStack.EMPTY;
		ItemStack stack1 = ItemStack.EMPTY;

		for (int currentSlotIndex = 0; currentSlotIndex < matrix.getSizeInventory(); ++currentSlotIndex) {
			ItemStack matrixSlotStack = matrix.getStackInSlot(currentSlotIndex);

			// Logic to check if the matrix should match an item repair recipe. (The same two items next to each other in the first two slow).
			if (!matrixSlotStack.isEmpty()) {
				if (matrixItemStackCount == 0) {
					stack0 = matrixSlotStack;
				}

				if (matrixItemStackCount == 1) {
					stack1 = matrixSlotStack;
				}

				++matrixItemStackCount;
			}
		}

		// Logic to check if the matrix should match an item repair recipe. (The same two items next to each other in the first two slow).
		if (matrixItemStackCount == 2 && stack0.getItem() == stack1.getItem() && stack0.getCount() == 1 && stack1.getCount() == 1
				&& stack0.getItem().isRepairable()) {

			Item item0 = stack0.getItem();

			int item0RemainingDurability = item0.getMaxDamage() - stack0.getItemDamage();
			int var7 = item0.getMaxDamage() - stack1.getItemDamage();
			int var8 =  (int) (item0RemainingDurability + var7 + item0.getMaxDamage() * 0.1);
			int newItemDamage = Math.max(0, item0.getMaxDamage() - var8);

			return new ItemStack(stack0.getItem(), 1, newItemDamage);
		} else {
			//// Normal, registered recipes.
			Iterator recipeIterator = this.recipes.iterator();
			IAnvilRecipe iAnvilRecipe = null;

			while (recipeIterator.hasNext()) {
				IAnvilRecipe rec = (IAnvilRecipe) recipeIterator.next();

				if (((IRecipe) rec).matches(matrix, world)) {
					iAnvilRecipe = rec;
					break;
				}
			}

			if (iAnvilRecipe != null) {
				time = iAnvilRecipe.getCraftTime();
				hammer = iAnvilRecipe.getRecipeHammer();
				anvilTier = iAnvilRecipe.getAnvilTier();
				hot = iAnvilRecipe.outputHot();
				toolType = iAnvilRecipe.getToolType();

				if (!iAnvilRecipe.useCustomTiers()){
					anvil.setProgressMax(time);
					anvil.setRequiredHammerTier(hammer);
					anvil.setRequiredAnvilTier(anvilTier);
				}

				anvil.setHotOutput(hot);
				anvil.setRequiredToolType(toolType);

				if (!iAnvilRecipe.getResearch().equalsIgnoreCase("tier")){
					anvil.setRequiredResearch(iAnvilRecipe.getResearch());
				}

				anvil.setRequiredSkill(iAnvilRecipe.getSkill());

				return iAnvilRecipe.getCraftingResult(matrix);
			}
			return ItemStack.EMPTY;
		}
	}
}
