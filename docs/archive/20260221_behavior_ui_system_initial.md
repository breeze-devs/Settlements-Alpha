# Building a Live-Refreshing Custom UI in Minecraft (NeoForge)

This guide explains how to build a custom, live-refreshing UI in modern Minecraft (using NeoForge). Because this is a "status board" without actual inventory item slots (like moving items from your backpack to a chest), you don't need a complex `Menu`/`Container` setup. You just need a **Client-Side Screen** that receives constant data updates from the **Server**.

## UI Mockup
20260221_behavior_ui_mockup.png

## Architecture

Here is the architecture to achieve a smooth, live-updating Minecraft UI:

### 1. Define the Sync Packet (Network layer)
Minecraft's UI is strictly client-side, but the villager's behavior logic runs on the server side. To "live refresh", you must send custom network packets from the server to the client.

First, define a custom payload (e.g., `BehaviorSyncPacket`) containing the state of the 8 behaviors:

```java
public record BehaviorSyncPacket(List<BehaviorData> rows) implements CustomPacketPayload {
    public static final Type<BehaviorSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mymod", "behavior_sync"));

    // Nested record to hold a single row's UI state
    public record BehaviorData(
        String title, 
        ItemStack icon,
        boolean isRunning, 
        String stageName, 
        boolean preconditionMet,
        float progressPct, 
        float cooldownSeconds
    ) {}

    // ... standard NeoForge buf codecs ...
}
```

### 2. Create the GUI Screen (Client layer)
Create a class that extends `net.minecraft.client.gui.screens.Screen`. This class will hold the latest `BehaviorData` and render it every frame.

```java
public class BehaviorControllerScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mymod", "textures/gui/behavior_controller.png");
    private List<BehaviorData> currentData = new ArrayList<>();
    
    public BehaviorControllerScreen() {
        super(Component.literal("Behavior Controller"));
    }

    // This method is used to LIVE UPDATE the data without closing the screen
    public void updateData(List<BehaviorData> newData) {
        this.currentData = newData;
    }

    @Override
    protected void init() {
        super.init();
        // Add your "Enable All" and "Back" buttons here
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
            .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick); // Darkens the game background

        // 1. Draw your custom UI background panel
        int x = (this.width - 256) / 2;
        int y = (this.height - 200) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, 256, 200);

        // 2. Draw the Header
        guiGraphics.drawString(this.font, this.title, x + 8, y + 8, 0xFFFFFF, false);

        // 3. Draw the Live Rows
        int rowY = y + 25;
        for (BehaviorData row : currentData) {
            renderBehaviorRow(guiGraphics, row, x + 10, rowY);
            rowY += 20; // Move down for the next row
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick); // Renders the buttons
    }

    private void renderBehaviorRow(GuiGraphics guiGraphics, BehaviorData row, int x, int y) {
        // Render Icon
        guiGraphics.renderItem(row.icon(), x, y);
        
        // Render Title
        guiGraphics.drawString(this.font, row.title(), x + 20, y + 4, 0xFFFFFF, false);
        
        // Render State Text (Green if Started, Red if Stopped)
        int stateColor = row.isRunning() ? 0x55FF55 : 0xFF5555;
        guiGraphics.drawString(this.font, row.isRunning() ? "Started" : "Stopped", x + 120, y + 4, stateColor, false);
        
        // Render Cooldown Progress Bar (Dynamically change width based on cooldown)
        if (row.cooldownSeconds() > 0) {
            int barWidth = (int) ((row.cooldownSeconds() / MAX_COOLDOWN) * 40);
            guiGraphics.fill(x + 180, y + 4, x + 180 + barWidth, y + 8, 0xFF00AAFF); // Blue bar
            guiGraphics.drawString(this.font, String.format("%.1fs", row.cooldownSeconds()), x + 225, y + 4, 0xAAAAAA, false);
        }
    }
}
```

### 3. Handle the Live Update
When the client receives the `BehaviorSyncPacket`, intercept it and update the screen if it's currently open. This prevents flickering because you aren't re-opening the GUI, you are just updating the variables inside it.

```java
public static void handleSyncPacket(BehaviorSyncPacket payload, IPayloadContext context) {
    context.enqueueWork(() -> {
        // Ensure we are on the client
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof BehaviorControllerScreen behaviorScreen) {
            // Live-feed the new data into the currently open screen
            behaviorScreen.updateData(payload.rows());
        }
    });
}
```

### 4. Push Updates from the Server
On the Server, wherever your `BaseVillager` processes its behavior ticks, you just need to occasionally stream this packet to the player who is viewing the UI.

To prevent spamming the network, you can either:
1. Send the packet **only when a state changes** (e.g., from *Idle* to *Running*, or *Precondition Failed* to *Passed*).
2. For smooth progress and cooldown bars, send it **every 2-5 ticks** (roughly 10 times a second) to the specific player looking at the UI.

```java
// On the server tick loop:
if (player.containerMenu != null /* or however you track if they are looking at the UI */) {
    List<BehaviorData> displayData = collectBehaviorDataFromVillager(villager);
    PacketDistributor.sendToPlayer(player, new BehaviorSyncPacket(displayData));
}
```

### Summary of the flow:
**Player right clicks Villager -> Server says "Open Screen" -> Client opens `BehaviorControllerScreen` -> Server streams `BehaviorSyncPacket` 10 times a second -> Client's `render()` method instantly redraws the UI with the fresh variables.**

## Refined Product Requirements (Game Jam Decisions)

Based on our architectural discussions, we have simplified the MVP and defined the following product directions:

### 1. Stage Visualization (Simplified MVP)
We are deferring the complex graph/flowchart visualization. Instead of building an entire static flowchart of stages for each behavior, the UI will simply **surface the current active stage** of the running behavior. This avoids the massive architectural overhead of refactoring `StagedStep` to explicitly declare its flow graph upfront, and still gives the player great visibility into what the villager is doing right now.

### 2. Behavior Sorting and Queueing
Since a single villager might have 10+ behaviors registered to them, the UI layout will dynamically sort the list rather than just showing a massive static list:
- **Top:** The currently **Active/Running** behavior.
- **Middle:** Behaviors that are **Ready/Queued** (e.g., preconditions are met, waiting for cooldown, or queued by future LLM-powered logic).
- **Bottom:** **Inactive** behaviors (preconditions not met, disabled, etc.).

### 3. Network Optimization (Deferred to LLD)
While the UI relies on a ~10Hz sync rate to keep progress bars smooth, we must ensure the payload is lightweight. We are explicitly calling out the need to avoid sending static data (like behavior titles and icons) 10 times a second. The exact packet splitting strategy (e.g., static layout packet vs. dynamic sync packet) will be deferred to the Low-Level Design (LLD) phase.
