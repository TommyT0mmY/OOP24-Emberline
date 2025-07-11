package dev.emberline.game.world.buildings;

import dev.emberline.core.components.InputComponent;
import dev.emberline.core.components.RenderComponent;
import dev.emberline.core.components.UpdateComponent;
import dev.emberline.core.sounds.AudioController;
import dev.emberline.core.sounds.event.SfxSoundEvent.SoundType;
import dev.emberline.game.world.Building;
import dev.emberline.game.world.World;
import dev.emberline.game.world.buildings.tower.Tower;
import dev.emberline.game.world.buildings.towerloader.TowerLoader.TowerToLoad;
import dev.emberline.game.world.buildings.towerprebuild.TowerPreBuild;
import dev.emberline.gui.towerdialog.NewBuildDialogLayer;
import dev.emberline.gui.towerdialog.TowerDialogLayer;
import dev.emberline.utility.Coordinate2D;
import dev.emberline.utility.Vector2D;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Manages the lifecycle and interactions of towers and pre-built tower in the world.
 * Responsible for rendering, updating, and handling input for towers and their associated dialogs.
 */
public class TowersManager implements UpdateComponent, RenderComponent, InputComponent, Serializable {

    @Serial
    private static final long serialVersionUID = 8310031147169513653L;

    private transient TowerDialogLayer towerDialogLayer;
    private transient NewBuildDialogLayer newBuildDialogLayer;

    private final Set<Building> buildings = new HashSet<>();
    private final Collection<TowerPreBuild> toBuild = new LinkedList<>();

    private final World world;

    /**
     * Constructs a new instance of the {@code TowersManager} class and initializes its internal state.
     *
     * @param world the {@link World} instance to which this {@code TowersManager} belongs, providing
     *              access to game-related resources and functionality
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "This is intended behavior as this class needs a reference to world,"
                    + " to instantiate new towers."
    )
    public TowersManager(final World world) {
        this.world = world;
    }

    /**
     * Adds the new {@code TowerPreBuild} instances, form the new wave, to the existing buildings.
     *
     * @param towerToLoad of the tower.
     */
    public void addTowersToBuild(final List<TowerToLoad> towerToLoad) {
        buildings.addAll(towerToLoad
                .stream()
                .map(tower -> new TowerPreBuild(
                        new Coordinate2D(tower.x(), tower.y()), this))
                .toList()
        );
    }

    /**
     * Opens a new build dialog for the provided tower. If a dialog is already open for a
     * different tower, it closes the current dialog and creates a new one. The newly created
     * dialog is linked to the specified tower and assigns the current player as its listener.
     *
     * @param tower the {@code TowerPreBuild} instance for which the new build dialog is to be opened
     */
    public void openNewBuildDialog(final TowerPreBuild tower) {
        if (newBuildDialogLayer == null || !newBuildDialogLayer.getTowerPreBuild().equals(tower)) {
            AudioController.requestSfxSound(this, SoundType.OPEN_DIALOG_CHAINS);
            newBuildDialogLayer = new NewBuildDialogLayer(tower);
        }
    }

    /**
     * Opens a dialog for interacting with the specified tower. If a dialog is already open for
     * a different tower, it closes the current dialog and creates a new one for the given tower.
     *
     * @param tower the tower attached to the tower dialog to be opened
     */
    public void openTowerDialog(final Tower tower) {
        if (towerDialogLayer == null || !towerDialogLayer.getTower().equals(tower)) {
            AudioController.requestSfxSound(this, SoundType.OPEN_DIALOG_CHAINS);
            towerDialogLayer = new TowerDialogLayer(tower);
        }
    }

    /**
     * Closes the currently open new build dialog.
     */
    public void closeNewBuildDialog() {
        newBuildDialogLayer = null;
    }

    /**
     * Closes the currently open tower dialog.
     */
    public void closeTowerDialog() {
        towerDialogLayer = null;
    }

    /**
     * Adds the provided {@code TowerPreBuild} instance to the list of towers to be built.
     * If the given {@code TowerPreBuild} instance is not in the {@code buildings} set, an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param preBuild the {@code TowerPreBuild} object representing the tower's pre-build state
     *                 that is intended to be added for construction
     * @throws IllegalArgumentException if {@code preBuild} is not contained within the {@code buildings} set
     */
    public void buildTower(final TowerPreBuild preBuild) {
        if (!buildings.contains(preBuild)) {
            throw new IllegalArgumentException("preBuild must be already added to the buildings set");
        }
        toBuild.add(preBuild);
    }

    /**
     * Processes the inputs of the eventual dialogs and all the buildings.
     * It also closes the eventual dialogs if clicked elsewhere.
     *
     * @param inputEvent the input event to process
     */
    @Override
    public void processInput(final InputEvent inputEvent) {
        if (inputEvent.isConsumed()) {
            return;
        }

        if (newBuildDialogLayer != null) {
            newBuildDialogLayer.processInput(inputEvent);
        }

        if (towerDialogLayer != null) {
            towerDialogLayer.processInput(inputEvent);
        }

        for (final Building building : buildings) {
            if (inputEvent.isConsumed()) {
                return;
            }
            building.processInput(inputEvent);
        }

        // Clicking elsewhere closes the active tower dialog
        if (!inputEvent.isConsumed() && inputEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
            closeNewBuildDialog();
            closeTowerDialog();
        }
    }

    /**
     * Renders the current state of the {@code TowersManager}, including its dialogs
     * and all the associated {@code Building} objects.
     *
     * @see RenderComponent#render()
     */
    @Override
    public void render() {
        if (newBuildDialogLayer != null) {
            newBuildDialogLayer.render();
        }

        if (towerDialogLayer != null) {
            towerDialogLayer.render();
        }

        for (final Building building : buildings) {
            building.render();
        }
    }

    /**
     * Updates all buildings and builds all pre-build towers awaiting construction.
     *
     * @param elapsed the time elapsed since the last update in nanoseconds
     */
    @Override
    public void update(final long elapsed) {
        for (final Building building : buildings) {
            building.update(elapsed);
        }

        for (final TowerPreBuild preBuild : toBuild) {
            buildings.remove(preBuild);
            final Vector2D towerLocationBottomLeft = new Coordinate2D(
                    preBuild.getWorldTopLeft().getX(), preBuild.getWorldBottomRight().getY()
            );
            closeNewBuildDialog();
            buildings.add(new Tower(towerLocationBottomLeft, world));
        }
        toBuild.clear();
    }
}
