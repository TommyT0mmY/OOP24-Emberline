package dev.emberline.game.world.entities.enemies.enemy.concrete;

import com.fasterxml.jackson.databind.JsonNode;
import dev.emberline.core.config.ConfigLoader;
import dev.emberline.game.world.World;
import dev.emberline.game.world.entities.enemies.enemy.AbstractEnemy;
import dev.emberline.game.world.entities.enemies.enemy.EnemyType;
import dev.emberline.utility.Vector2D;

import java.io.Serial;

/**
 * Represents a Pig, a specific type of enemy in the game.
 * This class extends the {@link AbstractEnemy} class to provide
 * the implementations unique to a Pig enemy.
 * The Pig class defines its metadata properties by using
 * a JSON configuration file specified by {@link #ASSET_PATH}.
 * <p>
 * A Pig should represent a normal enemy, that means that all the stats
 * should be pretty balanced
 */
public class Pig extends AbstractEnemy {
    @Serial
    private static final long serialVersionUID = 5359660777148905655L;

    private static final Metadata METADATA;
    private static final String ASSET_PATH = "/sprites/enemyAssets/pig.json";

    static {
        final JsonNode metadataNode = ConfigLoader.loadNode(ASSET_PATH).get("metadata");
        METADATA = ConfigLoader.loadConfig(metadataNode, Metadata.class);
    }

    /**
     * Constructs a Pig enemy at the specified spawn point within the given world.
     *
     * @param spawnPoint the location where the pig should be instantiated
     * @param world      the game world in which the pig exists
     * @see AbstractEnemy#AbstractEnemy(Vector2D, World)
     */
    public Pig(final Vector2D spawnPoint, final World world) {
        super(spawnPoint, world);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Metadata getMetadata() {
        return METADATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EnemyType getEnemyType() {
        return EnemyType.PIG;
    }
}
