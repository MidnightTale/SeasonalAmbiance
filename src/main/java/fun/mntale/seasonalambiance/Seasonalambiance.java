package fun.mntale.seasonalambiance;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

@Mod(Seasonalambiance.MODID)
public class Seasonalambiance {

    public static final String MODID = "seasonalambiance";

    private static final Minecraft mc = Minecraft.getInstance();
    private static final RandomSource random = RandomSource.create();
    private static final List<FallingImage> fallingObjects = new ArrayList<>(
        200
    );
    private static final Map<Season, List<ResourceLocation>> seasonalTextures =
        new EnumMap<>(Season.class);

    private static Season currentSeason = Season.AUTUMN;
    private static long lastSeasonCheck = 0;
    private static long lastFrameTimeNanos = System.nanoTime();
    private static int lastScreenWidth = 0;
    private static int lastScreenHeight = 0;

    // GPU batch rendering
    private static final ParticleBatchRenderer batchRenderer =
        new ParticleBatchRenderer();

    public Seasonalambiance(IEventBus modEventBus, ModContainer modContainer) {
        // Register config
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // Register config screen factory (built-in NeoForge GUI)
        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            (minecraft, parent) ->
                new net.neoforged.neoforge.client.gui.ConfigurationScreen(
                    modContainer,
                    parent
                )
        );

        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(this::loadSeasonalTextures);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);

        System.out.println(
            "[Seasonal Ambiance] Mod initialized with GPU rendering!"
        );
    }

    private void onRenderGui(ScreenEvent.Render.Post event) {
        // Check if particles are enabled
        if (!Config.enabled) return;

        // Check screen-specific filters
        if (
            event.getScreen() instanceof TitleScreen &&
            !Config.enableOnTitleScreen
        ) return;
        if (
            event.getScreen() instanceof AbstractContainerScreen &&
            !Config.enableOnInventory
        ) return;
        if (
            !(event.getScreen() instanceof TitleScreen) &&
            !(event.getScreen() instanceof AbstractContainerScreen) &&
            !Config.enableInGame
        ) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSeasonCheck > 5000) {
            currentSeason = getCurrentSeason();
            lastSeasonCheck = currentTime;
        }

        List<ResourceLocation> currentTextures = seasonalTextures.get(
            currentSeason
        );
        if (currentTextures == null || currentTextures.isEmpty()) return;

        int screenWidth = event.getScreen().width;
        int screenHeight = event.getScreen().height;

        // Detect GUI scale changes and reposition particles proportionally
        if (
            lastScreenWidth != 0 &&
            lastScreenHeight != 0 &&
            (lastScreenWidth != screenWidth || lastScreenHeight != screenHeight)
        ) {
            float widthRatio = (float) screenWidth / lastScreenWidth;
            float heightRatio = (float) screenHeight / lastScreenHeight;

            // Scale particle positions to match new screen dimensions
            for (FallingImage obj : fallingObjects) {
                obj.x *= widthRatio;
                obj.y *= heightRatio;
            }

            // Remove particles that are still off-screen after scaling
            fallingObjects.removeIf(
                obj -> obj.x < -50 || obj.x > screenWidth + 50 || obj.y < -50
            );
        }

        lastScreenWidth = screenWidth;
        lastScreenHeight = screenHeight;

        float spawnChance = 0.03F;
        float speed = 1.0F;
        float speedVariance = 0.8F;

//        if (event.getScreen() instanceof TitleScreen) {
//            spawnChance = 0.12F;
//        } else if (event.getScreen() instanceof AbstractContainerScreen<?>) {
//            spawnChance = 0.02F;
//            // Removed: speed = 0.3F;
//            // speedVariance = 0.2F;
//        }

        SeasonConfig config = currentSeason.config;
        spawnChance *= (float) (config.spawnMultiplier *
            Config.spawnRateMultiplier);
        speed *= (float) (config.speedMultiplier * Config.fallSpeedMultiplier);

        // Spawn new objects
        if (!currentTextures.isEmpty() && random.nextFloat() < spawnChance) {
            ResourceLocation texture = currentTextures.get(
                random.nextInt(currentTextures.size())
            );
            // Add margin to ensure particles spawn across full width including edges
            float x = -20.0F + random.nextFloat() * (screenWidth + 40.0F);
            float objectSpeed = speed + random.nextFloat() * speedVariance;
            float scale = 0.8F + random.nextFloat() * 0.4F;
            float rotationSpeed = 0.8F + random.nextFloat() * 0.4F;

            fallingObjects.add(
                new FallingImage(
                    texture,
                    x,
                    -20.0F,
                    objectSpeed,
                    scale,
                    rotationSpeed,
                    (float) (config.horizontalSway *
                        Config.horizontalSwayMultiplier)
                )
            );
        }

        // Calculate delta time for frame-rate independent movement
        long nowNanos = System.nanoTime();
        float deltaTime = Math.max(
            0,
            Math.min(0.1F, (nowNanos - lastFrameTimeNanos) / 1_000_000_000.0F)
        );
        lastFrameTimeNanos = nowNanos;

        // Get mouse position once
        double mouseX = mc.mouseHandler.xpos() / mc.getWindow().getGuiScale();
        double mouseY = mc.mouseHandler.ypos() / mc.getWindow().getGuiScale();

        // Update all particles
        for (int i = fallingObjects.size() - 1; i >= 0; i--) {
            FallingImage obj = fallingObjects.get(i);
            obj.update(deltaTime);

            // Remove particles that are off-screen
            if (
                obj.y > screenHeight + 40 ||
                obj.x < -40 ||
                obj.x > screenWidth + 40
            ) {
                fallingObjects.remove(i);
                continue;
            }

            // Mouse repulsion
            // Apply mouse repulsion if enabled
            if (Config.mouseRepulsionRadius > 0) {
                double dx = obj.x - mouseX;
                double dy = obj.y - mouseY;
                double distSq = dx * dx + dy * dy;
                double radiusSq =
                    Config.mouseRepulsionRadius * Config.mouseRepulsionRadius;

                if (distSq < radiusSq && distSq > 1.0E-4D) {
                    double dist = Math.sqrt(distSq);
                    double strength =
                        (1.0D - dist / Config.mouseRepulsionRadius) *
                        Config.mouseRepulsionStrength;
                    obj.x += (float) ((dx / dist) * strength);
                    obj.y += (float) ((dy / dist) * strength);
                }
            }
        }

        // GPU batch render all particles at once
        batchRenderer.renderBatch(event.getGuiGraphics(), fallingObjects);
    }

    @SuppressWarnings("ConstantConditions")
    private static Season getCurrentSeason() {
        LocalDate now = LocalDate.now();
        Month month = now.getMonth();
        int day = now.getDayOfMonth();

        if (month == Month.JANUARY && day <= 3) return Season.NEW_YEAR;
        if (
            month == Month.FEBRUARY && day >= 10 && day <= 14
        ) return Season.VALENTINES;
        if (
            (month == Month.DECEMBER && day >= 15) ||
            (month == Month.JANUARY && day <= 5)
        ) return Season.CHRISTMAS;
        if (month == Month.JUNE) return Season.PRIDE;

        if (
            (month == Month.SEPTEMBER && day >= 22) ||
            month == Month.OCTOBER ||
            month == Month.NOVEMBER ||
            (month == Month.DECEMBER && day <= 14)
        ) return Season.AUTUMN;
        if (
            (month == Month.DECEMBER && day >= 21) ||
            month == Month.JANUARY ||
            month == Month.FEBRUARY ||
            (month == Month.MARCH && day <= 19)
        ) return Season.WINTER;
        if (
            (month == Month.MARCH && day >= 20) ||
            month == Month.APRIL ||
            month == Month.MAY ||
            (month == Month.JUNE && day <= 20)
        ) return Season.SPRING;

        return Season.SUMMER;
    }

    private void loadSeasonalTextures() {
        ResourceManager rm = mc.getResourceManager();
        System.out.println("[Seasonal Ambiance] Loading seasonal textures...");

        for (Season season : Season.values()) {
            String basePath = "textures/gui/ambiance/" + season.folderName;
            ArrayList<ResourceLocation> textures = new ArrayList<>();

            Map<ResourceLocation, Resource> found = rm.listResources(
                basePath,
                name -> name.getPath().endsWith(".png")
            );
            for (ResourceLocation rl : found.keySet()) {
                textures.add(
                    ResourceLocation.fromNamespaceAndPath(MODID, rl.getPath())
                );
            }

            textures.trimToSize();
            seasonalTextures.put(season, textures);
            System.out.println(
                "[Seasonal Ambiance] Loaded " +
                    textures.size() +
                    " textures for " +
                    season.name()
            );
        }
    }

    private enum Season {
        AUTUMN(1.0F, 1.0F, 1.0F, "autumn"),
        WINTER(0.8F, 0.6F, 0.8F, "winter"),
        SPRING(0.9F, 0.8F, 1.2F, "spring"),
        SUMMER(0.7F, 0.5F, 0.9F, "summer"),
        NEW_YEAR(1.15F, 1.1F, 1.5F, "newyear"),
        VALENTINES(0.95F, 0.85F, 1.0F, "valentines"),
        HALLOWEEN(1.1F, 1.0F, 1.4F, "halloween"),
        CHRISTMAS(1.2F, 1.15F, 1.6F, "christmas"),
        PRIDE(0.95F, 0.85F, 1.0F, "pride");

        final SeasonConfig config;
        final String folderName;

        Season(
            float spawnMult,
            float speedMult,
            float sway,
            String folderName
        ) {
            this.config = new SeasonConfig(spawnMult, speedMult, sway);
            this.folderName = folderName;
        }
    }

    private record SeasonConfig(
        float spawnMultiplier,
        float speedMultiplier,
        float horizontalSway
    ) {}

    private static class FallingImage {

        final ResourceLocation texture;
        float x;
        float y;
        final float speed;
        float time;
        final float scale;
        final float rotationSpeed;
        float angle;
        final float horizontalSway;

        FallingImage(
            ResourceLocation texture,
            float x,
            float y,
            float speed,
            float scale,
            float rotationSpeed,
            float horizontalSway
        ) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.scale = scale;
            this.rotationSpeed = rotationSpeed;
            this.time = 0.0F;
            this.angle = random.nextFloat() * 360.0F;
            this.horizontalSway = horizontalSway;
        }

        void update(float deltaTime) {
            // Frame-rate independent movement with dynamic horizontal sway
            deltaTime = Math.min(deltaTime, 0.1F);
            this.y += this.speed * deltaTime * 60.0F;
            this.time += 0.08F * deltaTime * 60.0F;
            this.x +=
                (float) Math.sin(this.time) *
                this.horizontalSway *
                1.2F *
                deltaTime *
                60.0F;
            this.angle +=
                this.rotationSpeed *
                deltaTime *
                60.0F *
                (float) Config.rotationSpeedMultiplier;
        }
    }

    private static class ParticleBatchRenderer {

        void renderBatch(
            GuiGraphics guiGraphics,
            List<FallingImage> particles
        ) {
            if (particles.isEmpty()) return;

            PoseStack poseStack = guiGraphics.pose();

            // Render each particle using GuiGraphics blit (proper integration)
            for (FallingImage particle : particles) {
                poseStack.pushPose();

                // Translate to particle position
                poseStack.translate(particle.x, particle.y, 0);

                // Rotate around center (Z axis)
                poseStack.mulPose(
                    new org.joml.Quaternionf().rotateZ(
                        (float) Math.toRadians(particle.angle)
                    )
                );

                // Calculate size using config
                float size = Config.particleSize * particle.scale;
                int iSize = (int) size;

                // Draw centered
                int halfSize = iSize / 2;
                RenderSystem.setShaderTexture(0, particle.texture);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                Matrix4f matrix = poseStack.last().pose();
                BufferBuilder bufferBuilder = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_TEX
                );

                bufferBuilder
                    .addVertex(matrix, -halfSize, -halfSize, 0)
                    .setUv(0, 0);
                bufferBuilder
                    .addVertex(matrix, -halfSize, halfSize, 0)
                    .setUv(0, 1);
                bufferBuilder
                    .addVertex(matrix, halfSize, halfSize, 0)
                    .setUv(1, 1);
                bufferBuilder
                    .addVertex(matrix, halfSize, -halfSize, 0)
                    .setUv(1, 0);

                BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

                RenderSystem.disableBlend();

                poseStack.popPose();
            }
        }
    }
}
