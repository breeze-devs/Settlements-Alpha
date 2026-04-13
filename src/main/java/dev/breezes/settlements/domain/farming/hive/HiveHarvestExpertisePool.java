package dev.breezes.settlements.domain.farming.hive;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class HiveHarvestExpertisePool {

    @SerializedName("rolls")
    int rolls;

    @SerializedName("items")
    List<HiveHarvestItemEntry> items;

}
