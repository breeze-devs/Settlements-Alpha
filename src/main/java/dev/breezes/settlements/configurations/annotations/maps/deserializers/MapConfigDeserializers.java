package dev.breezes.settlements.configurations.annotations.maps.deserializers;

public class MapConfigDeserializers {

    public static MapConfigDeserializer<?> getDeserializer(String deserializer) {
        return switch (deserializer) {
            case "StringToString" -> new StringToStringMapConfigDeserializer();
            case "StringToInteger" -> new StringToIntegerMapConfigDeserializer();
//            case "StringToLong" -> new StringToLongMapConfigDeserializer();
//            case "StringToDouble" -> new StringToDoubleMapConfigDeserializer();
            default -> throw new IllegalArgumentException("Unknown deserializer: " + deserializer);
        };
    }

}
