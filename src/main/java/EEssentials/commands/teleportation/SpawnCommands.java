package EEssentials.commands.teleportation;

import EEssentials.EEssentials;
import EEssentials.util.Location;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.command.argument.EntityArgumentType;

import static net.minecraft.server.command.CommandManager.*;

/**
 * Defines the /spawn related commands to manage a universal spawn location.
 */
public class SpawnCommands {

    public static final String SPAWN_SELF_PERMISSION_NODE = "eessentials.spawn.self";
    public static final String SPAWN_OTHER_PERMISSION_NODE = "eessentials.spawn.other";
    public static final String SETSPAWN_PERMISSION_NODE = "eessentials.setspawn";

    /**
     * Registers spawn related commands (/setspawn, /spawn).
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Set a new universal spawn location.
        dispatcher.register(literal("setspawn")
                .requires(Permissions.require(SETSPAWN_PERMISSION_NODE, 2))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;

                    EEssentials.storage.worldSpawns.setSpawn(Location.fromPlayer(player));
                    player.sendMessage(Text.literal("Set Spawn to current location."), false);

                    return 1;
                })
        );

        // Teleport the player or the target to the universal spawn location.
        dispatcher.register(literal("spawn")
                .requires(Permissions.require(SPAWN_SELF_PERMISSION_NODE, 2))
                .executes(ctx -> teleportToSpawn(ctx)) // Teleport the executing player
                .then(argument("target", EntityArgumentType.player())
                        .requires(Permissions.require(SPAWN_OTHER_PERMISSION_NODE, 2))
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                            return teleportToSpawn(ctx, target);  // Teleport the specified player
                        }))
        );
    }

    private static int teleportToSpawn(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = targets.length > 0 ? targets[0] : ctx.getSource().getPlayer();
        if (player == null) return 0;

        Location spawnLocation = EEssentials.storage.worldSpawns.serverSpawn;
        if (spawnLocation != null) {
            spawnLocation.teleport(player);

            if (player.equals(source.getPlayer())) {
                player.sendMessage(Text.of("Teleported to spawn."), false);
            } else {
                source.sendMessage(Text.of("Teleported " + player.getName().getString() + " to spawn."));
            }

            return 1;
        } else {
            player.sendMessage(Text.of("Spawn Location not set."), false);
            return 0; // Return a failure code.
        }
    }

}
