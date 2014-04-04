package com.metrink.croquet.examples.crm.data;

import java.io.Serializable;


/**
 * Interface that provides an ID for a bean.
 */
public interface Identifiable extends Serializable {

    /**
     * Gets the ID for the Serializable instance of an object.
     * @return the ID of the object.
     */
    public Integer getId();
}
