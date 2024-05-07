package io.smallrye.common.net;

import static java.lang.invoke.MethodHandles.lookup;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "SRCOM", length = 5)
interface Messages {
    Messages msg = org.jboss.logging.Messages.getBundle(lookup(), Messages.class);

    @Message(id = 2000, value = "Invalid address string \"%s\"")
    IllegalArgumentException invalidAddress(String address);

    @Message(id = 2001, value = "Invalid address length of %d; must be 4 or 16")
    IllegalArgumentException invalidAddressBytes(int length);

}
