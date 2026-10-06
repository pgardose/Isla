package isla;

/** Outcome of Shop.buyItem. */
public enum PurchaseResult {
    SUCCESS,
    NOT_IN_STOCK,
    INSUFFICIENT_FUNDS,
    INVENTORY_FULL;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
