package fun.mntale.seasonalambiance;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.jetbrains.annotations.NotNull;

public class SeasonalAmbianceConfigScreen extends Screen {
    private final Screen parent;
    private final ConfigurationScreen configScreen;
    private Button clearButton;

    public SeasonalAmbianceConfigScreen(ModContainer modContainer, Screen parent) {
        super(Component.literal("Seasonal Ambiance Config"));
        this.parent = parent;
        this.configScreen = new ConfigurationScreen(modContainer, parent);
    }

    @Override
    protected void init() {
        // Initialize the base config screen
        assert this.minecraft != null;
        this.configScreen.init(this.minecraft, this.width, this.height);

        // Add clear particles button at the bottom
        this.clearButton = Button.builder(
                        Component.translatable("seasonalambiance.button.clear_particles"),
                        button -> Seasonalambiance.clearAllParticles()
                )
                .pos(this.width / 2 - 75, this.height - 35)
                .size(150, 20)
                .build();

        this.addRenderableWidget(this.clearButton);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the config screen behind
        this.configScreen.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render our button on top
        this.clearButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(this.parent);
    }
}