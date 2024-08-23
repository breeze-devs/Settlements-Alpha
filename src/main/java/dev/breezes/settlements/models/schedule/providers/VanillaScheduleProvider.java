package dev.breezes.settlements.models.schedule.providers;

import dev.breezes.settlements.models.schedule.ISchedule;
import dev.breezes.settlements.models.schedule.IScheduleProvider;

public class VanillaScheduleProvider implements IScheduleProvider {

    @Override
    public ISchedule provideSchedule() {
        return null;
    }

    /*
     * Daily planning: happens every day at 0? or another time?
     * - This selects a small subset of behaviors (with weights?) for each activity from the larger pool of behaviors
     * - This can be based on random selection, or based on some other criteria (e.g. LLM)
     * - Perhaps "core memories" can be used to influence this selection
     * - Or if there's some "occasion" happening, the behaviors can be selected based on that?
     * - This approach should allow for behavior diversity and be less laggy
     *
     * Dynamic activity selection: happens every time a behavior ends
     * - This selects a new behavior (randomly? weighted random?) from the subset of behaviors selected during daily planning
     * - Priority system
     */

}
