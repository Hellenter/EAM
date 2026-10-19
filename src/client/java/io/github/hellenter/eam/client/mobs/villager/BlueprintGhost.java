package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintData;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintLogic;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintTemplate;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintTemplatePayloads;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageBuildModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ghost of a blueprint's building: while the player holds a blueprint, every block of the building shows as a pale
 * translucent cube where he looks, red where something stands in the way, with the building's outline drawn sharp and an
 * arrow on the ground out of the side it faces. Above the hotbar a hint names the keys that build and turn it. The
 * template comes from the server once and is kept until the player leaves the world.
 */
public final class BlueprintGhost {
	private static final int FREE = ARGB.colorFromFloat(0.22F, 0.55F, 0.8F, 1.0F);
	private static final int BLOCKED = ARGB.colorFromFloat(0.45F, 1.0F, 0.15F, 0.1F);
	private static final int EDGE = ARGB.colorFromFloat(0.95F, 0.1F, 0.3F, 0.55F);
	private static final int ARROW = ARGB.colorFromFloat(1.0F, 1.0F, 0.85F, 0.2F);
	private static final float EDGE_WIDTH = 2.5F;
	private static final float ARROW_WIDTH = 6.0F;
	private static final double ARROW_LENGTH = 4.0;
	private static final int HINT = 0xFFFFFFFF;
	private static final int HINT_BLOCKED = 0xFFFF5555;
	private static final int RECHECK_TICKS = 10;

	private static final Map<Identifier, BlueprintTemplate> TEMPLATES = new HashMap<>();
	private static final Set<Identifier> ASKED = new HashSet<>();
	/** The ghost's blocks, worked out on the client tick and drawn every frame. */
	private static List<Ghost> ghost = List.of();
	private static List<BlueprintLogic.Edge> edges = List.of();
	private static int blockedCount;
	private static boolean holding;
	private static BlueprintTemplate shownTemplate;
	private static BlockPos shownOrigin;
	private static int shownTurns;
	private static int age;
	private static BlueprintTemplate edgesTemplate;
	private static BlockPos edgesOrigin;
	private static int edgesTurns;

	private record Ghost(BlockPos pos, boolean air, boolean blocked) {
	}

	private BlueprintGhost() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(BlueprintTemplatePayloads.Reply.TYPE,
				(payload, context) -> TEMPLATES.put(payload.template().id(), payload.template()));
		ClientTickEvents.END_CLIENT_TICK.register(BlueprintGhost::tick);
		HudElementRegistry.addLast(Eam.id("blueprint_hint"), BlueprintGhost::hint);
	}

	/** The keys that build and turn the blueprint, above the hotbar, and how many blocks are in the way. */
	private static void hint(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!holding) return;
		Font font = minecraft.font;
		Component use = minecraft.options.keyUse.getTranslatedKeyMessage();
		Component sneak = minecraft.options.keyShift.getTranslatedKeyMessage();
		Component text = Component.translatable("item.eam.blueprint.hint", use, sneak);
		int y = graphics.guiHeight() - 86;
		graphics.text(font, text, (graphics.guiWidth() - font.width(text)) / 2, y, HINT, true);
		if (blockedCount > 0 && shownTemplate != null) {
			Component blocked = Component.translatable("item.eam.blueprint.blocked", blockedCount);
			graphics.text(font, blocked, (graphics.guiWidth() - font.width(blocked)) / 2, y - 11, HINT_BLOCKED, true);
		}
	}

	private static void tick(Minecraft minecraft) {
		Player player = minecraft.player;
		if (player == null || minecraft.level == null) {
			TEMPLATES.clear();
			ASKED.clear();
			clear();
			return;
		}
		BlueprintData data = held(player);
		holding = data != null;
		if (data == null) {
			clear();
			return;
		}
		BlueprintTemplate template = TEMPLATES.get(data.template());
		if (template == null) {
			if (ASKED.add(data.template())) ClientPlayNetworking.send(new BlueprintTemplatePayloads.Request(data.template()));
			clear();
			return;
		}
		BlockPos look = BlueprintTemplate.target(player, 1.0F);
		if (look == null) {
			clear();
			return;
		}
		BlockPos origin = template.origin(look, data.turns());
		age++;
		if (template != shownTemplate || !origin.equals(shownOrigin) || data.turns() != shownTurns || age >= RECHECK_TICKS) {
			recheck(minecraft, template, origin, data.turns());
		}
		draw();
	}

	private static void recheck(Minecraft minecraft, BlueprintTemplate template, BlockPos origin, int turns) {
		age = 0;
		shownTemplate = template;
		shownOrigin = origin;
		shownTurns = turns;
		List<Ghost> blocks = new ArrayList<>();
		Set<BlockPos> solid = new HashSet<>();
		int blocked = 0;
		for (BlueprintTemplate.Placed block : template.placed(origin, turns)) {
			if (!block.state().isAir()) solid.add(block.pos());
			if (minecraft.level.getBlockState(block.pos()).equals(block.state())) continue;
			boolean inWay = BlueprintTemplate.blocked(minecraft.level, block);
			if (inWay) blocked++;
			if (block.state().isAir() && !inWay) continue;
			blocks.add(new Ghost(block.pos(), block.state().isAir(), inWay));
		}
		ghost = List.copyOf(blocks);
		blockedCount = blocked;
		// The outline only changes with the building's place or turn, not with what stands in the way.
		if (template != edgesTemplate || !origin.equals(edgesOrigin) || turns != edgesTurns) {
			edges = BlueprintLogic.outline(solid);
			edgesTemplate = template;
			edgesOrigin = origin;
			edgesTurns = turns;
		}
	}

	private static BlueprintData held(Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			BlueprintData data = stack.get(VillageBuildModule.BLUEPRINT);
			if (data != null) return data;
		}
		return null;
	}

	private static void clear() {
		ghost = List.of();
		edges = List.of();
		edgesTemplate = null;
		blockedCount = 0;
		shownTemplate = null;
	}

	/** Emitted every client tick: the game keeps a tick's gizmos on screen until the next tick. */
	private static void draw() {
		List<Ghost> blocks = ghost;
		if (blocks.isEmpty() || shownTemplate == null) return;
		for (Ghost block : blocks) {
			Gizmos.cuboid(block.pos(), block.blocked() ? 0.02F : 0.0F, GizmoStyle.fill(block.blocked() ? BLOCKED : FREE));
		}
		for (BlueprintLogic.Edge edge : edges) {
			Vec3 start = new Vec3(edge.x(), edge.y(), edge.z());
			Vec3 end = start.add(edge.axis() == 0 ? edge.length() : 0, edge.axis() == 1 ? edge.length() : 0, edge.axis() == 2 ? edge.length() : 0);
			Gizmos.line(start, end, EDGE, EDGE_WIDTH);
		}
		arrow(shownTemplate.box(shownOrigin, shownTurns), shownTemplate.front(shownTurns));
	}

	/** A flat arrow on the ground out of the side the building faces, seen through blocks. */
	private static void arrow(AABB box, Direction front) {
		Vec3 center = box.getCenter();
		double y = box.minY + 1.05;
		double half = front.getAxis() == Direction.Axis.X ? box.getXsize() / 2 : box.getZsize() / 2;
		Vec3 out = new Vec3(front.getStepX(), 0, front.getStepZ());
		Vec3 side = new Vec3(-out.z, 0, out.x);
		Vec3 start = new Vec3(center.x, y, center.z).add(out.scale(half + 0.5));
		Vec3 tip = start.add(out.scale(ARROW_LENGTH));
		Gizmos.line(start, tip, ARROW, ARROW_WIDTH).setAlwaysOnTop();
		Gizmos.line(tip, tip.subtract(out.scale(1.4)).add(side.scale(1.2)), ARROW, ARROW_WIDTH).setAlwaysOnTop();
		Gizmos.line(tip, tip.subtract(out.scale(1.4)).subtract(side.scale(1.2)), ARROW, ARROW_WIDTH).setAlwaysOnTop();
	}
}
