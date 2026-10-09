package io.github.hellenter.eam.client.brewing;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The vanilla brewing screen with a recipe book button: it opens a panel left of the stand that draws the silver
 * stand's recipes as a tree, water at the top, each reagent on the arrow into the bottle it brews. Read-only.
 * The stand background is drawn here rather than by {@code BrewingStandScreen}, which always draws it centered and
 * would leave it behind when the open panel moves the slots right.
 */
public class SilverBrewingStandScreen extends AbstractContainerScreen<BrewingStandMenu> {
	private static final Identifier FUEL_LENGTH_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/fuel_length");
	private static final Identifier BREW_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");
	private static final Identifier BUBBLES_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/bubbles");
	private static final Identifier BREWING_STAND_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/brewing_stand.png");
	private static final int[] BUBBLE_LENGTHS = {29, 24, 20, 16, 11, 6, 0};
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final Component TITLE = Component.translatable("gui.eam.silver_brewing.recipes");
	private static final int BUTTON_X = 10;
	private static final int BUTTON_Y = 50;
	private static final int PAD = 8;
	private static final int TREE_TOP = 20;
	private static final int COLUMN = 26;
	/** Least room between two neighbouring durations. */
	private static final int TEXT_GAP = 4;
	private static final int SLOT = 18;
	private static final int TEXT = 9;
	/** From a bottle's bottom (or its duration) to the branch line. */
	private static final int STEM = 3;
	/** From the branch line to the next bottle: room for the reagent. */
	private static final int DROP = 20;
	private static final int GAP = 2;
	private static final int BODY = 0xFFC6C6C6;
	private static final int LINE = 0xFF373737;
	private static final int TEXT_COLOR = 0xFF404040;

	/** Stays open or closed for the rest of the game session, like the vanilla recipe book. */
	private static boolean open;

	private @Nullable SilverRecipeTree tree;
	private int[] levelTops = new int[0];
	private boolean[] levelText = new boolean[0];
	private int column = COLUMN;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;

	public SilverBrewingStandScreen(BrewingStandMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
		tree = SilverRecipeTree.build(minecraft.level.recipeAccess());
		if (tree.isEmpty()) {
			tree = null;
			return;
		}
		measure();
		place();
		addRenderableWidget(new ImageButton(leftPos + BUTTON_X, topPos + BUTTON_Y, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
			open = !open;
			place();
			button.setPosition(leftPos + BUTTON_X, topPos + BUTTON_Y);
		}));
	}

	/** Panel size and the top of each tree level; a level with durations under its bottles gets a line of text more. */
	private void measure() {
		int depth = tree.depth();
		levelTops = new int[depth];
		levelText = new boolean[depth];
		float tickRate = minecraft.level.tickRateManager().tickrate();
		column = COLUMN;
		for (int i = 0; i < tree.nodes().size(); i++) {
			Component duration = SilverRecipeTree.duration(tree.nodes().get(i).stack(), tickRate);
			if (duration != null) {
				levelText[tree.placement(i).depth()] = true;
				column = Math.max(column, font.width(duration) + TEXT_GAP);
			}
		}
		int y = TREE_TOP;
		for (int d = 0; d < depth; d++) {
			levelTops[d] = y;
			y = bottom(d) + STEM + DROP;
		}
		panelHeight = bottom(depth - 1) + PAD - 2;
		panelWidth = Math.max(tree.columns() * column, font.width(TITLE)) + 2 * PAD;
	}

	/** Moves the stand right to make room for an open panel, as the vanilla recipe book does. */
	private void place() {
		int centered = (width - imageWidth) / 2;
		leftPos = open ? Math.min(centered + (panelWidth + GAP) / 2, width - imageWidth) : centered;
		panelX = Math.max(0, leftPos - panelWidth - GAP);
		panelY = Math.max(0, topPos + (imageHeight - panelHeight) / 2);
	}

	/** Bottom of a level's bottles, durations included. */
	private int bottom(int depth) {
		return levelTops[depth] + SLOT + (levelText[depth] ? TEXT : 0);
	}

	private int centerX(int node) {
		return panelX + PAD + (int) (tree.placement(node).column() * column) + column / 2;
	}

	private int top(int node) {
		return panelY + levelTops[tree.placement(node).depth()];
	}

	private int branchY(int parent) {
		return panelY + bottom(tree.placement(parent).depth()) + STEM;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		extractStand(graphics);
		if (open && tree != null) {
			extractPanel(graphics);
		}
	}

	/** The vanilla brewing stand background with its fuel bar, progress arrow and bubbles, at leftPos. */
	private void extractStand(GuiGraphicsExtractor graphics) {
		int xo = leftPos;
		int yo = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BREWING_STAND_LOCATION, xo, yo, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
		int fuel = menu.getFuel();
		int totalFuel = menu.getTotalFuel();
		int fuelLength = totalFuel > 0 ? Mth.clamp(Mth.positiveCeilDiv(18 * fuel, totalFuel), 0, 18) : 0;
		if (fuelLength > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_LENGTH_SPRITE, 18, 4, 0, 0, xo + 60, yo + 44, fuelLength, 4);
		}
		int ticks = menu.getBrewingTicks();
		int totalTicks = menu.getTotalBrewingTicks();
		if (ticks > 0 && totalTicks > 0) {
			int length = (int) (28.0F * (1.0F - (float) ticks / totalTicks));
			if (length > 0) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BREW_PROGRESS_SPRITE, 9, 28, 0, 0, xo + 97, yo + 16, 9, length);
			}
			length = BUBBLE_LENGTHS[ticks / 2 % 7];
			if (length > 0) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUBBLES_SPRITE, 12, 29, 0, 29 - length, xo + 63, yo + 14 + 29 - length, 12, length);
			}
		}
	}

	private void extractPanel(GuiGraphicsExtractor graphics) {
		int x0 = panelX;
		int y0 = panelY;
		int x1 = panelX + panelWidth;
		int y1 = panelY + panelHeight;
		graphics.fill(x0 + 1, y0, x1 - 1, y1, 0xFF000000);
		graphics.fill(x0, y0 + 1, x1, y1 - 1, 0xFF000000);
		graphics.fill(x0 + 1, y0 + 1, x1 - 3, y1 - 3, 0xFFFFFFFF);
		graphics.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, 0xFF555555);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, BODY);
		graphics.text(font, TITLE, x0 + (panelWidth - font.width(TITLE)) / 2, y0 + 7, TEXT_COLOR, false);

		List<SilverRecipeTree.Node> nodes = tree.nodes();
		// Lines first: the stem under each parent, the branch across its children, a drop with an arrowhead to each child.
		for (int i = 0; i < nodes.size(); i++) {
			int parent = nodes.get(i).parent();
			if (parent < 0) {
				continue;
			}
			int px = centerX(parent);
			int cx = centerX(i);
			int by = branchY(parent);
			int ty = top(i);
			graphics.fill(px, panelY + bottom(tree.placement(parent).depth()), px + 1, by + 1, LINE);
			graphics.fill(Math.min(px, cx), by, Math.max(px, cx) + 1, by + 1, LINE);
			graphics.fill(cx, by, cx + 1, ty - 1, LINE);
			graphics.fill(cx - 2, ty - 3, cx + 3, ty - 2, LINE);
			graphics.fill(cx - 1, ty - 2, cx + 2, ty - 1, LINE);
		}
		float tickRate = minecraft.level.tickRateManager().tickrate();
		for (int i = 0; i < nodes.size(); i++) {
			SilverRecipeTree.Node node = nodes.get(i);
			int cx = centerX(i);
			int ty = top(i);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, cx - SLOT / 2, ty, SLOT, SLOT);
			graphics.item(node.stack(), cx - 8, ty + 1);
			Component duration = SilverRecipeTree.duration(node.stack(), tickRate);
			if (duration != null) {
				graphics.text(font, duration, cx - font.width(duration) / 2 + 1, ty + SLOT + 1, TEXT_COLOR, false);
			}
			if (!node.reagent().isEmpty()) {
				int ry = reagentY(i);
				graphics.fill(cx - 8, ry, cx + 8, ry + 16, BODY);
				graphics.item(node.reagent(), cx - 8, ry);
			}
		}
	}

	private int reagentY(int node) {
		return branchY(node(node).parent()) + 1;
	}

	private SilverRecipeTree.Node node(int node) {
		return tree.nodes().get(node);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (!open || tree == null || !menu.getCarried().isEmpty()) {
			return;
		}
		ItemStack hovered = hoveredInPanel(mouseX, mouseY);
		if (hovered != null) {
			graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
		}
	}

	private @Nullable ItemStack hoveredInPanel(double mouseX, double mouseY) {
		for (int i = 0; i < tree.nodes().size(); i++) {
			SilverRecipeTree.Node node = node(i);
			int x = centerX(i) - 8;
			if (inside(mouseX, mouseY, x, top(i) + 1)) {
				return node.stack();
			}
			if (!node.reagent().isEmpty() && inside(mouseX, mouseY, x, reagentY(i))) {
				return node.reagent();
			}
		}
		return null;
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y) {
		return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
	}

	@Override
	protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
		boolean inPanel = open && tree != null
				&& mx >= panelX && mx < panelX + panelWidth && my >= panelY && my < panelY + panelHeight;
		return super.hasClickedOutside(mx, my, xo, yo) && !inPanel;
	}
}
