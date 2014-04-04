package com.metrink.croquet.examples.crm.data;

import java.io.Serializable;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A generic {@link SortableDataProvider} that reads from a DB through an {@link EntityManager}.
 *
 * @param <T> the type of the entity to read.
 */
public class GenericDataProvider<T extends Serializable> extends SortableDataProvider<T, String> {

    private static final long serialVersionUID = -4513383410805199586L;

    private static final Logger LOG = LoggerFactory.getLogger(GenericDataProvider.class);

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("SE_BAD_FIELD")
    private final EntityManager entityManager;
    private final Class<T> type;

    /**
     * Constructs an instance of the {@link GenericDataProvider} injecting an instance of the {@link EntityManager}.
     * @param entityManager the injected {@link EntityManager} to be used for queries.
     * @param type the type to associate with the data provider.
     */
    @Inject
    GenericDataProvider(final EntityManager entityManager, @Assisted final Class<T> type) {
        this.entityManager = entityManager;
        this.type = type;
    }

    @Override
    public IModel<T> model(final T arg) {
        return new Model<T>(arg);
    }

    /**
     * Get the entity manager for classes that extend this one.
     * @return returns the entity manager.
     */
    EntityManager getEntityManager() {
        return entityManager;
    }

    /*
     * Note we cannot use the @Transactional annotation here because it makes
     * not serializable: https://code.google.com/p/google-guice/issues/detail?id=12
     */
    @Override
    public Iterator<? extends T> iterator(final long first, final long count) {
        entityManager.getTransaction().begin();

        try {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<T> criteria = criteriaBuilder.createQuery(type);
            final Root<T> from = criteria.from(type);

            // setup the sort
            final SortParam<String> param = getSort();

            if(param == null || param.isAscending()) {
                LOG.debug("Setting order by to ascending for {}", type.getName());
                criteria.orderBy(criteriaBuilder.asc(from.get("name")));
            } else {
                LOG.debug("Setting order by to descending for {}", type.getName());
                criteria.orderBy(criteriaBuilder.desc(from.get("name")));
            }

            return entityManager.createQuery(criteria)
                                .setFirstResult((int)first)
                                .setMaxResults((int)count)
                                .getResultList()
                                .iterator();
        } finally {
            entityManager.getTransaction().commit();
        }
    }

    @Override
    public long size() {
        return entityManager.createQuery("select count(*) from " + type.getName(), Long.class)
                                .getSingleResult();
    }


    /**
     * Factory class for the {@link GenericDataProvider}.
     */
    public interface GenericDataProviderFactory<T2 extends Serializable> {
        /**
         * Factory method for creating a new {@link GenericDataProvider} given a type.
         * @param type the entity type to associate with this {@link GenericDataProvider}.
         * @return an {@link GenericDataProvider}.
         */
        public GenericDataProvider<T2> create(final Class<T2> type);
    }

}
