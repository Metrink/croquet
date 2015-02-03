package com.metrink.croquet.hibernate;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.sql.DataSource;

import net.sf.cglib.core.DefaultNamingPolicy;
import net.sf.cglib.core.Predicate;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.wicket.proxy.LazyInitProxyFactory.IWriteReplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;
import com.sop4j.dbutils.QueryRunner;


/**
 * Proxy class for {@link QueryRunner}.
 */
public class QueryRunnerProxyFactory {
    private static final Logger LOG = LoggerFactory.getLogger(QueryRunnerProxyFactory.class);

    private QueryRunnerProxyFactory() {
    }

    /**
     * Creates a proxy object that will write-replace with a wrapper around a {@link QueryRunner}.
     * @param dataSourceProvider a provider to generate DataSources.
     * @return the proxied instance.
     */
    public static QueryRunner createProxy(final Provider<DataSource> dataSourceProvider) {
        final QueryRunnerInterceptor handler = new QueryRunnerInterceptor(dataSourceProvider);

        final Enhancer e = new Enhancer();

        // make sure we're Serializable and have a write replace method
        e.setInterfaces(new Class[] { Serializable.class, IWriteReplace.class });
        e.setSuperclass(QueryRunner.class);
        e.setCallback(handler);
        e.setNamingPolicy(new DefaultNamingPolicy() {
            @Override
            public String getClassName(final String prefix,
                                       final String source,
                                       final Object key,
                                       final Predicate names) {
                return super.getClassName("PROXY_" + prefix, source, key, names);
            }
        });

        LOG.trace("Created proxy for an EntityManager");

        return (QueryRunner)e.create();

    }

    /**
     * Method interceptor for the proxy.
     */
    private static class QueryRunnerInterceptor
        implements MethodInterceptor, Serializable, IWriteReplace {

        private static final Logger LOG = LoggerFactory.getLogger(QueryRunnerInterceptor.class);
        private static final long serialVersionUID = 1L;

        private final Provider<DataSource> dataSourceProvider;
        private transient QueryRunner queryRunner;

        public QueryRunnerInterceptor(final Provider<DataSource> dataSourceProvider) {
            if(dataSourceProvider == null) {
                throw new IllegalArgumentException("DataSource provider is null");
            }

            this.dataSourceProvider = dataSourceProvider;
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

            if(queryRunner == null) {
                final DataSource dataSource = dataSourceProvider.get();

                queryRunner = new QueryRunner(dataSource);
            }

            return proxy.invoke(queryRunner, args);
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
            LOG.trace("Creating wrapper for a QueryRunner");

            return new QueryRunnerWrapper(dataSourceProvider);
        }

    }

    /**
     * A simple wrapper around an EntityManagerFactory to provide fresh instances after deserialization.
     */
    private static class QueryRunnerWrapper implements Serializable {

        private static final long serialVersionUID = 1L;
        private final Provider<DataSource> dataSourceProvider;

        public QueryRunnerWrapper(final Provider<DataSource> dataSourceProvider) {
            this.dataSourceProvider = dataSourceProvider;
        }

        private Object readResolve() throws ObjectStreamException {
            return QueryRunnerProxyFactory.createProxy(dataSourceProvider);
        }
    }

}
