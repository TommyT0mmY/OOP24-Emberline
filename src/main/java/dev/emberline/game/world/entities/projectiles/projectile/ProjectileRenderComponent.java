package dev.emberline.game.world.entities.projectiles.projectile;

import dev.emberline.core.GameLoop;
import dev.emberline.core.components.RenderComponent;
import dev.emberline.core.components.UpdateComponent;
import dev.emberline.core.render.CoordinateSystem;
import dev.emberline.core.render.RenderPriority;
import dev.emberline.core.render.RenderTask;
import dev.emberline.core.render.Renderer;
import dev.emberline.game.world.entities.projectiles.projectile.Projectile.PositionAndRotation;
import dev.emberline.utility.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.io.Serial;
import java.io.Serializable;

class ProjectileRenderComponent implements RenderComponent, Serializable {

    @Serial
    private static final long serialVersionUID = 7918701324023489282L;

    private static final double WIDTH = 1;
    private static final double HEIGHT = 1;

    private final Projectile owner;
    private final ProjectileAnimation projectileAnimation;

    ProjectileRenderComponent(final Projectile owner) {
        this.owner = owner;
        this.projectileAnimation = new ProjectileAnimation(owner);
    }

    /**
     * This method retrieves the current position and rotation of the projectile
     * and translates it to the screen coordinate system. It then rotates the screen space
     * by the rotation amount and draws the current frame of the {@link ProjectileAnimation}
     * to have the effect of the projectile following the trajectory.
     */
    @Override
    public void render() {
        final Renderer renderer = GameLoop.getInstance().getRenderer();
        final GraphicsContext gc = renderer.getGraphicsContext();
        final CoordinateSystem cs = renderer.getWorldCoordinateSystem();

        final PositionAndRotation posAndRot = owner.getPositionAndRotation();
        final Vector2D position = posAndRot.position();
        final double rotation = posAndRot.rotation();

        final double screenWidth = WIDTH * cs.getScale();
        final double screenHeight = HEIGHT * cs.getScale();

        final double positionScreenX = cs.toScreenX(position.getX());
        final double positionScreenY = cs.toScreenY(position.getY());

        final Image currentFrame = projectileAnimation.getImage();

        renderer.addRenderTask(new RenderTask(RenderPriority.PROJECTILES, () -> {
            gc.save();

            gc.translate(positionScreenX, positionScreenY);
            gc.rotate(rotation);

            // make so that the tip of the projectile hits
            gc.drawImage(currentFrame, -screenWidth / 2, -screenHeight / 2, screenWidth, screenHeight);

            gc.restore();
        }));
    }

    UpdateComponent getAnimationUpdatable() {
        return projectileAnimation;
    }
}
