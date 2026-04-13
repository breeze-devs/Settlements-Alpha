package dev.breezes.settlements.domain.farming.hive;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class HiveHarvestBlockData {

    @SerializedName("block")
    String block;

    @SerializedName("expertise_pools")
    Map<String, HiveHarvestExpertisePool> expertisePools;

}
