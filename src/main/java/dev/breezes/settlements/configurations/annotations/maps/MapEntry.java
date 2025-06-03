package dev.breezes.settlements.configurations.annotations.maps;

public @interface MapEntry {

    String key();

    String value() default "";

}
