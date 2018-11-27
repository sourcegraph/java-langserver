package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by beyang on 7/26/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShowMessageParams {

    private MessageType type;

    private String message;

    public static ShowMessageParams of(MessageType type, String message) {
        ShowMessageParams p = new ShowMessageParams();
        p.setType(type);
        p.setMessage(message);
        return p;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
