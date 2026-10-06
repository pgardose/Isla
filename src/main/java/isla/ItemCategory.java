package isla;

/** The three item categories shown as inventory tabs. */
public enum ItemCategory {
    CONSUMABLE("Consumables"),
    TOOL("Tools"),
    SOUVENIR("Souvenirs");

    private final String label;

    ItemCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
