/** Sell-back at half price, drawn from the owned counters in Buy. */
public class Sell {
    private final Buy stock;
    public Sell(Buy stock) { this.stock = stock; }
    public void giveStartingInventory() {
        stock.getItems().get(0).owned += 1;
        stock.getItems().get(3).owned += 1;
    }
    public int sellPrice(Buy.Item it) { return Math.max(1, it.price / 2); }
    public boolean sell(Buy.Item it, Money m) {
        if (it == null || it.soldOut || it.owned <= 0) return false;
        it.owned--;
        m.earn(sellPrice(it));
        return true;
    }
    public boolean hasAnythingToSell() {
        for (Buy.Item it : stock.getItems()) {
            if (!it.soldOut && it.owned > 0) return true;
        }
        return false;
    }
}
