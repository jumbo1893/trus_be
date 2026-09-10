package com.jumbo.trus.service.activity.footbar.connect;

public class FootbarReconnectRequiredException extends RuntimeException {
    public FootbarReconnectRequiredException() {
        super("Propojení s Footbarem již není platné. Propojte prosím účet znovu.");
    }
}
