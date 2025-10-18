package fun.mntale.seasonalambiance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Seasonalambiance.MODID)
public class Config {

    private static final ModConfigSpec.Builder BUILDER =
        new ModConfigSpec.Builder();

    // Particle Settings
    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER.comment(
        "Enable or disable falling particles"
    ).define("enabled", true);

    public static final ModConfigSpec.DoubleValue SPAWN_RATE_MULTIPLIER =
        BUILDER.comment(
            "Multiplier for particle spawn rate (0.1 = 10%, 1.0 = 100%, 2.0 = 200%)"
        ).defineInRange("spawnRateMultiplier", 0.2, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue FALL_SPEED_MULTIPLIER =
        BUILDER.comment(
            "Multiplier for particle fall speed (0.5 = 50% speed, 1.0 = normal, 2.0 = 200% speed)"
        ).defineInRange("fallSpeedMultiplier", 0.3, 0.1, 5.0);

    public static final ModConfigSpec.DoubleValue HORIZONTAL_SWAY_MULTIPLIER =
        BUILDER.comment(
            "Multiplier for horizontal sway movement (0.0 = no sway, 1.0 = normal, 2.0 = double sway)"
        ).defineInRange("horizontalSwayMultiplier", 0.2, 0.0, 3.0);

    public static final ModConfigSpec.DoubleValue ROTATION_SPEED_MULTIPLIER =
        BUILDER.comment(
            "Multiplier for particle rotation speed (0.0 = no rotation, 1.0 = normal, 2.0 = double speed)"
        ).defineInRange("rotationSpeedMultiplier", 0.8, 0.0, 3.0);

    public static final ModConfigSpec.IntValue PARTICLE_SIZE = BUILDER.comment(
        "Base size of particles in pixels (8-64)"
    ).defineInRange("particleSize", 12, 8, 64);

    public static final ModConfigSpec.DoubleValue MOUSE_REPULSION_RADIUS =
        BUILDER.comment(
            "Radius around mouse cursor where particles are repelled (0 = disabled, 30 = default)"
        ).defineInRange("mouseRepulsionRadius", 30.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue MOUSE_REPULSION_STRENGTH =
        BUILDER.comment("Strength of mouse repulsion effect").defineInRange(
            "mouseRepulsionStrength",
            6.0,
            0.0,
            20.0
        );

    // Screen-specific settings
    public static final ModConfigSpec.BooleanValue ENABLE_ON_TITLE_SCREEN =
        BUILDER.comment("Enable particles on title screen").define(
            "enableOnTitleScreen",
            true
        );

    public static final ModConfigSpec.BooleanValue ENABLE_ON_INVENTORY =
        BUILDER.comment(
            "Enable particles on inventory/container screens"
        ).define("enableOnInventory", true);

    public static final ModConfigSpec.BooleanValue ENABLE_IN_GAME =
        BUILDER.comment("Enable particles on all other game screens").define(
            "enableInGame",
            true
        );

    // Season overrides
    public static final ModConfigSpec.BooleanValue FORCE_SEASON_ENABLED =
        BUILDER.comment(
            "Force a specific season instead of auto-detecting"
        ).define("forceSeasonEnabled", false);

    public static final ModConfigSpec.EnumValue<SeasonChoice> FORCED_SEASON =
        BUILDER.comment(
            "Which season to force (select from dropdown)"
        ).defineEnum("forcedSeason", SeasonChoice.AUTUMN);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Cached values for faster access
    public static boolean enabled;
    public static double spawnRateMultiplier;
    public static double fallSpeedMultiplier;
    public static double horizontalSwayMultiplier;
    public static double rotationSpeedMultiplier;
    public static int particleSize;
    public static double mouseRepulsionRadius;
    public static double mouseRepulsionStrength;
    public static boolean enableOnTitleScreen;
    public static boolean enableOnInventory;
    public static boolean enableInGame;
    public static boolean forceSeasonEnabled;
    public static SeasonChoice forcedSeason;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Cache config values
        enabled = ENABLED.get();
        spawnRateMultiplier = SPAWN_RATE_MULTIPLIER.get();
        fallSpeedMultiplier = FALL_SPEED_MULTIPLIER.get();
        horizontalSwayMultiplier = HORIZONTAL_SWAY_MULTIPLIER.get();
        rotationSpeedMultiplier = ROTATION_SPEED_MULTIPLIER.get();
        particleSize = PARTICLE_SIZE.get();
        mouseRepulsionRadius = MOUSE_REPULSION_RADIUS.get();
        mouseRepulsionStrength = MOUSE_REPULSION_STRENGTH.get();
        enableOnTitleScreen = ENABLE_ON_TITLE_SCREEN.get();
        enableOnInventory = ENABLE_ON_INVENTORY.get();
        enableInGame = ENABLE_IN_GAME.get();
        forceSeasonEnabled = FORCE_SEASON_ENABLED.get();
        forcedSeason = FORCED_SEASON.get();
    }

    public enum SeasonChoice {
        AUTUMN,
        WINTER,
        SPRING,
        SUMMER,
        NEW_YEAR,
        VALENTINES,
        HALLOWEEN,
        CHRISTMAS,
        PRIDE,
    }
}
