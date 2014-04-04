package com.metrink.croquet.inject;

import java.io.ObjectStreamException;

/**
 * Interface to ensure writeReplace is called.
 */
public interface IWriteReplace {

    /**
     * Write replace method.
     * @return the new object.
     * @throws ObjectStreamException .
     */
    Object writeReplace() throws ObjectStreamException;
}
