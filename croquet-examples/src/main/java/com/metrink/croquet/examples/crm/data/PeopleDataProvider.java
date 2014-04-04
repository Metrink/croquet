package com.metrink.croquet.examples.crm.data;


import java.util.Iterator;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * An extension of {@link GenericDataProvider} that is specialized for {@link PeopleBean}s.
 */
public class PeopleDataProvider extends GenericDataProvider<PeopleBean> {
    private static final long serialVersionUID = 3045313935791682897L;

    private static final Logger LOG = LoggerFactory.getLogger(PeopleDataProvider.class);

    private final Integer companyId;

    @Inject
    PeopleDataProvider(final EntityManager entityManager, @Nullable @Assisted final Integer companyId) {
        super(entityManager, PeopleBean.class);

        this.companyId = companyId;
    }

    @Override
    public Iterator<? extends PeopleBean> iterator(final long first, final long count) {
        final EntityManager entityManager = getEntityManager();

        entityManager.getTransaction().begin();

        try {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<PeopleBean> criteria = criteriaBuilder.createQuery(PeopleBean.class);
            final Root<PeopleBean> from = criteria.from(PeopleBean.class);

            // add the criteria if we have one
            if(companyId != null) {
                criteria.where(criteriaBuilder.equal(from.get("company"), companyId));
            }

            // setup the sort
            final SortParam<String> param = getSort();

            if(param == null || param.isAscending()) {
                LOG.debug("Setting order by to ascending for PeopleBean");
                criteria.orderBy(criteriaBuilder.asc(from.get("name")));
            } else {
                LOG.debug("Setting order by to descending for PeopleBean");
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
        final EntityManager entityManager = getEntityManager();

        // add the criteria if we have one (not as elegant as the one above)
        if(companyId != null) {
            return entityManager
                    .createQuery("select count(*) from PeopleBean where COMPANYID = " + companyId, Long.class)
                    .getSingleResult();
        } else {
            return entityManager.createQuery("select count(*) from PeopleBean", Long.class)
                    .getSingleResult();
        }
    }

    /**
     * Factory class for the {@link PeopleDataProvider}.
     */
    public interface PeopleDataProviderFactory {
        /**
         * Factory method for creating a new {@link PeopleDataProvider}.
         * @param companyId company to filter on, or null for everything.
         * @return an {@link PeopleDataProvider}.
         */
        public PeopleDataProvider create(final Integer companyId);
}


}
