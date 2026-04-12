package com.jumbo.trus.dto.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextWithRedirect {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String title;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String text;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private WarningType warningType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RedirectDTO redirect;

    public TextWithRedirect(StringAndString stringAndString) {
        this.title = stringAndString.getTitle();
        this.text = stringAndString.getText();
    }
}
