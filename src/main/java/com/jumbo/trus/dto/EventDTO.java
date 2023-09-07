package com.jumbo.trus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {

    private String type;

    private Map<String, Object> body;

}
