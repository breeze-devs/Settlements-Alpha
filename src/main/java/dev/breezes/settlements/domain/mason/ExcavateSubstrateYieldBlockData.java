package dev.breezes.settlements.domain.mason;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ExcavateSubstrateYieldBlockData {

    @SerializedName("block")
    String block;

    @SerializedName("selection_weight")
    double selectionWeight;

    @SerializedName("expertise_pools")
    Map<String, ExcavateSubstrateYieldExpertisePool> expertisePools;

}
