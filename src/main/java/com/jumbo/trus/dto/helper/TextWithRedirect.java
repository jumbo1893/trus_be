package com.jumbo.trus.dto.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextWithRedirect {

    private String title;
    private String text;
    private RedirectDTO redirect;

    public TextWithRedirect(StringAndString stringAndString) {
        this.title = stringAndString.getTitle();
        this.text = stringAndString.getText();
    }
}
