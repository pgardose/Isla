package isla;

/**
 * What happened when the player interacted with something. The game layer
 * decides how to show it (dialogue box or shop screen), so Interactable
 * implementations never print or draw anything themselves.
 */
public final class InteractionResult {

    public enum Kind { DIALOGUE, SHOP }

    private final Kind kind;
    private final String text;
    private final Shop shop;

    private InteractionResult(Kind kind, String text, Shop shop) {
        this.kind = kind;
        this.text = text;
        this.shop = shop;
    }

    public static InteractionResult dialogue(String text) {
        return new InteractionResult(Kind.DIALOGUE, text, null);
    }

    public static InteractionResult shop(String greeting, Shop shop) {
        return new InteractionResult(Kind.SHOP, greeting, shop);
    }

    public Kind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    /** The shop to open; only non-null when kind == SHOP. */
    public Shop getShop() {
        return shop;
    }
}
