package com.example.styloflow.mapper;

import com.example.styloflow.util.Money;
import org.mapstruct.MapperConfig;

/** Shared config for every mapper: Spring beans, and all {@code BigDecimal} values scaled to 2 decimals. */
@MapperConfig(componentModel = "spring", uses = Money.class)
public interface MapStructConfig {
}
