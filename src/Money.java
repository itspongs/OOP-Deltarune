public class Money {
    private int amount;
    public Money(int start) { amount = Math.max(0, start); }
    public int get() { return amount; }
    public boolean canAfford(int price) { return price >= 0 && amount >= price; }
    public boolean spend(int price) {
        if (!canAfford(price)) return false;
        amount -= price;
        return true;
    }
    public void earn(int v) { if (v > 0) amount += v; }
}
