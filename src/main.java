import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Seam's Shop (Deltarune Ch.1) - single-screen store.
 * Painted with raw Swing: shelf backdrop + animated Seam, clean textbox
 * container, real bitmap font + heart sprite, typewriter dialogue.
 * Run with: java src/main.java
 */
public class main {
    private enum Mode { MAIN, BUY, BUY_CONFIRM, BUY_MSG, SELL, SELL_CONFIRM, SELL_MSG, TALK, TALK_VIEW, EXIT }
    private enum Mood { IDLE, TALK, HAPPY, SAD, DISGUSTED }

    private static final int W = 1025, H = 768, BOTTOM_H = 384, TOP_H = H - BOTTOM_H;
    private static final double FS = 3;
    private static final int MENU_X = 790, MONEY_X = 700, MONEY_Y = TOP_H + BOTTOM_H - 104;
    private static final int[] MENU_ROWS = {TOP_H + 43, TOP_H + 105, TOP_H + 167, TOP_H + 229};
    private static final String[] MENU = {"Buy", "Sell", "Talk", "Exit"};
    private static final int LEFT_X = 80, LIST_TOP = TOP_H + 43, LIST_GAP = 52, PRICE_X = 430;
    private static final int DIALOG_X = 60;
    private static final int YES_Y = TOP_H + 167, NO_Y = YES_Y + LIST_GAP;
    private static final String HEART_PATH =
        "spr_heart/layers/2dc52cbf-f704-4979-b101-79c1515a8c00/0cd8f0a1-4d2c-4fa8-8289-4929c35f0e54.png";

    private Image topBg, bottomBg, heartSprite, ohFace, infoBoxImg;
    private Image[] idle = new Image[4], talkF = new Image[3], laugh = new Image[2], annoyed = new Image[2];
    private BitmapFont font;
    private JPanel panel;

    private final Money money = new Money(209);
    private final AudioPlay audio = new AudioPlay();
    private final Buy stock = new Buy();
    private final Sell seller = new Sell(stock);
    private final Talk talk = new Talk();

    private Mode mode = Mode.MAIN;
    private Mood mood = Mood.IDLE;
    private int mainIndex = 0, itemIndex = 0, talkIndex = 0, confirmIndex = 0, talkPage = 0;
    private int tick = 0;

    private String fullText = "";
    private int shown = 0;
    private Timer typer;
    private double infoP = 0, seamP = 0;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new main().start());
    }

    private void start() {
        seller.giveStartingInventory();
        audio.startMusic();
        topBg = load("background.png");
        bottomBg = load("shoptextcontainer.png");
        infoBoxImg = load("information-container.png");
        heartSprite = load(HEART_PATH);
        String[] idleN = {"idle_1.png", "idle_2.png", "idle_3.png", "idle_4.png"};
        String[] talkN = {"talk_1.png", "talk_2.png", "talk_3.png"};
        String[] laughN = {"laugh_1.png", "laugh_2.png"};
        String[] annoyN = {"impatient_1.png", "impatient_2.png"};
        for (int i = 0; i < 4; i++) idle[i] = load(idleN[i]);
        for (int i = 0; i < 3; i++) talkF[i] = load(talkN[i]);
        for (int i = 0; i < 2; i++) laugh[i] = load(laughN[i]);
        for (int i = 0; i < 2; i++) annoyed[i] = load(annoyN[i]);
        ohFace = load("oh_1.png");
        font = new BitmapFont("src/assets/font/fnt_main/fnt_main.png",
            "src/assets/font/fnt_main/fnt_main.yy");

        panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw((Graphics2D) g);
            }
        };
        panel.setBackground(Color.BLACK);
        panel.setFocusable(true);
        panel.setPreferredSize(new Dimension(W, H));

        bind("UP", () -> move(-1));
        bind("DOWN", () -> move(1));
        bindKey(KeyEvent.VK_W, () -> move(-1));
        bindKey(KeyEvent.VK_S, () -> move(1));
        bindKey(KeyEvent.VK_Z, this::confirm);
        bind("ENTER", this::confirm);
        bindKey(KeyEvent.VK_X, this::back);
        bind("ESCAPE", this::back);

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover(e.getX(), e.getY()); }
        });
        panel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                panel.requestFocusInWindow();
                if (e.getButton() == MouseEvent.BUTTON3) back();
                else click(e.getX(), e.getY());
            }
        });

        new Timer(150, e -> { tick++; panel.repaint(); }).start();
        new Timer(30, e -> {
            double wantInfo = (mode == Mode.BUY || mode == Mode.BUY_CONFIRM) ? 1 : 0;
            double wantLeft = (mode == Mode.BUY || mode == Mode.BUY_CONFIRM || mode == Mode.BUY_MSG) ? 1 : 0;
            double n1 = step(infoP, wantInfo), n2 = step(seamP, wantLeft);
            if (n1 != infoP || n2 != seamP) {
                infoP = n1;
                seamP = n2;
                panel.repaint();
            }
        }).start();

        JFrame frame = new JFrame("Seam's Shop - Deltarune");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.getContentPane().setBackground(Color.BLACK);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();
        say("* Hee hee...\n* Welcome, travellers.");
    }

    private void bind(String key, Runnable action) {
        panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        panel.getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    /** Case-independent key binding (typed-letter bindings only fire on uppercase). */
    private void bindKey(int code, Runnable action) {
        String name = "key" + code;
        panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(code, 0), name);
        panel.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private Image load(String name) {
        for (String p : new String[]{"src/assets/sprites/" + name, "assets/sprites/" + name}) {
            try {
                File f = new File(p);
                if (f.exists()) return ImageIO.read(f);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // ---------------- dialogue ----------------

    private void say(String text) {
        sayAs(Mood.TALK, text);
    }

    private void sayAs(Mood m, String text) {
        fullText = text;
        shown = 0;
        mood = m;
        if (typer != null) typer.stop();
        typer = new Timer(22, e -> {
            shown++;
            if (shown >= fullText.length()) {
                typer.stop();
                if (mood == Mood.TALK) mood = Mood.IDLE;
            }
            panel.repaint();
        });
        typer.start();
    }

    private void stopTyping() {
        if (typer != null) typer.stop();
        if (shown < fullText.length()) {
            shown = fullText.length();
            if (mood == Mood.TALK) mood = Mood.IDLE;
            panel.repaint();
        }
    }

    private boolean typing() { return shown < fullText.length(); }

    // ---------------- input ----------------

    private void move(int d) {
        if (typing()) { stopTyping(); return; }
        switch (mode) {
            case MAIN: mainIndex = (mainIndex + d + 4) % 4; break;
            case BUY:
                for (int k = 0; k < 5; k++) {
                    itemIndex = (itemIndex + d + 5) % 5;
                    if (itemIndex == 4 || !stock.getItems().get(itemIndex).soldOut) break;
                }
                break;
            case SELL:
                itemIndex = (itemIndex + d + sellRows()) % sellRows();
                break;
            case TALK: talkIndex = (talkIndex + d + talk.size() + 1) % (talk.size() + 1); break;
            case BUY_CONFIRM:
            case SELL_CONFIRM: confirmIndex = 1 - confirmIndex; break;
            default: return;
        }
        audio.playMove();
        panel.repaint();
    }

    private void confirm() {
        if (typing()) { stopTyping(); return; }
        switch (mode) {
            case MAIN: pickMain(mainIndex); break;
            case BUY: openBuyConfirm(); break;
            case BUY_CONFIRM: doBuy(confirmIndex == 0); break;
            case BUY_MSG: showBuy(); break;
            case SELL: openSellConfirm(); break;
            case SELL_CONFIRM: doSell(confirmIndex == 0); break;
            case SELL_MSG: showSell(); break;
            case TALK: viewTopic(); break;
            case TALK_VIEW: advanceTopic(); break;
            case EXIT: showMain(); break;
        }
    }

    private void back() {
        if (typing()) { stopTyping(); return; }
        switch (mode) {
            case BUY:
            case SELL:
            case TALK:
            case EXIT: showMain(); break;
            case BUY_CONFIRM: showBuy(); break;
            case BUY_MSG: showBuy(); break;
            case SELL_CONFIRM: showSell(); break;
            case SELL_MSG: showSell(); break;
            case TALK_VIEW: showTalk(); break;
            default: break;
        }
    }

    private double step(double v, double t) {
        double s = 0.12;
        return v < t ? Math.min(t, v + s) : Math.max(t, v - s);
    }

    private int rowAt(int y, int top, int gap, int count) {        for (int i = 0; i < count; i++) {
            if (y >= top + i * gap - 10 && y <= top + i * gap + 48) return i;
        }
        return -1;
    }

    private void hover(int x, int y) {
        if (typing()) return;
        switch (mode) {
            case MAIN:
                if (x > 700) {
                    int r = rowAt(y, MENU_ROWS[0], 62, 4);
                    if (r >= 0 && r != mainIndex) { mainIndex = r; audio.playMove(); panel.repaint(); }
                }
                break;
            case BUY:
                if (x < 660) {
                    int r = rowAt(y, LIST_TOP, LIST_GAP, 5);
                    if (r >= 0 && r != itemIndex
                        && (r == 4 || !stock.getItems().get(r).soldOut)) {
                        itemIndex = r;
                        audio.playMove();
                        panel.repaint();
                    }
                }
                break;
            case SELL:
                if (x < 660) {
                    int r = rowAt(y, LIST_TOP, LIST_GAP, sellRows());
                    if (r >= 0 && r != itemIndex) { itemIndex = r; audio.playMove(); panel.repaint(); }
                }
                break;
            case TALK:
                if (x < 660) {
                    int r = rowAt(y, LIST_TOP, LIST_GAP, talk.size() + 1);
                    if (r >= 0 && r != talkIndex) { talkIndex = r; audio.playMove(); panel.repaint(); }
                }
                break;
            case BUY_CONFIRM:
            case SELL_CONFIRM: {
                int r = rowAt(y, YES_Y, LIST_GAP, 2);
                if (r >= 0 && r != confirmIndex) { confirmIndex = r; audio.playMove(); panel.repaint(); }
                break;
            }
            default: break;
        }
    }

    private void click(int x, int y) {
        if (typing()) { stopTyping(); return; }
        switch (mode) {
            case MAIN:
                if (x > 700) {
                    int r = rowAt(y, MENU_ROWS[0], 62, 4);
                    if (r >= 0) { mainIndex = r; pickMain(r); }
                }
                break;
            case BUY:
                if (x < 660) {
                    int r = rowAt(y, LIST_TOP, LIST_GAP, 5);
                    if (r >= 0 && (r == 4 || !stock.getItems().get(r).soldOut)) {
                        if (r == itemIndex) openBuyConfirm();
                        else { itemIndex = r; panel.repaint(); }
                    }
                }
                break;
            case BUY_CONFIRM: {
                int r = rowAt(y, YES_Y, LIST_GAP, 2);
                if (r >= 0) doBuy(r == 0);
                break;
            }
            case BUY_MSG: showBuy(); break;
            case SELL:
                if (x < 660) {
                    int r = rowAt(y, LIST_TOP, LIST_GAP, sellRows());
                    if (r >= 0) {
                        if (r == itemIndex) openSellConfirm();
                        else { itemIndex = r; panel.repaint(); }
                    }
                }
                break;
            case SELL_CONFIRM: {
                int r = rowAt(y, YES_Y, LIST_GAP, 2);
                if (r >= 0) doSell(r == 0);
                break;
            }
            case SELL_MSG: showSell(); break;
            case TALK:
                if (x < 660) {
                    int r = rowAt(y, LIST_TOP, LIST_GAP, talk.size() + 1);
                    if (r >= 0) {
                        if (r == talkIndex) viewTopic();
                        else { talkIndex = r; panel.repaint(); }
                    }
                }
                break;
            case TALK_VIEW: advanceTopic(); break;
            case EXIT: showMain(); break;
        }
    }

    // ---------------- screens ----------------

    private void showMain() {
        mode = Mode.MAIN;
        mood = Mood.IDLE;
        say("* Hee hee...\n* Welcome, travellers.");
    }

    private void pickMain(int i) {
        audio.playSelect();
        itemIndex = 0;
        talkIndex = 0;
        if (i == 0) { mode = Mode.BUY; mood = Mood.IDLE; }
        else if (i == 1) { mode = Mode.SELL; mood = Mood.IDLE; }
        else if (i == 2) { mode = Mode.TALK; mood = Mood.IDLE; }
        else {
            mode = Mode.EXIT;
            mood = Mood.HAPPY;
            say("* Nope, You can't leave, fellow lightner.");
        }
        panel.repaint();
    }

    private void showBuy() { mode = Mode.BUY; mood = Mood.IDLE; panel.repaint(); }

    private void openBuyConfirm() {
        if (itemIndex == 4) { showMain(); return; }
        Buy.Item it = stock.getItems().get(itemIndex);
        if (it.soldOut) {
            mode = Mode.BUY_MSG;
            say("* Hee hee... That one is already yours.\n* SOLD OUT - already purchased.");
            panel.repaint();
            return;
        }
        mode = Mode.BUY_CONFIRM;
        confirmIndex = 0;
        audio.playSelect();
        panel.repaint();
    }

    private void doBuy(boolean yes) {
        if (!yes) { showBuy(); return; }
        Buy.Item it = stock.getItems().get(itemIndex);
        mode = Mode.BUY_MSG;
        switch (stock.buy(it, money)) {
            case SUCCESS:
                mood = Mood.HAPPY;
                say("* Hee hee... Thank you!\n* Got " + it.name + "! Balance: $" + money.get());
                break;
            case SOLD_OUT:
                say("* Hee hee... That one is already yours.\n* SOLD OUT - already purchased.");
                break;
            default:
                mood = Mood.SAD;
                say("* Hee hee... You don't have enough D$.\n* " + it.name + " costs "
                    + it.price + "D$. You have $" + money.get() + ".");
        }
    }

    private int sellCount() {
        int n = 0;
        for (Buy.Item it : stock.getItems()) {
            if (!it.soldOut && it.owned > 0) n++;
        }
        return n;
    }

    private Buy.Item sellItemAt(int row) {
        int r = 0;
        for (Buy.Item it : stock.getItems()) {
            if (!it.soldOut && it.owned > 0 && r++ == row) return it;
        }
        return null;
    }

    private int sellRows() { return sellCount() + 1; }

    private void showSell() {
        mode = Mode.SELL;
        mood = Mood.IDLE;
        if (itemIndex > sellCount()) itemIndex = sellCount();
        panel.repaint();
    }

    private void openSellConfirm() {
        if (itemIndex == sellCount() || !seller.hasAnythingToSell()) { showMain(); return; }
        if (sellItemAt(itemIndex) == null) return;
        mode = Mode.SELL_CONFIRM;
        confirmIndex = 0;
        audio.playSelect();
        panel.repaint();
    }

    private void doSell(boolean yes) {
        if (!yes) { showSell(); return; }
        Buy.Item it = sellItemAt(itemIndex);
        mode = Mode.SELL_MSG;
        if (it != null && seller.sell(it, money)) {
            if (it.name.equals("Reynold")) {
                sayAs(Mood.DISGUSTED, "* Eugh...! Why would you sell me that?!\n* Take your "
                    + seller.sellPrice(it) + "D$ and get it away from me.");
            } else {
                mood = Mood.HAPPY;
                say("* Hee hee... Sold! +" + seller.sellPrice(it) + "D$.\n* Balance: $" + money.get() + ".");
            }
        } else {
            showSell();
        }
    }

    private void showTalk() { mode = Mode.TALK; mood = Mood.IDLE; panel.repaint(); }

    private void viewTopic() {
        if (talkIndex == talk.size()) { showMain(); return; }
        talkPage = 0;
        mode = Mode.TALK_VIEW;
        audio.playSelect();
        say(talk.lineFor(talkIndex, talkPage));
    }

    private void advanceTopic() {
        if (talkPage + 1 < talk.pages(talkIndex)) {
            talkPage++;
            say(talk.lineFor(talkIndex, talkPage));
        } else {
            showTalk();
        }
    }

    // ---------------- painting ----------------

    private Image seamNow() {
        switch (mood) {
            case TALK: return talkF[tick % 3] != null ? talkF[tick % 3] : idle[0];
            case HAPPY: return laugh[tick % 2] != null ? laugh[tick % 2] : idle[0];
            case SAD: return ohFace != null ? ohFace : idle[0];
            case DISGUSTED: return annoyed[tick % 2] != null ? annoyed[tick % 2] : idle[0];
            default: return idle[(tick / 3) % 4] != null ? idle[(tick / 3) % 4] : idle[0];
        }
    }

    private void draw(Graphics2D g) {
        g.setComposite(AlphaComposite.Src);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        g.setComposite(AlphaComposite.SrcOver);
        if (topBg != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(topBg, 0, 0, W, TOP_H, null);
        }
        Image s = seamNow();
        if (s != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int size = 380;
            int bgBottom = TOP_H;
            g.setClip(0, 0, W, TOP_H);
            g.drawImage(s, (W - size) / 2 - (int) (190 * seamP * seamP), bgBottom - size, size, size, null);
            g.setClip(null);
        }
        drawInfoBox(g);
        if (bottomBg != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(bottomBg, 0, TOP_H, W, BOTTOM_H, null);
        }
        if (font == null || !font.isLoaded()) return;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        if (mode == Mode.MAIN) {
            for (int i = 0; i < 4; i++) {
                font.drawString(g, MENU[i], MENU_X, MENU_ROWS[i], FS);
            }
            drawHeart(g, 748, MENU_ROWS[mainIndex]);
        } else if (mode == Mode.EXIT) {
            font.drawString(g, "Stay", MENU_X, MENU_ROWS[0], FS);
            drawHeart(g, 748, MENU_ROWS[0]);
        } else if (mode == Mode.BUY || mode == Mode.BUY_CONFIRM || mode == Mode.BUY_MSG) {
            drawRightText(g, "* What would you like to buy?");
        } else if (mode == Mode.SELL || mode == Mode.SELL_CONFIRM || mode == Mode.SELL_MSG) {
            drawRightText(g, "* What would you like to sell?");
        } else {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            for (int i = 0; i < 4; i++) {
                font.drawString(g, MENU[i], MENU_X, MENU_ROWS[i], FS);
            }
            g.setComposite(old);
        }
        font.drawString(g, "$" + money.get(), MONEY_X, MONEY_Y, FS);

        switch (mode) {
            case MAIN:
            case TALK_VIEW:
            case EXIT:
            case BUY_MSG:
            case SELL_MSG:
                drawDialogue(g);
                break;
            case BUY:
                drawBuyList(g);
                break;
            case BUY_CONFIRM: {
                Buy.Item it = stock.getItems().get(itemIndex);
                drawConfirm(g, "Buy " + it.name + " for " + it.price + "D$?");
                break;
            }
            case SELL:
                drawSellList(g);
                break;
            case SELL_CONFIRM: {
                Buy.Item it = sellItemAt(itemIndex);
                if (it != null) drawConfirm(g, "Sell " + it.name + " for " + seller.sellPrice(it) + "D$?");
                break;
            }
            case TALK:
                drawTalkList(g);
                break;
        }
    }

    private void drawRightText(Graphics2D g, String text) {
        int y = LIST_TOP;
        for (String line : wrap(text, 280)) {
            font.drawString(g, line, 700, y, FS);
            y += 44;
        }
    }

    /** Floating black info box above the right box. Only while buying. */
    private void drawInfoBox(Graphics2D g) {
        if (mode != Mode.BUY && mode != Mode.BUY_CONFIRM) return;
        if (infoP < 0.02) return;
        double e = infoP * infoP;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        List<String> lines = new ArrayList<String>();
        if (itemIndex >= 4) {
            for (String l : wrap("Nothing you're looking for?", 296)) lines.add(l);
        } else {
            Buy.Item it = stock.getItems().get(itemIndex);
            lines.add(it.name);
            if (it.soldOut) {
                lines.add("SOLD OUT.");
            } else {
                lines.add(it.price + "D$.");
                for (String l : wrap("* " + it.description, 296)) lines.add(l);
            }
        }
        int x0 = 630, w = 392, h = 365;
        int y0 = TOP_H - h + 25 + (int) ((1 - e) * 70);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, e * 1.5)));
        if (infoBoxImg != null) {
            g.drawImage(infoBoxImg, x0, y0, w, h, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(x0, y0, w, 3);
            g.fillRect(x0, y0 + h - 3, w, 3);
            g.fillRect(x0, y0, 3, h);
            g.fillRect(x0 + w - 3, y0, 3, h);
        }
        int y = y0 + 58;
        for (String l : lines) {
            font.drawString(g, l, 700, y, FS);
            y += 44;
        }
        g.setComposite(old);
    }

    private void drawDialogue(Graphics2D g) {
        String part = fullText.substring(0, Math.min(shown, fullText.length()));
        int y = LIST_TOP;
        for (String para : part.split("\n", -1)) {
            for (String line : wrap(para, 560)) {
                font.drawString(g, line, DIALOG_X, y, FS);
                y += 48;
            }
        }
    }

    private void drawBuyList(Graphics2D g) {
        List<Buy.Item> items = stock.getItems();
        for (int i = 0; i < items.size(); i++) {
            Buy.Item it = items.get(i);
            Composite old = null;
            if (it.soldOut) {
                old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            }
            font.drawString(g, it.name, LEFT_X, LIST_TOP + i * LIST_GAP, FS);
            font.drawString(g, it.soldOut ? "SOLD OUT" : it.price + "D$" + (it.owned > 0 ? " x" + it.owned : ""),
                PRICE_X, LIST_TOP + i * LIST_GAP, FS);
            if (old != null) g.setComposite(old);
            else if (i == itemIndex) drawHeart(g, 40, LIST_TOP + i * LIST_GAP);
        }
        font.drawString(g, "Exit", LEFT_X, LIST_TOP + 4 * LIST_GAP, FS);
        if (itemIndex == 4) drawHeart(g, 40, LIST_TOP + 4 * LIST_GAP);
    }

    private void drawSellList(Graphics2D g) {
        if (sellCount() == 0) {
            font.drawString(g, "Exit", LEFT_X, LIST_TOP, FS);
            if (itemIndex == 0) drawHeart(g, 40, LIST_TOP);
            font.drawString(g, "* (Nothing to sell.)", LEFT_X, LIST_TOP + LIST_GAP, FS);
            return;
        }
        int r = 0;
        for (Buy.Item it : stock.getItems()) {
            if (it.soldOut || it.owned <= 0) continue;
            font.drawString(g, it.name + " x" + it.owned, LEFT_X, LIST_TOP + r * LIST_GAP, FS);
            font.drawString(g, seller.sellPrice(it) + "D$", PRICE_X, LIST_TOP + r * LIST_GAP, FS);
            if (r == itemIndex) drawHeart(g, 40, LIST_TOP + r * LIST_GAP);
            r++;
        }
        font.drawString(g, "Exit", LEFT_X, LIST_TOP + r * LIST_GAP, FS);
        if (r == itemIndex) drawHeart(g, 40, LIST_TOP + r * LIST_GAP);
    }

    private void drawTalkList(Graphics2D g) {
        String[] topics = talk.getTopics();
        for (int i = 0; i < topics.length; i++) {
            font.drawString(g, topics[i], LEFT_X, LIST_TOP + i * LIST_GAP, FS);
            if (i == talkIndex) drawHeart(g, 40, LIST_TOP + i * LIST_GAP);
        }
        font.drawString(g, "Exit", LEFT_X, LIST_TOP + topics.length * LIST_GAP, FS);
        if (talkIndex == topics.length) drawHeart(g, 40, LIST_TOP + topics.length * LIST_GAP);
    }

    private void drawConfirm(Graphics2D g, String prompt) {
        int y = LIST_TOP;
        for (String line : wrap(prompt, 540)) {
            font.drawString(g, line, LEFT_X, y, FS);
            y += 48;
        }
        font.drawString(g, "Yes", LEFT_X, YES_Y, FS);
        font.drawString(g, "No", LEFT_X, NO_Y, FS);
        drawHeart(g, 40, YES_Y + confirmIndex * LIST_GAP);
    }

    private void drawHeart(Graphics2D g, int x, int y) {
        if (heartSprite == null) return;
        int hs = (int) (9 * FS);
        int hh = font.getHeight(FS);
        g.drawImage(heartSprite, x, y + (hh - hs) / 2 + 1, hs, hs, null);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ", -1)) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (font.stringWidth(test, FS) <= maxWidth) {
                line = new StringBuilder(test);
            } else {
                if (line.length() > 0) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        lines.add(line.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }
}
