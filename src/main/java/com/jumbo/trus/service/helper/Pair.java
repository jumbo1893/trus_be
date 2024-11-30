package com.jumbo.trus.service.helper;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair<S, T> {
    private final S first;
    private final T second;
}
