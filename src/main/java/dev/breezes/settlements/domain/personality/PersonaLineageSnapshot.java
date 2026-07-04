package dev.breezes.settlements.domain.personality;

/**
 * Transient birth-time capture of both parents' persona for a bred villager. Held on the child
 * only long enough for the persona-generation sweep to consume it into the wire request.
 */
public record PersonaLineageSnapshot(ParentPersona parentA, ParentPersona parentB) {

    public PersonaLineageSnapshot {
        parentA = parentA == null ? ParentPersona.unknown() : parentA;
        parentB = parentB == null ? ParentPersona.unknown() : parentB;
    }

    public static PersonaLineageSnapshot empty() {
        return new PersonaLineageSnapshot(ParentPersona.unknown(), ParentPersona.unknown());
    }

    public boolean hasAnySignal() {
        return parentA.hasSignal() || parentB.hasSignal();
    }

}
