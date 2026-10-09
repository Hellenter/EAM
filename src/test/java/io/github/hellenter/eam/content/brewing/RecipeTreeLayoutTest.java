package io.github.hellenter.eam.content.brewing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

class RecipeTreeLayoutTest {
	@Test
	void parentSitsOverItsChildren() {
		// water -> holy water -> three potions, the first and last with a splash version
		int[] parents = {-1, 0, 1, 1, 1, 2, 4};
		List<RecipeTreeLayout.Placement> placements = RecipeTreeLayout.layout(parents);
		assertEquals(3, RecipeTreeLayout.width(parents));
		assertEquals(new RecipeTreeLayout.Placement(0, 2), placements.get(2));
		assertEquals(new RecipeTreeLayout.Placement(1, 2), placements.get(3));
		assertEquals(new RecipeTreeLayout.Placement(2, 2), placements.get(4));
		assertEquals(new RecipeTreeLayout.Placement(0, 3), placements.get(5));
		assertEquals(new RecipeTreeLayout.Placement(2, 3), placements.get(6));
		assertEquals(new RecipeTreeLayout.Placement(1, 1), placements.get(1));
		assertEquals(new RecipeTreeLayout.Placement(1, 0), placements.get(0));
	}

	@Test
	void rootsSitSideBySide() {
		int[] parents = {-1, 0, 0, -1};
		List<RecipeTreeLayout.Placement> placements = RecipeTreeLayout.layout(parents);
		assertEquals(0.5, placements.get(0).column());
		assertEquals(2, placements.get(3).column());
		assertEquals(3, RecipeTreeLayout.width(parents));
	}
}
