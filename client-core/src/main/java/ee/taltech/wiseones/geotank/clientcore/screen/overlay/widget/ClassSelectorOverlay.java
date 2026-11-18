package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.TankClassPalette;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;

/**
 * Diep.io-style non-blocking class selector — a strip of cards above the
 * skill panel and to the right of the XP bar. Movement and shooting
 * continue while the strip is up; the overlay is dismissed by clicking a
 * card or pressing the matching number key.
 * <p>
 * Layout math is wrapped in {@link Layout} so draw and hit-test always agree.
 * Hover scaling lives in {@link CardBox}; same input feeds the visual scale
 * and the click target so they can never drift apart.
 */
public final class ClassSelectorOverlay implements HudOverlay {

    // Card visual / sizing constants
    private static final float CARD_MIN_WIDTH = 95f;
    private static final float CARD_MAX_WIDTH = 180f;
    private static final float CARD_ASPECT = 0.85f;
    private static final float CARD_GAP_RATIO = 0.08f;
    private static final float CARDS_BOTTOM_OFFSET = 46f;
    private static final float CARDS_LEFT_GAP = 20f;
    private static final float CARDS_RIGHT_MARGIN = 10f;

    private static final Color CARD_OUTLINE = new Color(1f, 1f, 1f, 0.55f);
    private static final Color KEY_BADGE = new Color(1f, 0.91f, 0.41f, 1f);

    private final HudResources res;

    public ClassSelectorOverlay(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean active(HudFrame frame) {
        TankClass[] classes = frame.viewModel().availableClasses();
        return frame.viewModel().classSelectionActive() && classes != null && classes.length > 0;
    }

    /**
     * Returns the class-card index under the given Gdx-coord (top-down Y) mouse
     * position, or -1 if no card is hit / overlay isn't active.
     */
    public int classUnderMouse(HudFrame frame, int mouseX, int mouseY) {
        if (!active(frame)) return -1;
        TankClass[] classes = frame.viewModel().availableClasses();
        Layout layout = new Layout(frame.screenW(), classes.length);
        float vx = mouseX / frame.scale();
        float sy = frame.screenH() - mouseY / frame.scale();
        return layout.hit(vx, sy);
    }

    @Override
    public void render(HudFrame frame) {
        TankClass[] classes = frame.viewModel().availableClasses();
        TankClass[] secondaries = frame.viewModel().availableSecondaryClasses();
        Layout layout = new Layout(frame.screenW(), classes.length);
        int hovered = hoveredIndex(frame, layout);
        float pulse = 0.5f + 0.5f * (float) Math.sin(
                (System.currentTimeMillis() % 1800L) / 1800f * Math.PI * 2.0);

        drawShapes(layout, classes, secondaries, hovered, pulse);
        drawText(layout, classes, secondaries, hovered, pulse);
    }

    /** Returns the secondary at index i, or null if no parallel array / single-class card. */
    private static TankClass secondaryAt(TankClass[] secondaries, int i) {
        return (secondaries != null && i < secondaries.length) ? secondaries[i] : null;
    }

    private int hoveredIndex(HudFrame frame, Layout layout) {
        float vx = Gdx.input.getX() / frame.scale();
        float sy = frame.screenH() - Gdx.input.getY() / frame.scale();
        return layout.hit(vx, sy);
    }

    private void drawShapes(Layout layout, TankClass[] classes, TankClass[] secondaries, int hovered, float pulse) {
        res.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < classes.length; i++) {
            boolean isHovered = i == hovered;
            CardBox box = CardBox.forIndex(layout, i, isHovered);
            float headerH = box.h * 0.24f;
            float badgeR = box.h * 0.075f;
            Color primaryColor = TankClassPalette.colorFor(classes[i]);
            TankClass secondaryClass = secondaryAt(secondaries, i);
            // Multiclass card: blend primary color on left, secondary color on right.
            Color rightColor = secondaryClass != null
                    ? TankClassPalette.colorFor(secondaryClass)
                    : primaryColor;

            // Drop shadow
            res.shapeRenderer.setColor(0f, 0f, 0f, 0.38f);
            res.shapeRenderer.rect(box.x + 3f, box.y - 4f, box.w, box.h);

            // Card body — vertical gradient
            Color bodyBot = isHovered
                    ? new Color(0.08f, 0.09f, 0.12f, 0.96f)
                    : new Color(0.05f, 0.06f, 0.08f, 0.92f);
            Color bodyTop = isHovered
                    ? new Color(0.20f, 0.22f, 0.27f, 0.98f)
                    : new Color(0.13f, 0.14f, 0.17f, 0.95f);
            res.shapeRenderer.rect(box.x, box.y, box.w, box.h, bodyBot, bodyBot, bodyTop, bodyTop);

            // Header strip — vertical dark→bright gradient on each side; left = primary, right = secondary.
            Color headerLeftDark = new Color(primaryColor.r * 0.55f, primaryColor.g * 0.55f, primaryColor.b * 0.55f, 1f);
            Color headerRightDark = new Color(rightColor.r * 0.55f, rightColor.g * 0.55f, rightColor.b * 0.55f, 1f);
            res.shapeRenderer.rect(box.x, box.y + box.h - headerH, box.w, headerH,
                    headerLeftDark, headerRightDark, rightColor, primaryColor);

            // Divider under header
            res.shapeRenderer.setColor(0f, 0f, 0f, 0.45f);
            res.shapeRenderer.rect(box.x, box.y + box.h - headerH - 1.5f, box.w, 1.5f);

            // Bottom accent stripe (split for multiclass)
            if (secondaryClass != null) {
                res.shapeRenderer.setColor(primaryColor);
                res.shapeRenderer.rect(box.x, box.y, box.w * 0.5f, 3f);
                res.shapeRenderer.setColor(rightColor);
                res.shapeRenderer.rect(box.x + box.w * 0.5f, box.y, box.w * 0.5f, 3f);
            } else {
                res.shapeRenderer.setColor(primaryColor);
                res.shapeRenderer.rect(box.x, box.y, box.w, 3f);
            }

            // Key badge circle (top-left, overlapping header)
            float badgeCX = box.x + badgeR + 6f;
            float badgeCY = box.y + box.h - headerH / 2f;
            res.shapeRenderer.setColor(0f, 0f, 0f, 0.55f);
            res.shapeRenderer.circle(badgeCX + 1f, badgeCY - 1f, badgeR + 1f);
            res.shapeRenderer.setColor(KEY_BADGE);
            res.shapeRenderer.circle(badgeCX, badgeCY, badgeR);
        }
        res.shapeRenderer.end();

        res.shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < classes.length; i++) {
            boolean isHovered = i == hovered;
            CardBox box = CardBox.forIndex(layout, i, isHovered);
            if (isHovered) {
                Color c = TankClassPalette.colorFor(classes[i]);
                float a = 0.75f + 0.25f * pulse;
                res.shapeRenderer.setColor(c.r, c.g, c.b, a);
                // Thicker double-outline ring on hover
                res.shapeRenderer.rect(box.x, box.y, box.w, box.h);
                res.shapeRenderer.rect(box.x - 1f, box.y - 1f, box.w + 2f, box.h + 2f);
            } else {
                res.shapeRenderer.setColor(CARD_OUTLINE);
                res.shapeRenderer.rect(box.x, box.y, box.w, box.h);
            }
        }
        res.shapeRenderer.end();
    }

    private void drawText(Layout layout, TankClass[] classes, TankClass[] secondaries, int hovered, float pulse) {
        res.spriteBatch.begin();

        // Pulsing title above the card strip — centered over the cards, not the
        // whole screen, so it stays clear of leaderboard/skill panel on narrow screens.
        float cardsTotalW = classes.length * layout.cardW + Math.max(0, classes.length - 1) * layout.gap;
        float cardsCenterX = layout.startX + cardsTotalW / 2f;

        String title = "CHOOSE YOUR CLASS";
        res.glyphLayout.setText(res.fontLarge, title);
        float titleAlpha = 0.85f + 0.15f * pulse;
        res.fontLarge.setColor(1f, 0.91f, 0.41f, titleAlpha);
        res.fontLarge.draw(res.spriteBatch, title,
                cardsCenterX - res.glyphLayout.width / 2f,
                layout.cardY + layout.cardH + 42f);

        String subtitle = "Click a card or press 1-" + classes.length;
        res.glyphLayout.setText(res.fontSmall, subtitle);
        res.fontSmall.setColor(0.85f, 0.85f, 0.85f, 0.9f);
        res.fontSmall.draw(res.spriteBatch, subtitle,
                cardsCenterX - res.glyphLayout.width / 2f,
                layout.cardY + layout.cardH + 22f);

        for (int i = 0; i < classes.length; i++) {
            TankClass tc = classes[i];
            boolean isHovered = i == hovered;
            CardBox box = CardBox.forIndex(layout, i, isHovered);
            float headerH = box.h * 0.24f;
            float badgeR = box.h * 0.075f;

            float badgeCX = box.x + badgeR + 6f;
            float badgeCY = box.y + box.h - headerH / 2f;

            // Key number inside badge
            String keyStr = String.valueOf(i + 1);
            res.glyphLayout.setText(res.fontMedium, keyStr);
            res.fontMedium.setColor(Color.BLACK);
            res.fontMedium.draw(res.spriteBatch, keyStr,
                    badgeCX - res.glyphLayout.width / 2f,
                    badgeCY + res.glyphLayout.height / 2f);

            // Class name centered in header (shifted right to clear the badge).
            // Multiclass card shows both names joined with "+"; small font keeps it on one line.
            TankClass secondaryClass = secondaryAt(secondaries, i);
            String name = (secondaryClass == null)
                    ? tc.getDisplayName().toUpperCase()
                    : tc.getDisplayName().toUpperCase() + " + " + secondaryClass.getDisplayName().toUpperCase();
            var nameFont = (secondaryClass == null) ? res.fontMedium : res.fontSmall;
            res.glyphLayout.setText(nameFont, name);
            nameFont.setColor(Color.WHITE);
            float nameLeftBound = badgeCX + badgeR + 4f;
            float nameRightBound = box.x + box.w - 6f;
            float nameCenterX = (nameLeftBound + nameRightBound) / 2f;
            nameFont.draw(res.spriteBatch, name,
                    nameCenterX - res.glyphLayout.width / 2f,
                    HudUtil.centeredTextY(box.y + box.h - headerH, headerH, res.glyphLayout.height));

            // Stat-summary text in the body
            String[] lines = classStatLines(tc, secondaryClass);
            float padding = box.w * 0.09f;
            float bulletR = 2.5f;
            float textX = box.x + padding + bulletR * 2f + 4f;
            float lineH = Math.max(12f, box.h * 0.095f);
            float bodyTopY = box.y + box.h - headerH - 8f;
            Color body = isHovered ? Color.WHITE : new Color(0.92f, 0.92f, 0.92f, 1f);
            res.fontSmall.setColor(body);
            for (int k = 0; k < lines.length; k++) {
                float lineY = bodyTopY - k * lineH;
                if (lineY < box.y + 10f) break;
                res.fontSmall.draw(res.spriteBatch, lines[k], textX, lineY);
            }
        }
        res.spriteBatch.end();

        // Bullets need a separate Filled shape pass — kept off the text method
        // so that method stays a pure SpriteBatch body without juggling state.
        drawStatBullets(layout, classes, secondaries, hovered);
    }

    private void drawStatBullets(Layout layout, TankClass[] classes, TankClass[] secondaries, int hovered) {
        res.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < classes.length; i++) {
            boolean isHovered = i == hovered;
            CardBox box = CardBox.forIndex(layout, i, isHovered);
            float headerH = box.h * 0.24f;
            float padding = box.w * 0.09f;
            float bulletR = 2.5f;
            float bulletX = box.x + padding + bulletR;
            TankClass secondaryClass = secondaryAt(secondaries, i);
            String[] lines = classStatLines(classes[i], secondaryClass);
            float lineH = Math.max(12f, box.h * 0.095f);
            float bodyTopY = box.y + box.h - headerH - 8f;
            res.shapeRenderer.setColor(TankClassPalette.colorFor(classes[i]));
            for (int k = 0; k < lines.length; k++) {
                float lineY = bodyTopY - k * lineH;
                if (lineY < box.y + 10f) break;
                res.shapeRenderer.circle(bulletX, lineY - 4f, bulletR);
            }
        }
        res.shapeRenderer.end();
    }

    /**
     * Per-class stat summary, one trait per line. Cards are too narrow to
     * fit a comma-joined list at the body font size, and the renderer
     * doesn't wrap — so each modifier needs its own row. Multiclass cards
     * show the union of both classes' lines.
     */
    private static String[] classStatLines(TankClass tc, TankClass secondary) {
        String[] primaryLines = singleClassLines(tc);
        if (secondary == null) return primaryLines;
        String[] secondaryLines = singleClassLines(secondary);
        String[] merged = new String[primaryLines.length + secondaryLines.length];
        System.arraycopy(primaryLines, 0, merged, 0, primaryLines.length);
        System.arraycopy(secondaryLines, 0, merged, primaryLines.length, secondaryLines.length);
        return merged;
    }

    private static String[] singleClassLines(TankClass tc) {
        return switch (tc) {
            case ARTILLERY -> new String[]{"+Cannons", "+Spread"};
            case SPEEDSTER -> new String[]{"+Move Speed", "+Bullet Speed"};
            case MACHINE_GUN -> new String[]{"+Fire Rate", "-Bullet Lifetime"};
            case TANK -> new String[]{"+Max HP", "+HP Regen", "-Move Speed"};
            case STREAM -> new String[]{"+Bullets", "-Bullet Speed", "-Spread"};
            case RAMMER -> new String[]{"+Body Damage", "+Max HP"};
            case SNIPER -> new String[]{"+Bullet Damage", "+Pierce", "-Fire Rate", "+Bullet Lifetime"};
            default -> new String[]{"-"};
        };
    }

    /**
     * Card-strip layout — width, gap, and start position are all derived from
     * the available area so any number of cards (1..7) lays out cleanly.
     */
    private static final class Layout {
        final float cardW;
        final float cardH;
        final float gap;
        final float startX;
        final float cardY;
        final int count;

        Layout(float screenW, int count) {
            this.count = count;
            float leftBound = SkillPanelWidget.LEFT_MARGIN + SkillPanelWidget.BAR_WIDTH + CARDS_LEFT_GAP;
            float rightBound = screenW - CARDS_RIGHT_MARGIN;
            float availW = Math.max(1f, rightBound - leftBound);

            float rawW = availW / (count + Math.max(0, count - 1) * CARD_GAP_RATIO);
            this.cardW = Math.max(CARD_MIN_WIDTH, Math.min(CARD_MAX_WIDTH, rawW));
            this.cardH = cardW * CARD_ASPECT;
            this.gap = cardW * CARD_GAP_RATIO;
            float totalW = count * cardW + Math.max(0, count - 1) * gap;
            this.startX = leftBound + (availW - totalW) / 2f;
            this.cardY = XpBarWidget.BOTTOM_MARGIN + XpBarWidget.HEIGHT + CARDS_BOTTOM_OFFSET;
        }

        float cardX(int index) {
            return startX + index * (cardW + gap);
        }

        int hit(float sx, float sy) {
            if (sy < cardY || sy > cardY + cardH) return -1;
            for (int i = 0; i < count; i++) {
                float cx = cardX(i);
                if (sx >= cx && sx <= cx + cardW) return i;
            }
            return -1;
        }
    }

    /**
     * Per-card geometry. Hover slightly grows the box; same scaling applies to
     * the visual draw and the click target so they stay aligned.
     */
    private static final class CardBox {
        final float x, y, w, h;

        CardBox(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        static CardBox forIndex(Layout layout, int i, boolean hovered) {
            float x = layout.cardX(i);
            float y = layout.cardY;
            float w = layout.cardW;
            float h = layout.cardH;
            if (hovered) {
                float dx = w * 0.05f;
                float dy = h * 0.05f;
                x -= dx / 2f;
                y -= dy / 2f;
                w += dx;
                h += dy;
            }
            return new CardBox(x, y, w, h);
        }
    }
}
