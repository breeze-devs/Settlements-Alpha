package dev.breezes.settlements.domain.farming.hive;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HiveHarvestItemEntry {

    @SerializedName("item")
    String itemId;

    @SerializedName("weight")
    double weight;

    @SerializedName("min_count")
    int minCount;

    @SerializedName("max_count")
    int maxCount;

}
