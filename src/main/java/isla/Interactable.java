package isla;

/**
 * Anything the player can interact with.
 *
 * OOP note (POLYMORPHISM + ABSTRACTION): the game calls interact(player)
 * without knowing whether it is talking to a Vendor or a QuestGiver; each
 * implementation returns a different InteractionResult.
 */
public interface Interactable {

    InteractionResult interact(Player player);
}
