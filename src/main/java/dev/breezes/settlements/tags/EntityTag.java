package dev.breezes.settlements.tags;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum EntityTag {

    /**
     * Indicates that this animal is owned by a village
     */
    VILLAGE_OWNED_ANIMAL("village_owned_animal"),
    ;

    private static final String NAMESPACE = "settlements";
    private final String name;

    public String getTag() {
        return NAMESPACE + ":" + this.name;
    }

}
