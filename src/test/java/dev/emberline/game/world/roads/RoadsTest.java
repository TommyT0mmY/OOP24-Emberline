package dev.emberline.game.world.roads;

import dev.emberline.utility.Coordinate2D;
import dev.emberline.utility.Vector2D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class RoadsTest {

    private static final String ROADS_PATH = "/roads/";
    private final Roads roads = new Roads(ROADS_PATH);

    @Test
    void testGetNextDifferentWeights() {
        final Vector2D zeroZero = new Coordinate2D(0.5, 0.5);
        final Vector2D oneZero = new Coordinate2D(1.5, 0.5);
        final Vector2D zeroOne = new Coordinate2D(0.5, 1.5);
        Optional<Vector2D> next;

        next = roads.getNext(zeroZero);
        Assertions.assertTrue(next.isPresent());
        Assertions.assertEquals(next.get(), oneZero);

        next = roads.getNext(zeroZero);
        Assertions.assertTrue(next.isPresent());
        Assertions.assertEquals(next.get(), zeroOne);

        next = roads.getNext(zeroZero);
        Assertions.assertTrue(next.isPresent());
        Assertions.assertEquals(next.get(), zeroOne);

        next = roads.getNext(zeroZero);
        Assertions.assertTrue(next.isPresent());
        Assertions.assertEquals(next.get(), oneZero);
    }

    @Test
    void testGetNextWithZeroWeight() {
        final Vector2D oneOne = new Coordinate2D(1.5, 1.5);
        final Vector2D twoOne = new Coordinate2D(2.5, 1.5);
        Optional<Vector2D> next;

        next = roads.getNext(oneOne);
        Assertions.assertTrue(next.isPresent());
        Assertions.assertEquals(next.get(), twoOne);

        next = roads.getNext(oneOne);
        Assertions.assertTrue(next.isPresent());
        Assertions.assertEquals(next.get(), twoOne);
    }

    @Test
    void testGetNextLastNodeGivesEmpty() {
        final Vector2D oneZero = new Coordinate2D(1.5, 0.5);
        final Vector2D zeroOne = new Coordinate2D(0.5, 1.5);
        final Vector2D twoOne = new Coordinate2D(2.5, 1.5);
        final Vector2D oneTwo = new Coordinate2D(1.5, 2.5);

        Assertions.assertFalse(roads.getNext(oneZero).isPresent());
        Assertions.assertFalse(roads.getNext(zeroOne).isPresent());
        Assertions.assertFalse(roads.getNext(twoOne).isPresent());
        Assertions.assertFalse(roads.getNext(oneTwo).isPresent());
    }
}
