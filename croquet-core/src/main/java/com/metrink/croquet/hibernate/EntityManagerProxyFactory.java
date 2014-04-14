package com.metrink.croquet.hibernate;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.persistence.EntityManager;

import net.sf.cglib.core.DefaultNamingPolicy;
import net.sf.cglib.core.Predicate;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metrink.croquet.inject.IWriteReplace;

/**
 * Factory class that generates a proxy instance for {@link EntityManager}s.
 */
class EntityManagerProxyFactory {
    private static final Logger LOG = LoggerFactory.getLogger(EntityManagerProxyFactory.class);

    private EntityManagerProxyFactory() { }

    /**
     * Creates a proxy object that will write-replace with a wrapper around the {@link EntityManager}.
     * @param factory a factory to generate EntityManagers.
     * @return the proxied instance.
     */
    static EntityManager createProxy(final HibernateEntityManagerFactory factory) {
        final EntityManagerInterceptor handler = new EntityManagerInterceptor(factory);

        final Enhancer e = new Enhancer();

        // make sure we're Serializable and have a write replace method
        e.setInterfaces(new Class[] { EntityManager.class, Serializable.class, IWriteReplace.class });
        e.setSuperclass(Object.class);
        e.setCallback(handler);
        e.setNamingPolicy(new DefaultNamingPolicy() {
            @Override
            public String getClassName(final String prefix,
                                       final String source,
                                       final Object key,
                                       final Predicate names) {
                return super.getClassName("CROQUET_ENTITY_MANAGER_PROXY_" + prefix, source, key, names);
            }
        });

        LOG.trace("Created proxy for an EntityManager");

        return (EntityManager)e.create();
    }

    /**
     * Method interceptor for the proxy.
     */
    private static class EntityManagerInterceptor implements MethodInterceptor,
                                                     Serializable,
                                                     IWriteReplace {

        private static final Logger LOG = LoggerFactory.getLogger(EntityManagerInterceptor.class);
        private static final long serialVersionUID = 1L;
        private final HibernateEntityManagerFactory factory;

        private transient EntityManager entityManager;

        /**
         * Constructor.
         *
         * @param instance the original instance of the class.
         */
        public EntityManagerInterceptor(final HibernateEntityManagerFactory factory) {
            if(factory == null) {
                throw new IllegalArgumentException("HibernateEntityManagerFactory is null");
            }
            this.factory = factory;
        }

        @Override
        public Object intercept(final Object object,
                                final Method method,
                                final Object[] args,
                                final MethodProxy proxy)
                                        // CHECKSTYLE:OFF
                                        throws Throwable {
                                        // CHECKSTYLE:ON

            if(isMethod(method, void.class, "finalize")) {
                // swallow finalize call
                return null;
            } else if(isMethod(method, boolean.class, "equals", Object.class)) {
                return (equals(args[0])) ? Boolean.TRUE : Boolean.FALSE;
            } else if(isMethod(method, int.class, "hashCode")) {
                return hashCode();
            } else if(isMethod(method, String.class, "toString")) {
                return toString();
            } else if(isMethod(method, Object.class, "writeReplace")) {
                return writeReplace();
            }

            if(entityManager == null || !entityManager.isOpen()) {
                entityManager = factory.createEntityManager();
            }

            return proxy.invoke(entityManager, args);
        }

        private boolean isMethod(final Method method,
                                 final Class<?> retClass,
                                 final String name,
                                 final Class<?>... paramClasses) {

            if(method.getReturnType() != retClass) {
                return false;
            }

            if(!method.getName().equals(name)) {
                return false;
            }

            if(!Arrays.equals(method.getParameterTypes(), paramClasses)) {
                return false;
            }

            return true;
        }

        @Override
        public Object writeReplace() throws ObjectStreamException {
            LOG.trace("Creating wrapper for an EntityManager");

            if(entityManager != null && entityManager.isOpen()) {
                // make sure we close our transactions
                entityManager.close();
                entityManager = null;
            }

            return new EntityManagerWrapper(factory);
        }

    }

    /**
     * A simple wrapper around an EntityManagerFactory to provide fresh instances after deserialization.
     */
    private static class EntityManagerWrapper implements Serializable {

        private static final long serialVersionUID = 1L;
        private final HibernateEntityManagerFactory factory;

        public EntityManagerWrapper(final HibernateEntityManagerFactory factory) {
            this.factory = factory;
        }

        private Object readResolve() throws ObjectStreamException {
            return EntityManagerProxyFactory.createProxy(factory);
        }
    }

}
