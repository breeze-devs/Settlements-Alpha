package dev.breezes.settlements.infrastructure.config.annotations.maps;

public @interface MapEntry {

    String key();

    String value() default "";

}
