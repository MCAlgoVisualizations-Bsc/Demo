package io.github.mcalgovisualizations.demo.algorithms.GraphSearch;

import io.github.mcalgovisualizations.prefab.Displays.EntityCreatureDisplay;
import io.github.mcalgovisualizations.visualization.renderer.LayoutResult;
import io.github.mcalgovisualizations.visualization.renderer.scene.AbstractScene;
import io.github.mcalgovisualizations.visualization.renderer.scene.SceneContext;
import io.github.mcalgovisualizations.visualization.renderer.scene.VillagerPOV;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphScene extends AbstractScene implements VillagerPOV {
    private static final int SEARCHER_SLOT = -100;
    private final List<ItemEntity> extraEntities = new ArrayList<>();

    private Task blindTask = null;

    public GraphScene(@NotNull SceneContext context) {
        super(context);

        // Periodically check for players riding villagers in this scene and apply/remove blindness
        blindTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    if (instance == null) return;

                    // collect villager entities present in this scene
                    Set<Entity> villagerEntities = new HashSet<>();
                    for (var display : displaysBySlot.values()) {
                        if (display instanceof EntityCreatureDisplay creatureDisplay) {
                            var e = creatureDisplay.getEntity();
                            if (e != null) villagerEntities.add(e);
                        }
                    }

                    // also consider the special camera target if any
                    var cam = cameraTarget();
                    if (cam != null) villagerEntities.add(cam);

                    // for each player in the instance decide if they should have blindness
                    for (Player player : instance.getPlayers()) {
                        boolean riding = false;
                        for (Entity vill : villagerEntities) {
                            if (vill.getPassengers().contains(player)) {
                                riding = true;
                                break;
                            }
                        }

                        boolean hasBlind = player.getActiveEffects().stream()
                                .anyMatch(potion -> potion.potion().effect() == PotionEffect.BLINDNESS);

                        if (riding && !hasBlind) {
                            Potion blindPotion = new Potion(PotionEffect.BLINDNESS, (byte) 0, Potion.INFINITE_DURATION);
                            player.addEffect(blindPotion);
                        } else if (!riding && hasBlind) {
                            player.removeEffect(PotionEffect.BLINDNESS);
                        }
                    }
                })
                .repeat(5, TimeUnit.SERVER_TICK)
                .schedule();
    }

    public Instance getInstance() {
        return this.instance;
    }

    public void addItemEntity(ItemEntity entity) {
        extraEntities.add(entity);
    }

    public Entity cameraTarget() {
        var display = getDisplay(SEARCHER_SLOT);
        if (display instanceof EntityCreatureDisplay creatureDisplay) {
            return creatureDisplay.getEntity();
        }
        return null;
    }

    @Override
    public void setLayout(LayoutResult[] layoutResults) {
        for (int i = 0; i < layoutResults.length; i++) {
            if (layoutResults[i] != null) {
                addDisplay(i, layoutResults[i].displayValue());
            }
        }
    }

    @Override
    public void cleanUp() {
        if (blindTask != null) {
            blindTask.cancel();
            blindTask = null;
        }

        for (ItemEntity entity : extraEntities) {
            entity.remove();
        }
        extraEntities.clear();
        super.cleanUp();
    }
}
