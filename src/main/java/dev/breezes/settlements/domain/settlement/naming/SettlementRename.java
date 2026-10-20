package dev.breezes.settlements.domain.settlement.naming;

import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents the result of a settlement renaming attempt.
 */
public sealed interface SettlementRename permits SettlementRename.Renamed, SettlementRename.Unchanged {

    /**
     * Resolves requested name using {@link SettlementNameGenerator#numbered}, treating currentName as available.
     * Returns {@link Unchanged} if the resolved name exactly equals currentName, or {@link Renamed} otherwise.
     *
     * @param levelNames the unique settlement names in the level, optionally including currentName
     * @throws IllegalArgumentException if currentName or requested is blank
     */
    static SettlementRename of(@Nonnull String currentName,
                               @Nonnull String requested,
                               @Nonnull Collection<String> levelNames) {
        // Names are unique in a level, so removing one occurrence removes the settlement's own name and nothing else
        List<String> otherNames = new ArrayList<>(levelNames);
        otherNames.remove(currentName);

        String name = SettlementNameGenerator.numbered(requested, otherNames);
        return name.equals(currentName) ? new Unchanged(currentName) : new Renamed(currentName, name);
    }

    /**
     * A rename result that changes the settlement's name.
     */
    record Renamed(String previousName, String name) implements SettlementRename {

        /**
         * @param previousName the name before the rename
         * @param name         the name after the rename
         * @throws IllegalArgumentException if either name is blank or the names are equal
         */
        public Renamed {
            Validate.notBlank(previousName, "Previous settlement name must not be blank");
            Validate.notBlank(name, "Settlement name must not be blank");
            Validate.isTrue(!previousName.equals(name), "A rename must change the name: %s", name);
        }

    }

    /**
     * A rename result that keeps the settlement's name.
     */
    record Unchanged(String name) implements SettlementRename {

        /**
         * @param name the name retained by the rename
         * @throws IllegalArgumentException if name is blank
         */
        public Unchanged {
            Validate.notBlank(name, "Settlement name must not be blank");
        }

    }

}
