package dev.breezes.settlements.models.actions;

import java.util.List;

public interface IActionPlan<T> {

    List<IActionStep<T>> actionSteps();

}
