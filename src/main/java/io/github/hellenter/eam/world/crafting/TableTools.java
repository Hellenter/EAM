package io.github.hellenter.eam.world.crafting;

import java.util.List;

/**
 * What the tools in a crafting table do for one craft.
 *
 * @param knives  tiers of the knives in the table
 * @param saws    tiers of the saws
 * @param hammers tiers of the hammers
 * @param metal   whether the grid holds metal, so hammers help
 */
public record TableTools(List<Integer> knives, List<Integer> saws, List<Integer> hammers, boolean metal) {
}
