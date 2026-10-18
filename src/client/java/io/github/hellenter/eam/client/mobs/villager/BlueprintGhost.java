package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintData;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintTemplate;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintTemplatePayloads;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageBuildModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ghost of a blueprint's building: while the player holds a blueprint, every block of the building shows as a pale
 * translucent cube where he looks, red where something stands in the way. The template comes from the server once and
 * is kept until the player leaves the world.
 */
public final class BlueprintGhost {
	private static final int FREE = ARGB.colorFromFloat(0.28F, 0.55F, 0.8F, 1.0F);
	private static final int BLOCKED = ARGB.colorFromFloat(0.45F, 1.0F, 0.15F, 0.1F);
	private static final int OUTLINE = ARGB.colorFromFloat(0.9F, 0.75F, 0.9F, 1.0F);
	private static final int RECHECK_TICKS = 10;

	private static final Map<Identifier, BlueprintTemplate> TEMPLATES = new HashMap<>();
	private static final Set<Identifier> ASKED = new HashSet<>();
	/** The ghost's blocks, worked out on the client tick and drawn every frame. */
	private static List<Ghost> ghost = List.of();
	private static BlueprintTemplate shownTemplate;
	private static BlockPos shownOrigin;
	private static int shownTurns;
	private static int age;

	private record Ghost(BlockPos pos, boolean air, boolean blocked) {
	}

	private BlueprintGhost() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(BlueprintTemplatePayloads.Reply.TYPE,
				(payload, context) -> TEMPLATES.put(payload.template().id(), payload.template()));
		ClientTickEvents.END_CLIENT_TICK.register(BlueprintGhost::tick);
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
		for (BlueprintTemplate.Placed block : template.placed(origin, turns)) {
			if (minecraft.level.getBlockState(block.pos()).equals(block.state())) continue;
			boolean blocked = BlueprintTemplate.blocked(minecraft.level, block);
			if (block.state().isAir() && !blocked) continue;
			blocks.add(new Ghost(block.pos(), block.state().isAir(), blocked));
		}
		ghost = List.copyOf(blocks);
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
		shownTemplate = null;
	}

	/** Emitted every client tick: the game keeps a tick's gizmos on screen until the next tick. */
	private static void draw() {
		List<Ghost> blocks = ghost;
		if (blocks.isEmpty() || shownTemplate == null) return;
		for (Ghost block : blocks) {
			Gizmos.cuboid(block.pos(), block.blocked() ? 0.02F : 0.0F, GizmoStyle.fill(block.blocked() ? BLOCKED : FREE));
		}
		Gizmos.cuboid(shownTemplate.box(shownOrigin, shownTurns), GizmoStyle.stroke(OUTLINE));
	}
}
