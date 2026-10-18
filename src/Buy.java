import java.util.ArrayList;
import java.util.List;

/** Seam's Chapter 1 stock. Manual is one-time (SOLD OUT); the rest are repeatable. */
public class Buy {
    public static class Item {
        public final String name, description;
        public final int price;
        public final boolean oneTime;
        public boolean soldOut = false;
        public int owned = 0;
        public Item(String n, int p, String d, boolean o) { name = n; price = p; description = d; oneTime = o; }
    }
    public enum Result { SUCCESS, NO_FUNDS, SOLD_OUT }
    private final List<Item> items = new ArrayList<Item>();
    public Buy() {
        items.add(new Item("Dark Candy", 40, "Heals 40HP. Tastes like darkness, but sweet.", false));
        items.add(new Item("ReviveMint", 100, "Revives a downed ally with 25% HP. Minty.", false));
        items.add(new Item("Manual", 50, "A fighting manual. Only one in stock!", true));
        items.add(new Item("Reynold", 10, "It's Reynold. Don't ask why he's for sale.", false));
    }
    public List<Item> getItems() { return items; }
    public Result buy(Item it, Money m) {
        if (it == null || it.soldOut) return Result.SOLD_OUT;
        if (!m.canAfford(it.price)) return Result.NO_FUNDS;
        m.spend(it.price);
        it.owned++;
        if (it.oneTime) it.soldOut = true;
        return Result.SUCCESS;
    }
}
