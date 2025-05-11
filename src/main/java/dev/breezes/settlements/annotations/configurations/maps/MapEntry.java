package dev.breezes.settlements.annotations.configurations.maps;

public @interface MapEntry {

    String key();

    String value() default "";

}
