package dev.breezes.settlements.models.actions;

public interface IAction<T> {

    /**
     * Should be called to progress the action
     */
    void tickAction(int delta);

}
