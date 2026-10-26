package com.cavmkii.seancraft.client.tarkov;

import com.cavmkii.seancraft.client.rpg.ClientRpg;
import com.cavmkii.seancraft.rpg.Attr;
import com.cavmkii.seancraft.rpg.PlayerData;
import com.cavmkii.seancraft.rpg.Skill;
import com.cavmkii.seancraft.rpg.Stats;
import com.cavmkii.seancraft.tetris.GearItem;
import com.cavmkii.seancraft.tetris.Grid;
import com.cavmkii.seancraft.tetris.GridId;
import com.cavmkii.seancraft.tetris.InvData;
import com.cavmkii.seancraft.tetris.ItemSize;
import com.cavmkii.seancraft.tetris.Loc;
import com.cavmkii.seancraft.tetris.Placed;
import com.cavmkii.seancraft.tetris.TetrisNet;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The Tarkov screen. GEAR: equipment, hotbar, the Tetris grids, and any open container or stash on the right.
 * CRAFTING: recipes you can make from everything you carry. SKILLS: character level, attributes and use-skills.
 * STATUS: body health and meds.
 *
 * <p>GEAR controls: click to pick up, click to put down, R to rotate, Shift+click to take half, Ctrl+click to
 * transfer, 1-9 to send the hovered item to a hotbar slot, right-click or Esc to let go.
 */
public class TarkovScreen extends Screen {
    public static final int GEAR = 0;
    public static final int CRAFTING = 1;
    public static final int SKILLS = 2;
    public static final int STATUS = 3;
    public static final int WEB = 4;
    private static final String[] TABS = {"GEAR", "CRAFTING", "SKILLS", "STATUS", "WEB"};

    private static final int C = 18;
    private static final int W = 426;
    private static final int H = 220;
    private static final int TOP = 24;
    private static final int COL4 = 264;
    private static final int COL4_W = 9 * C;
    private static final int ROW = 22;
    private static final int GRID_BTN_X = W - 86;

    private int tab;
    private float scale = 1;
    private float ox;
    private float oy;
    private boolean announced;

    private Loc heldFrom;
    private ItemStack held = ItemStack.EMPTY;
    private boolean heldRot;
    private int heldCount;

    private double containerScroll;
    private double craftScroll;
    private String search = "";
    private boolean searchFocused;
    private int ticks;
    private StatusScreen status;
    private com.cavmkii.seancraft.client.rpg.WebScreen web;

    public TarkovScreen(int tab) {
        super(Component.literal("Inventory"));
        this.tab = tab;
    }

    void setTab(int tab) {
        this.tab = tab;
        if (tab == CRAFTING) {
            ClientPlayNetworking.send(new TetrisNet.Craft("", 0));
        }
    }

    @Override
    protected void init() {
        scale = Math.min(1f, Math.min((width - 8) / (float) W, (height - TOP - 4) / (float) H));
        ox = (width - W * scale) / 2f;
        oy = TOP + Math.max(0, (height - TOP - H * scale) / 2f - 4);
        status = new StatusScreen();
        status.init(minecraft, width, height - TOP);
        if (web == null) {
            web = new com.cavmkii.seancraft.client.rpg.WebScreen();
        }
        web.init(minecraft, width, height - TOP);
        if (!announced) {
            announced = true;
            minecraft.getTutorial().onOpenInventory();
            ClientPlayNetworking.send(new TetrisNet.Action(4));
            if (tab == CRAFTING) {
                ClientPlayNetworking.send(new TetrisNet.Craft("", 0));
            }
        }
    }

    @Override
    public void removed() {
        ClientPlayNetworking.send(new TetrisNet.Action(5));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No vanilla blur; render() fills its own backdrop.
    }

    @Override
    public void tick() {
        ticks++;
        if (tab == CRAFTING && ticks % 40 == 0) {
            ClientPlayNetworking.send(new TetrisNet.Craft("", 0));
        }
        if (status != null) {
            status.tick();
        }
    }

    private double vx(double mouseX) {
        return (mouseX - ox) / scale;
    }

    private double vy(double mouseY) {
        return (mouseY - oy) / scale;
    }

    // ================================================================== layout

    private record Box(GridId id, int x, int y, int w, int h, List<Placed> items) {}

    private List<Box> boxes() {
        TetrisNet.Sync s = ClientTetris.sync;
        List<Box> out = new ArrayList<>();
        out.add(new Box(GridId.POCKETS, 66, 10, InvData.POCKET_W, InvData.POCKET_H, s.pockets()));
        if (s.rig().getItem() instanceof GearItem g) {
            out.add(new Box(GridId.RIG, 66, 60, g.spec.gridWidth, g.spec.gridHeight, s.rigItems()));
        }
        out.add(new Box(GridId.POUCH, 66, 128, s.pouchW(), s.pouchH(), s.pouch()));
        if (s.backpack().getItem() instanceof GearItem g) {
            out.add(new Box(GridId.BACKPACK, 164, 10, g.spec.gridWidth, g.spec.gridHeight, s.backpackItems()));
        }
        return out;
    }

    /** x, y, size of an equipment place. */
    private static int[] equipRect(int slot) {
        return switch (slot) {
            case 4 -> new int[] {25, 12, 36};
            case 5 -> new int[] {25, 62, 36};
            case 6 -> new int[] {0, 108, 22};
            default -> new int[] {0, 12 + slot * 24, 22};
        };
    }

    private static final String[] EQUIP_NAMES = {"Head", "Chest", "Legs", "Feet", "Rig", "Backpack", "Off-hand"};

    private static final int BTN_X = 164;
    private static final int BTN_W = 90;
    private static final int[] BTN_Y = {150, 166, 182};
    private static final String[] BTN = {"SORT", "STASH ALL", "DROP UNSORTED"};
    private static final int DISCARD_Y = 198;

    /** Rows on the right: unsorted first, then each container. y is before scrolling. */
    private record Section(int kind, int index, String title, int y, int height) {}

    private List<Section> sections() {
        TetrisNet.Sync s = ClientTetris.sync;
        List<Section> out = new ArrayList<>();
        int y = 0;
        if (!s.unsorted().isEmpty()) {
            int h = 10 + rows(s.unsorted().size()) * C;
            out.add(new Section(Loc.UNSORTED, 0, "UNSORTED (no room)", y, h));
            y += h + 6;
        }
        for (int i = 0; i < s.containers().size(); i++) {
            TetrisNet.ContainerView c = s.containers().get(i);
            int h = 10 + c.height() * C + (c.overflow().isEmpty() ? 0 : 10 + rows(c.overflow().size()) * C);
            out.add(new Section(Loc.CONTAINER, i, c.name().toUpperCase(Locale.ROOT), y, h));
            y += h + 6;
        }
        return out;
    }

    /** A grid on screen: top-left, size in cells, and its items. */
    private record GridRef(int x, int y, int w, int h, List<Placed> items) {}

    /** The grid a GRID or container-cell location belongs to. */
    private GridRef gridFor(Loc loc) {
        if (loc == null) {
            return null;
        }
        if (loc.kind() == Loc.GRID) {
            Box b = box(GridId.values()[loc.a()]);
            return b == null ? null : new GridRef(b.x, b.y, b.w, b.h, b.items);
        }
        if (loc.kind() == Loc.CONTAINER && loc.b() >= 0 && loc.a() < ClientTetris.sync.containers().size()) {
            TetrisNet.ContainerView c = ClientTetris.sync.containers().get(loc.a());
            for (Section sec : sections()) {
                if (sec.kind == Loc.CONTAINER && sec.index == loc.a()) {
                    return new GridRef(COL4, sec.y - (int) containerScroll + 10, c.width(), c.height(), c.items());
                }
            }
        }
        return null;
    }

    private static int rows(int n) {
        return Math.max(1, (n + 8) / 9);
    }

    private int sectionsHeight(List<Section> list) {
        if (list.isEmpty()) {
            return 0;
        }
        Section last = list.get(list.size() - 1);
        return last.y + last.height;
    }

    /** What's under a point (virtual coords) on the GEAR tab. For grids it's the cell. */
    private Loc locAt(double x, double y) {
        for (int slot = 0; slot < 7; slot++) {
            int[] r = equipRect(slot);
            if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[2]) {
                return Loc.equip(slot);
            }
        }
        if (y >= 190 && y < 190 + C && x >= 0 && x < 9 * C) {
            return Loc.hand((int) (x / C));
        }
        for (Box b : boxes()) {
            if (x >= b.x && x < b.x + b.w * C && y >= b.y && y < b.y + b.h * C) {
                return Loc.grid(b.id, (int) ((x - b.x) / C), (int) ((y - b.y) / C));
            }
        }
        if (x >= BTN_X && x < BTN_X + BTN_W && y >= DISCARD_Y && y < DISCARD_Y + 20) {
            return Loc.discard();
        }
        if (x >= COL4 && x < COL4 + COL4_W && y >= 0 && y < H) {
            for (Section sec : sections()) {
                int top = sec.y - (int) containerScroll + 10;
                int col = (int) ((x - COL4) / C);
                if (sec.kind == Loc.UNSORTED) {
                    int n = ClientTetris.sync.unsorted().size();
                    if (y >= top && y < top + rows(n) * C) {
                        int i = (int) ((y - top) / C) * 9 + col;
                        if (i >= 0 && i < n) {
                            return Loc.unsorted(i);
                        }
                    }
                    continue;
                }
                TetrisNet.ContainerView c = ClientTetris.sync.containers().get(sec.index);
                if (y >= top && y < top + c.height() * C && col < c.width()) {
                    return Loc.container(sec.index, col, (int) ((y - top) / C));
                }
                int otop = top + c.height() * C + 10;
                if (!c.overflow().isEmpty() && y >= otop && y < otop + rows(c.overflow().size()) * C) {
                    int i = (int) ((y - otop) / C) * 9 + col;
                    if (i >= 0 && i < c.overflow().size()) {
                        return Loc.containerOverflow(sec.index, c.overflowSlots().get(i));
                    }
                }
            }
        }
        return null;
    }

    private Box box(GridId id) {
        for (Box b : boxes()) {
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    private Placed placedAt(Loc loc) {
        GridRef r = gridFor(loc);
        return r == null ? null : new Grid(r.w, r.h, r.items).at(loc.b(), loc.c());
    }

    private ItemStack stackAt(Loc loc) {
        if (loc == null) {
            return ItemStack.EMPTY;
        }
        Inventory inv = minecraft.player.getInventory();
        TetrisNet.Sync s = ClientTetris.sync;
        return switch (loc.kind()) {
            case Loc.HAND -> inv.items.get(loc.a());
            case Loc.EQUIP -> switch (loc.a()) {
                case 4 -> s.rig();
                case 5 -> s.backpack();
                case 6 -> inv.offhand.get(0);
                default -> inv.armor.get(3 - loc.a());
            };
            case Loc.GRID -> {
                Placed p = placedAt(loc);
                yield p == null ? ItemStack.EMPTY : p.stack;
            }
            case Loc.CONTAINER -> {
                if (loc.b() >= 0) {
                    Placed p = placedAt(loc);
                    yield p == null ? ItemStack.EMPTY : p.stack;
                }
                if (loc.a() >= s.containers().size()) {
                    yield ItemStack.EMPTY;
                }
                TetrisNet.ContainerView c = s.containers().get(loc.a());
                int i = c.overflowSlots().indexOf(loc.c());
                yield i < 0 ? ItemStack.EMPTY : c.overflow().get(i);
            }
            case Loc.UNSORTED -> loc.a() < s.unsorted().size() ? s.unsorted().get(loc.a()) : ItemStack.EMPTY;
            default -> ItemStack.EMPTY;
        };
    }

    // ================================================================== render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xF4101014);
        if (tab == STATUS || tab == WEB) {
            g.pose().pushPose();
            g.pose().translate(0, TOP, 0);
            (tab == STATUS ? status : web).render(g, mouseX, mouseY - TOP, partialTick);
            g.pose().popPose();
        } else {
            g.pose().pushPose();
            g.pose().translate(ox, oy, 0);
            g.pose().scale(scale, scale, 1);
            int mx = (int) Math.floor(vx(mouseX));
            int my = (int) Math.floor(vy(mouseY));
            ItemStack tip = switch (tab) {
                case GEAR -> renderGear(g, mx, my);
                case CRAFTING -> renderCrafting(g, mx, my);
                default -> {
                    renderSkills(g);
                    yield ItemStack.EMPTY;
                }
            };
            g.pose().popPose();
            if (!tip.isEmpty() && held.isEmpty()) {
                g.renderTooltip(font, tip, mouseX, mouseY);
            }
        }
        renderTabs(g, mouseX, mouseY);
    }

    private int tabX(int t) {
        return width / 2 - TABS.length * 74 / 2 + t * 74;
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, width, TOP - 2, 0xFF141418);
        for (int t = 0; t < TABS.length; t++) {
            int x = tabX(t);
            boolean active = t == tab;
            boolean hover = mouseX >= x && mouseX < x + 70 && mouseY >= 4 && mouseY < 20;
            g.fill(x, 4, x + 70, 20, active ? 0xFF3A3A44 : hover ? 0xFF2A2A32 : 0xFF202026);
            if (active) {
                g.fill(x, 19, x + 70, 20, 0xFFE0C060);
            }
            g.drawString(font, TABS[t], x + (70 - font.width(TABS[t])) / 2, 8, active ? 0xFFFFFFFF : 0xFF888888, false);
        }
    }

    private void slot(GuiGraphics g, int x, int y, int size, ItemStack stack, boolean hover, String empty) {
        g.fill(x, y, x + size - 1, y + size - 1, hover ? 0xFF4A4A56 : 0xFF2A2A30);
        g.fill(x + 1, y + 1, x + size - 2, y + size - 2, 0xFF1A1A1F);
        if (!stack.isEmpty() && !(heldFrom != null && stack == stackAt(heldFrom) && heldCount == 0)) {
            if (size >= 32) {
                g.pose().pushPose();
                g.pose().translate(x + size / 2f, y + size / 2f, 0);
                g.pose().scale(2, 2, 1);
                g.renderItem(stack, -8, -8);
                g.pose().popPose();
            } else {
                int o = (size - 16) / 2;
                g.renderItem(stack, x + o, y + o);
                g.renderItemDecorations(font, stack, x + o, y + o);
            }
        } else if (stack.isEmpty() && empty != null) {
            g.pose().pushPose();
            g.pose().translate(x + size / 2f, y + size / 2f - 2, 0);
            g.pose().scale(0.5f, 0.5f, 1);
            g.drawString(font, empty, -font.width(empty) / 2, 0, 0xFF555560, false);
            g.pose().popPose();
        }
    }

    private void label(GuiGraphics g, String text, int x, int y) {
        g.drawString(font, text, x, y, 0xFFE0C060, false);
    }

    private ItemStack renderGear(GuiGraphics g, int mx, int my) {
        Loc hover = locAt(mx, my);
        ItemStack tip = stackAt(hover);
        TetrisNet.Sync s = ClientTetris.sync;

        label(g, "EQUIPMENT", 0, 0);
        for (int slot = 0; slot < 7; slot++) {
            int[] r = equipRect(slot);
            slot(g, r[0], r[1], r[2], stackAt(Loc.equip(slot)), hover != null && hover.equals(Loc.equip(slot)), EQUIP_NAMES[slot]);
            if (!held.isEmpty() && equipAccepts(slot, held)) {
                // Where the held item can be worn.
                int c = hover != null && hover.equals(Loc.equip(slot)) ? 0xFF7CFC7C : 0xFF3F8F3F;
                g.renderOutline(r[0] - 1, r[1] - 1, r[2] + 1, r[2] + 1, c);
            }
        }
        label(g, "HOTBAR", 0, 180);
        for (int i = 0; i < 9; i++) {
            slot(g, i * C, 190, C, stackAt(Loc.hand(i)), hover != null && hover.equals(Loc.hand(i)), null);
        }

        for (Box b : boxes()) {
            String title = switch (b.id) {
                case POCKETS -> "POCKETS";
                case RIG -> "RIG";
                case BACKPACK -> "BACKPACK";
                case POUCH -> "SECURE POUCH";
            };
            label(g, title, b.x, b.y - 10);
            renderGrid(g, b);
        }
        if (s.rig().isEmpty()) {
            g.drawString(font, "no rig", 66, 64, 0xFF555560, false);
        }
        if (s.backpack().isEmpty()) {
            g.drawString(font, "no backpack", 164, 14, 0xFF555560, false);
        }

        for (int i = 0; i < BTN.length; i++) {
            boolean h = mx >= BTN_X && mx < BTN_X + BTN_W && my >= BTN_Y[i] && my < BTN_Y[i] + 14;
            boolean enabled = i != 1 || !s.containers().isEmpty();
            enabled &= i != 2 || !s.unsorted().isEmpty();
            g.fill(BTN_X, BTN_Y[i], BTN_X + BTN_W, BTN_Y[i] + 14, !enabled ? 0xFF1E1E22 : h ? 0xFF4C4C58 : 0xFF33333C);
            g.drawString(font, BTN[i], BTN_X + (BTN_W - font.width(BTN[i])) / 2, BTN_Y[i] + 3, enabled ? 0xFFDDDDDD : 0xFF555555, false);
        }
        boolean overDiscard = hover != null && hover.kind() == Loc.DISCARD;
        g.fill(BTN_X, DISCARD_Y, BTN_X + BTN_W, DISCARD_Y + 20, overDiscard && !held.isEmpty() ? 0xFF803030 : 0xFF3A2020);
        g.drawString(font, "DISCARD", BTN_X + (BTN_W - font.width("DISCARD")) / 2, DISCARD_Y + 6, 0xFFCC8888, false);

        renderRight(g, hover);
        renderHeld(g, mx, my, hover);
        return tip;
    }

    private void renderGrid(GuiGraphics g, Box b) {
        renderGrid(g, new GridRef(b.x, b.y, b.w, b.h, b.items));
    }

    private void renderGrid(GuiGraphics g, GridRef b) {
        g.fill(b.x - 1, b.y - 1, b.x + b.w * C, b.y + b.h * C, 0xFF000000);
        for (int cy = 0; cy < b.h; cy++) {
            for (int cx = 0; cx < b.w; cx++) {
                int x = b.x + cx * C;
                int y = b.y + cy * C;
                g.fill(x, y, x + C - 1, y + C - 1, 0xFF22262C);
            }
        }
        Placed source = heldFrom != null && heldCount == 0 ? placedAt(heldFrom) : null;
        for (Placed p : b.items) {
            if (p == source) {
                continue;
            }
            drawPlaced(g, p.stack, b.x + p.x * C, b.y + p.y * C, p.width(), p.height(), p.rotated, 0xFF39404C);
        }
    }

    private void drawPlaced(GuiGraphics g, ItemStack stack, int px, int py, int w, int h, boolean rotated, int bg) {
        int pw = w * C;
        int ph = h * C;
        g.fill(px, py, px + pw - 1, py + ph - 1, bg);
        g.fill(px, py, px + pw - 1, py + 1, 0x30FFFFFF);
        int k = Math.max(1, Math.min(2, Math.min(w, h)));
        g.pose().pushPose();
        g.pose().translate(px + pw / 2f - 0.5f, py + ph / 2f - 0.5f, 0);
        if (rotated) {
            g.pose().mulPose(Axis.ZP.rotationDegrees(90));
        }
        g.pose().scale(k, k, 1);
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        if (stack.getCount() > 1) {
            String n = String.valueOf(stack.getCount());
            g.drawString(font, n, px + pw - 2 - font.width(n), py + ph - 9, 0xFFFFFFFF, true);
        }
        if (stack.isBarVisible()) {
            int bw = (pw - 4) * stack.getBarWidth() / 13;
            g.fill(px + 2, py + ph - 3, px + pw - 2, py + ph - 2, 0xFF000000);
            g.fill(px + 2, py + ph - 3, px + 2 + bw, py + ph - 2, 0xFF000000 | stack.getBarColor());
        }
        g.pose().popPose();
    }

    private void renderRight(GuiGraphics g, Loc hover) {
        List<Section> secs = sections();
        double max = Math.max(0, sectionsHeight(secs) - H);
        containerScroll = Math.max(0, Math.min(max, containerScroll));
        if (secs.isEmpty()) {
            g.drawWordWrap(font, Component.literal(ClientTetris.sync.stash()
                    ? "Your stash is empty: place chests or barrels inside the hideout."
                    : "Open a chest or barrel to see it here. Inside your hideout, the stash shows here."), COL4, 4, COL4_W, 0xFF666670);
            return;
        }
        int sx0 = (int) (ox + COL4 * scale);
        int sy0 = (int) oy;
        g.enableScissor(sx0, sy0, (int) (ox + (COL4 + COL4_W) * scale) + 1, (int) (oy + H * scale));
        for (Section sec : secs) {
            int y = sec.y - (int) containerScroll;
            g.drawString(font, sec.title, COL4, y, sec.kind == Loc.UNSORTED ? 0xFFFF7755 : 0xFFE0C060, false);
            if (sec.kind == Loc.UNSORTED) {
                List<ItemStack> list = ClientTetris.sync.unsorted();
                for (int i = 0; i < list.size(); i++) {
                    Loc loc = Loc.unsorted(i);
                    slot(g, COL4 + (i % 9) * C, y + 10 + (i / 9) * C, C, list.get(i), hover != null && hover.equals(loc), null);
                }
                continue;
            }
            TetrisNet.ContainerView c = ClientTetris.sync.containers().get(sec.index);
            renderGrid(g, new GridRef(COL4, y + 10, c.width(), c.height(), c.items()));
            if (!c.overflow().isEmpty()) {
                int oy0 = y + 10 + c.height() * C;
                g.drawString(font, "NO ROOM IN GRID", COL4, oy0 + 1, 0xFFFF7755, false);
                for (int i = 0; i < c.overflow().size(); i++) {
                    Loc loc = Loc.containerOverflow(sec.index, c.overflowSlots().get(i));
                    slot(g, COL4 + (i % 9) * C, oy0 + 10 + (i / 9) * C, C, c.overflow().get(i), hover != null && hover.equals(loc), null);
                }
            }
        }
        g.disableScissor();
        if (max > 0) {
            int barH = Math.max(10, (int) (H * H / (H + max)));
            int barY = (int) ((H - barH) * containerScroll / max);
            g.fill(COL4 + COL4_W + 2, barY, COL4 + COL4_W + 4, barY + barH, 0xFF555560);
        }
    }

    private boolean equipAccepts(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1, 2, 3 -> minecraft.player.getEquipmentSlotForItem(stack) == new net.minecraft.world.entity.EquipmentSlot[] {
                    net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}[slot];
            case 4 -> stack.getItem() instanceof GearItem g && g.spec.kind == com.cavmkii.seancraft.tetris.GearSpec.Kind.RIG;
            case 5 -> stack.getItem() instanceof GearItem g && g.spec.kind == com.cavmkii.seancraft.tetris.GearSpec.Kind.BACKPACK;
            default -> false;
        };
    }

    private void renderHeld(GuiGraphics g, int mx, int my, Loc hover) {
        if (held.isEmpty()) {
            return;
        }
        ItemStack shown = heldCount > 0 ? held.copyWithCount(heldCount) : held;
        if (gridFor(hover) == null) {
            // Off the grids, carry a plain icon so it doesn't hide the slot underneath.
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.renderItem(shown, mx - 8, my - 8);
            g.renderItemDecorations(font, shown, mx - 8, my - 8);
            g.pose().popPose();
            return;
        }
        int[] size = ItemSize.of(held);
        int w = heldRot ? size[1] : size[0];
        int h = heldRot ? size[0] : size[1];
        if (gridFor(hover) != null) {
            GridRef b = gridFor(hover);
            if (b != null) {
                Placed self = heldCount == 0 ? placedAt(heldFrom) : null;
                Placed there = new Grid(b.w, b.h, b.items).at(hover.b(), hover.c());
                boolean merge = there != null && there != self && ItemStack.isSameItemSameComponents(there.stack, held);
                boolean ok = merge || new Grid(b.w, b.h, b.items).fits(w, h, hover.b(), hover.c(), self);
                int gx = b.x + hover.b() * C;
                int gy = b.y + hover.c() * C;
                g.pose().pushPose();
                g.pose().translate(0, 0, 150);
                g.fill(gx, gy, gx + (merge ? there.width() : w) * C - 1, gy + (merge ? there.height() : h) * C - 1, ok ? 0x6040C040 : 0x60C04040);
                g.pose().popPose();
            }
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        drawPlaced(g, shown, mx - 9, my - 9, w, h, heldRot && size[0] != size[1], 0x8039404C);
        g.pose().popPose();
    }

    // ================================================================== crafting

    private List<TetrisNet.CraftEntry> filtered() {
        String q = search.toLowerCase(Locale.ROOT);
        List<TetrisNet.CraftEntry> out = new ArrayList<>();
        for (TetrisNet.CraftEntry e : ClientTetris.crafts) {
            if (q.isEmpty() || e.result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(e);
            }
        }
        return out;
    }

    private ItemStack renderCrafting(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, 140, 14, searchFocused ? 0xFF55555F : 0xFF33333C);
        g.fill(1, 1, 139, 13, 0xFF101014);
        String shown = search.isEmpty() && !searchFocused ? "Search..." : search + (searchFocused && ticks / 6 % 2 == 0 ? "_" : "");
        g.drawString(font, shown, 4, 3, search.isEmpty() && !searchFocused ? 0xFF666670 : 0xFFFFFFFF, false);
        g.pose().pushPose();
        g.pose().translate(146, 3, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, "3x3: crafting table within 6 blocks, or hideout.", 0, 0, 0xFF888890, false);
        g.drawString(font, "Uses hotbar and grids" + (ClientTetris.sync.stash() ? ", and the stash." : "."), 0, 9, 0xFF888890, false);
        g.pose().popPose();
        boolean gridHover = mx >= GRID_BTN_X && mx < W && my >= 0 && my < 14;
        g.fill(GRID_BTN_X, 0, W, 14, gridHover ? 0xFF4C4C58 : 0xFF33333C);
        g.drawString(font, "CRAFTING GRID", GRID_BTN_X + (W - GRID_BTN_X - font.width("CRAFTING GRID")) / 2, 3, 0xFFDDDDDD, false);

        List<TetrisNet.CraftEntry> list = filtered();
        int top = 20;
        double max = Math.max(0, list.size() * ROW - (H - top));
        craftScroll = Math.max(0, Math.min(max, craftScroll));
        if (list.isEmpty()) {
            g.drawString(font, ClientTetris.crafts.isEmpty() ? "Nothing you can make right now." : "No matches.", 0, top + 4, 0xFF666670, false);
            return ItemStack.EMPTY;
        }
        ItemStack tip = ItemStack.EMPTY;
        g.enableScissor((int) ox, (int) (oy + top * scale), (int) (ox + W * scale), (int) (oy + H * scale));
        for (int i = 0; i < list.size(); i++) {
            int y = top + i * ROW - (int) craftScroll;
            if (y + ROW < top || y > H) {
                continue;
            }
            TetrisNet.CraftEntry e = list.get(i);
            boolean hover = my >= y && my < y + ROW - 2 && my >= top;
            g.fill(0, y, W, y + ROW - 2, hover ? 0xFF26262E : 0xFF1C1C22);
            g.renderItem(e.result(), 3, y + 2);
            g.renderItemDecorations(font, e.result(), 3, y + 2);
            g.drawString(font, e.result().getHoverName().getString(), 24, y + 6, 0xFFFFFFFF, false);
            String can = "can make " + e.times();
            g.drawString(font, can, W - 100 - font.width(can), y + 6, 0xFF888890, false);
            for (int bi = 0; bi < 2; bi++) {
                int bx = W - 90 + bi * 46;
                boolean bh = mx >= bx && mx < bx + 42 && my >= y + 2 && my < y + ROW - 4 && my >= top;
                g.fill(bx, y + 2, bx + 42, y + ROW - 4, bh ? 0xFF4C8C4C : 0xFF3A6A3A);
                String t = bi == 0 ? "CRAFT" : "ALL";
                g.drawString(font, t, bx + (42 - font.width(t)) / 2, y + 6, 0xFFFFFFFF, false);
            }
            if (hover && mx >= 0 && mx < 200) {
                tip = e.result();
            }
        }
        g.disableScissor();
        return tip;
    }

    // ================================================================== skills

    private void renderSkills(GuiGraphics g) {
        PlayerData data = ClientRpg.data;
        Stats stats = ClientRpg.stats;
        String head = "Level " + data.level + (data.level < PlayerData.MAX_LEVEL ? "  (" + data.xp + " / " + PlayerData.xpToNext(data.level) + " xp)" : "")
                + "   Free points " + data.freePoints() + "   Primary " + stats.primary.label;
        g.drawString(font, head, 0, 0, 0xFFFFE080, false);
        int ax = 0;
        for (Attr attr : Attr.values()) {
            String t = attr.label + " " + String.format("%.1f", stats.attr(attr));
            g.drawString(font, t, ax, 12, 0xFF000000 | attr.color, false);
            ax += font.width(t) + 14;
        }
        Skill[] skills = Skill.values();
        for (int i = 0; i < skills.length; i++) {
            Skill skill = skills[i];
            int x = (i % 2) * 214;
            int y = 28 + (i / 2) * 32;
            int level = data.skillLevel(skill);
            double xp = data.skillXp.getOrDefault(skill, 0.0);
            double from = Skill.xpForLevel(level);
            double to = Skill.xpForLevel(Math.min(Skill.MAX_LEVEL, level + 1));
            g.fill(x, y, x + 206, y + 29, 0xFF1C1C22);
            g.fill(x, y, x + 2, y + 29, 0xFF000000 | skill.attr.color);
            g.drawString(font, skill.label, x + 6, y + 4, 0xFF000000 | skill.attr.color, false);
            String lv = String.valueOf(level);
            g.drawString(font, lv, x + 200 - font.width(lv), y + 4, 0xFFFFFFFF, false);
            g.fill(x + 6, y + 14, x + 200, y + 16, 0xFF000000);
            if (level < Skill.MAX_LEVEL && to > from) {
                g.fill(x + 6, y + 14, x + 6 + (int) (194 * Math.min(1, (xp - from) / (to - from))), y + 16, 0xFF000000 | skill.attr.color);
            }
            g.pose().pushPose();
            g.pose().translate(x + 6, y + 19, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, skill.describe(level), 0, 0, 0xFFAAAAAA, false);
            g.pose().popPose();
        }
    }

    // ================================================================== input

    private void dropHeld() {
        heldFrom = null;
        held = ItemStack.EMPTY;
        heldCount = 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int t = 0; t < TABS.length; t++) {
            int x = tabX(t);
            if (mouseX >= x && mouseX < x + 70 && mouseY >= 4 && mouseY < 20) {
                dropHeld();
                setTab(t);
                return true;
            }
        }
        if (tab == STATUS) {
            return status.mouseClicked(mouseX, mouseY - TOP, button);
        }
        if (tab == WEB) {
            return web.mouseClicked(mouseX, mouseY - TOP, button);
        }
        double x = vx(mouseX);
        double y = vy(mouseY);
        if (tab == CRAFTING) {
            return clickCrafting(x, y);
        }
        if (tab != GEAR) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 1) {
            dropHeld();
            return true;
        }
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < BTN.length; i++) {
            if (x >= BTN_X && x < BTN_X + BTN_W && y >= BTN_Y[i] && y < BTN_Y[i] + 14) {
                ClientPlayNetworking.send(new TetrisNet.Action(i));
                return true;
            }
        }
        Loc loc = locAt(x, y);
        if (!held.isEmpty()) {
            if (loc != null && !(loc.equals(heldFrom) && heldCount == 0 && !rotationChanged())) {
                ClientPlayNetworking.send(new TetrisNet.Move(heldFrom, loc, heldRot, heldCount));
            }
            dropHeld();
            return true;
        }
        ItemStack stack = stackAt(loc);
        if (loc == null || stack.isEmpty() || loc.kind() == Loc.DISCARD) {
            return true;
        }
        if (hasControlDown()) {
            ClientPlayNetworking.send(new TetrisNet.QuickMove(loc));
            return true;
        }
        Placed p = placedAt(loc);
        heldFrom = p == null ? loc : loc.kind() == Loc.GRID ? Loc.grid(GridId.values()[loc.a()], p.x, p.y) : Loc.container(loc.a(), p.x, p.y);
        held = stack;
        heldRot = p != null && p.rotated;
        heldCount = hasShiftDown() && stack.getCount() > 1 ? stack.getCount() / 2 : 0;
        return true;
    }

    private boolean rotationChanged() {
        Placed p = placedAt(heldFrom);
        return p != null && p.rotated != heldRot;
    }

    private boolean clickCrafting(double x, double y) {
        searchFocused = x >= 0 && x < 140 && y >= 0 && y < 14;
        if (x >= GRID_BTN_X && x < W && y >= 0 && y < 14) {
            ClientPlayNetworking.send(new TetrisNet.Action(6));
            return true;
        }
        List<TetrisNet.CraftEntry> list = filtered();
        int top = 20;
        if (y < top) {
            return true;
        }
        int i = (int) ((y - top + craftScroll) / ROW);
        if (i < 0 || i >= list.size()) {
            return true;
        }
        int rowY = top + i * ROW - (int) craftScroll;
        for (int bi = 0; bi < 2; bi++) {
            int bx = W - 90 + bi * 46;
            if (x >= bx && x < bx + 42 && y >= rowY + 2 && y < rowY + ROW - 4) {
                TetrisNet.CraftEntry e = list.get(i);
                ClientPlayNetworking.send(new TetrisNet.Craft(e.id(), bi == 0 ? 1 : e.times()));
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return tab == WEB ? web.mouseDragged(mouseX, mouseY - TOP, button, dragX, dragY) : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return tab == WEB ? web.mouseReleased(mouseX, mouseY - TOP, button) : super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == WEB) {
            return web.mouseScrolled(mouseX, mouseY - TOP, scrollX, scrollY);
        }
        if (tab == CRAFTING) {
            craftScroll -= scrollY * 20;
        } else if (tab == GEAR && vx(mouseX) >= COL4) {
            containerScroll -= scrollY * 20;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tab == CRAFTING && searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                craftScroll = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            return keyCode != GLFW.GLFW_KEY_TAB || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !held.isEmpty()) {
            dropHeld();
            return true;
        }
        if (tab == GEAR && keyCode == GLFW.GLFW_KEY_R && !held.isEmpty()) {
            int[] s = ItemSize.of(held);
            if (s[0] != s[1]) {
                heldRot = !heldRot;
            }
            return true;
        }
        if (tab == GEAR && held.isEmpty() && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            double mx = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
            double my = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
            Loc loc = locAt(vx(mx), vy(my));
            if (loc != null && !stackAt(loc).isEmpty()) {
                ClientPlayNetworking.send(new TetrisNet.Move(loc, Loc.hand(keyCode - GLFW.GLFW_KEY_1), false, 0));
            }
            return true;
        }
        if (minecraft.options.keyInventory.matches(keyCode, scanCode) || ClientTarkov.OPEN_SECURE.matches(keyCode, scanCode)
                || ClientTarkov.OPEN_STATUS.matches(keyCode, scanCode)
                || com.cavmkii.seancraft.client.rpg.ClientRpg.OPEN_WEB.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (tab == CRAFTING && searchFocused && c >= ' ' && search.length() < 40) {
            search += c;
            craftScroll = 0;
            return true;
        }
        return super.charTyped(c, modifiers);
    }
}
