package dev.breezes.settlements.domain.mason;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ExcavateSubstrateYieldExpertisePool {

    @SerializedName("rolls")
    int rolls;

    @SerializedName("items")
    List<ExcavateSubstrateYieldItemEntry> items;

}
