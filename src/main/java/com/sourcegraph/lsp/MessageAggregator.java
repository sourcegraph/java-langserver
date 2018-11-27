package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.params.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * MessageAggregator aggregates error and warning messages that should trigger a user-visible message. It should be used
 * when the volume of individual messages would overwhelm the user and where an aggregate message would be more
 * appropriate to display in lieu of all the individual messages. It presents a SL4J-like interface for reporting
 * the individual messages.
 *
 * The client should call the `error`, `warn`, and `info` methods to log these messages (which also log to the console).
 * When `passthrough` is set to false (the default), a MessageAggregator will store the messages it receives
 * until `showAggregateMessageAndClear` is called, at which point if there are accrued error/warning messages, an
 * aggregate message through its messenger and all accrued error/warning messages are cleared.
 *
 * When `passthrough` is set to true, no error/warning messages are accrued and the MessageAggregator simply passes
 * through each message to its messenger. This is useful when some messages might be sent asynchronously (and thus
 * after `showAggregateMessageAndClear` has been called for the last time).
 *
 * MessageAggregator is thread-safe.
 *
 * Created by beyang on 7/27/17.
 */
public class MessageAggregator {

    private static final Logger log = LoggerFactory.getLogger(MessageAggregator.class);

    /**
     * messenger is how messages are actually sent. If null, the MessageAggregator will silently send no messages
     * (convenient for test code where no underlying messenger is needed).
     */
    @Nullable
    private Messenger messenger;

    /**
     * Accrued error, warning, and info messages.
     */
    private List<String> errors;
    private List<String> warnings;
    private List<String> infos;

    /**
     * prefix is appended to all individual (non-aggregate) messages that a MessageAggregator sends.
     */
    private String prefix;

    /**
     * If passthrough is true, the MessageAggregator just passes through messages and does not accrue them.
     */
    private boolean passthrough;

    public MessageAggregator(Messenger messenger) {
        this(messenger, "");
    }

    public MessageAggregator(Messenger messenger, String prefix) {
        this.messenger = messenger;
        this.passthrough = false;
        this.prefix = prefix != null ? prefix : "";
        errors = new ArrayList<>();
        warnings = new ArrayList<>();
        infos = new ArrayList<>();
    }

    public synchronized void setPassthrough(boolean passthrough) {
        this.passthrough = passthrough;
    }

    /**
     * Shows the aggregate message if there are any accrued errors or warnings, and then clears the accrued errors and
     * warnings. In addition, shows the first individual message of the aggregate message's level.
     * The level of the aggregate message is the maximum level of accrued messages. Currently, no aggregate message is
     * sent if there are no errors or warnings (i.e., only info-level messages).
     *
     * Returns true if and only if an aggregate message was sent.
     */
    public synchronized boolean showAggregateMessageAndClear(String aggregateMessage) {
        if (errors.size() > 0) {
            if (errors.size() == 1) {
                showMessage(MessageType.ERROR, prefix + errors.get(0));
            } else if (errors.size() == 2) {
                showMessage(MessageType.ERROR, prefix + errors.get(0) + " (and 1 other error)");
            } else {
                showMessage(MessageType.ERROR, prefix + errors.get(0) + " (and " + (errors.size() - 1) + " other errors)");
            }
            showMessage(MessageType.ERROR, aggregateMessage);
            clear();
            return true;
        } else if (warnings.size() > 0) {
            if (warnings.size() == 1) {
                showMessage(MessageType.WARNING, prefix + warnings.get(0));
            } else if (warnings.size() == 2) {
                showMessage(MessageType.WARNING, prefix + warnings.get(0) + " (and 1 other warning)");
            } else {
                showMessage(MessageType.WARNING, prefix + warnings.get(0) + " (and " + (warnings.size() - 1) + " other warnings)");
            }
            showMessage(MessageType.WARNING, aggregateMessage);
            clear();
            return true;
        }
        clear();
        return false;
    }

    public synchronized void error(String msg) {
        log.error(msg);
        if (passthrough) {
            showMessage(MessageType.ERROR, prefix + msg);
        } else {
            errors.add(msg);
        }
    }

    public synchronized void warn(String msg) {
        log.warn(msg);
        if (passthrough) {
            showMessage(MessageType.WARNING, prefix + msg);
        } else {
            warnings.add(msg);
        }
        warnings.add(msg);
    }

    public synchronized void info(String msg) {
        log.info(msg);
        if (passthrough) {
            showMessage(MessageType.INFO, prefix + msg);
        } else {
            infos.add(msg);
        }
    }

    /**
     * Convenience methods to mirror the SL4J API.
     */

    public synchronized void error(String format, Throwable t) {
        error(MessageFormatter.format(format, t).getMessage());
    }

    public synchronized void error(String format, Object obj) {
        error(MessageFormatter.format(format, obj).getMessage());
    }

    public synchronized void error(String format, Object obj1, Object obj2) {
        error(MessageFormatter.format(format, obj1, obj2).getMessage());
    }

    public synchronized void warn(String format, Object obj) {
        warn(MessageFormatter.format(format, obj).getMessage());
    }

    public synchronized void warn(String format, Object obj1, Object obj2) {
        warn(MessageFormatter.format(format, obj1, obj2).getMessage());
    }

    public synchronized void warn(String format, Object ...objs) {
        warn(MessageFormatter.arrayFormat(format, objs).getMessage());
    }

    public synchronized void info(String format, Object obj) {
        info(MessageFormatter.format(format, obj).getMessage());
    }

    private synchronized void showMessage(MessageType messageType, String message) {
        if (messenger == null) {
            return;
        }
        messenger.showMessage(messageType, message);
    }

    /**
     * Clears all accrued messages.
     */
    private synchronized void clear() {
        errors.clear();
        warnings.clear();
        infos.clear();
    }
}
