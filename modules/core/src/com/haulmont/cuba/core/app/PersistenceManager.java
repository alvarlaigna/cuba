/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.haulmont.cuba.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.EntityStatistics;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.persistence.DbmsSpecificFactory;
import com.haulmont.cuba.core.sys.persistence.DbmsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import javax.inject.Inject;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinTable;
import javax.persistence.Table;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component(PersistenceManagerAPI.NAME)
public class PersistenceManager implements PersistenceManagerAPI {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected volatile Set<String> softDeleteTables;

    protected volatile Set<String> secondaryTables;

    protected Map<String, EntityStatistics> statisticsCache;

    @Inject
    protected Persistence persistence;

    @Inject
    protected Metadata metadata;

    @Inject
    protected PersistenceSecurity security;

    protected PersistenceConfig config;

    @Inject
    public void setConfiguration(Configuration configuration) {
        config = configuration.getConfig(PersistenceConfig.class);
    }

    @Override
    public boolean isSoftDeleteFor(String table) {
        if (softDeleteTables == null) {
            initSoftDeleteTables();
        }
        return softDeleteTables.contains(table.toLowerCase());
    }

    @Override
    public List<String> getSoftDeleteTables() {
        if (softDeleteTables == null) {
            initSoftDeleteTables();
        }
        ArrayList<String> list = new ArrayList<>(softDeleteTables);
        Collections.sort(list);
        return list;
    }

    @Override
    public boolean isSecondaryTable(String table) {
        if (secondaryTables == null) {
            initSecondaryTables();
        }
        return secondaryTables.contains(table);
    }

    protected synchronized void initSoftDeleteTables() {
        if (softDeleteTables == null) { // double checked locking
            log.debug("Searching for soft delete tables");
            HashSet<String> set = new HashSet<>();

            DataSource datasource = persistence.getDataSource();
            Connection conn = null;
            try {
                conn = datasource.getConnection();
                DatabaseMetaData metaData = conn.getMetaData();

                String schema = "oracle".equals(DbmsType.getType()) ? metaData.getUserName() : null;
                log.trace("[initSoftDeleteTables] schema=" + schema);

                ResultSet tables = metaData.getTables(null, schema, null, new String[]{"TABLE"});
                while (tables.next()) {
                    String table = tables.getString("TABLE_NAME");
                    log.trace("[initSoftDeleteTables] found table " + table);

                    if (table != null) {
                        String deleteTsColumn = DbmsSpecificFactory.getDbmsFeatures().getDeleteTsColumn();
                        ResultSet columns = metaData.getColumns(null, schema, table, deleteTsColumn);
                        if (columns.next()) {
                            log.trace("[initSoftDeleteTables] table " + table + " has column " + deleteTsColumn);
                            set.add(table.toLowerCase());
                        }
                        columns.close();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (conn != null) conn.close();
                } catch (SQLException ignored) {
                }
            }

            softDeleteTables = set;
        }
    }

    protected synchronized void initSecondaryTables() {
        if (secondaryTables == null) { // double checked locking
            log.debug("Searching for secondary tables");
            HashSet<String> set = new HashSet<>();

            Collection<MetaClass> metaClasses = metadata.getTools().getAllPersistentMetaClasses();
            for (MetaClass metaClass : metaClasses) {
                for (MetaProperty metaProperty : metaClass.getProperties()) {
                    JoinTable joinTable = metaProperty.getAnnotatedElement().getAnnotation(JoinTable.class);
                    if (joinTable != null) {
                        set.add(joinTable.name());
                    }
                }

                if (isJoinTableInheritance(metaClass)) {
                    Table table = (Table) metaClass.getJavaClass().getAnnotation(Table.class);
                    if (table != null) {
                        set.add(table.name());
                    }
                }
            }

            secondaryTables = set;
        }
    }

    private boolean isJoinTableInheritance(MetaClass metaClass) {
        for (MetaClass aClass : metaClass.getAncestors()) {
            Inheritance inheritance = (Inheritance) aClass.getJavaClass().getAnnotation(Inheritance.class);
            if (inheritance != null && inheritance.strategy() == InheritanceType.JOINED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean useLazyCollection(String entityName) {
        EntityStatistics es = getStatisticsCache().get(entityName);
        if (es == null || es.getInstanceCount() == null)
            return false;
        else {
            int threshold = es.getLazyCollectionThreshold() != null ? es.getLazyCollectionThreshold() : config.getDefaultLazyCollectionThreshold();
            return es.getInstanceCount() > threshold;
        }
    }

    @Override
    public boolean useLookupScreen(String entityName) {
        EntityStatistics es = getStatisticsCache().get(entityName);
        if (es == null || es.getInstanceCount() == null)
            return false;
        else {
            int threshold = es.getLookupScreenThreshold() != null ? es.getLookupScreenThreshold() : config.getDefaultLookupScreenThreshold();
            return es.getInstanceCount() > threshold;
        }
    }

    @Override
    public int getFetchUI(String entityName) {
        EntityStatistics es = getStatisticsCache().get(entityName);
        if (es != null && es.getFetchUI() != null)
            return es.getFetchUI();
        else
            return config.getDefaultFetchUI();
    }

    @Override
    public int getMaxFetchUI(String entityName) {
        EntityStatistics es = getStatisticsCache().get(entityName);
        if (es != null && es.getMaxFetchUI() != null)
            return es.getMaxFetchUI();
        else
            return config.getDefaultMaxFetchUI();
    }

    protected synchronized Map<String, EntityStatistics> getStatisticsCache() {
        if (statisticsCache == null) {
            statisticsCache = new ConcurrentHashMap<>();
            internalLoadStatisticsCache();
        }
        return statisticsCache;
    }

    protected void internalLoadStatisticsCache() {
        log.info("Loading statistics cache");
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            TypedQuery<EntityStatistics> q = em.createQuery("select s from sys$EntityStatistics s", EntityStatistics.class);
            List<EntityStatistics> list = q.getResultList();
            for (EntityStatistics es : list) {
                statisticsCache.put(es.getName(), es);
            }
            tx.commit();
        } finally {
            tx.end();
        }
    }

    @Override
    public synchronized void flushStatisticsCache() {
        statisticsCache = null;
    }

    @Override
    public synchronized EntityStatistics enterStatistics(String name, Long instanceCount, Integer fetchUI, Integer maxFetchUI,
                                               Integer lazyCollectionThreshold, Integer lookupScreenThreshold) {
        Transaction tx = persistence.createTransaction();
        EntityStatistics es;
        try {
            EntityManager em = persistence.getEntityManager();

            es = getEntityStatisticsInstance(name, em);

            if (instanceCount != null) {
                es.setInstanceCount(instanceCount);
            }
            if (fetchUI != null) {
                es.setFetchUI(fetchUI);
            }
            if (maxFetchUI != null) {
                es.setMaxFetchUI(maxFetchUI);
            }
            if (lazyCollectionThreshold != null) {
                es.setLazyCollectionThreshold(lazyCollectionThreshold);
            }
            if (lookupScreenThreshold != null) {
                es.setLookupScreenThreshold(lookupScreenThreshold);
            }

            tx.commit();
        } finally {
            tx.end();
        }
        flushStatisticsCache();
        return es;
    }

    @Override
    public SortedMap<String, EntityStatistics> getEntityStatistics() {
        return new TreeMap<>(getStatisticsCache());
    }

    @Override
    public void deleteStatistics(String name) {
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            Query q = em.createQuery("delete from sys$EntityStatistics s where s.name = ?1");
            q.setParameter(1, name);
            q.executeUpdate();

            tx.commit();
        } finally {
            tx.end();
        }
        flushStatisticsCache();
    }

    @Override
    public void refreshStatisticsForEntity(String name) {
        log.debug("Refreshing statistics for entity " + name);
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            Query q = em.createQuery("select count(e) from " + name + " e");
            Long count = (Long) q.getSingleResult();

            EntityStatistics es = getEntityStatisticsInstance(name, em);
            es.setInstanceCount(count);
            getStatisticsCache().put(name, es);

            tx.commit();
        } finally {
            tx.end();
        }
    }

    protected EntityStatistics getEntityStatisticsInstance(String name, EntityManager em) {
        TypedQuery<EntityStatistics> q =
                em.createQuery("select s from sys$EntityStatistics s where s.name = ?1", EntityStatistics.class);
        q.setParameter(1, name);
        List<EntityStatistics> list = q.getResultList();

        EntityStatistics es;
        if (list.isEmpty()) {
            es = metadata.create(EntityStatistics.class);
            es.setName(name);
            em.persist(es);
        } else {
            es = list.get(0);
        }
        return es;
    }
}