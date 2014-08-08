/*
 * Copyright 2014 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package com.netflix.genie.server.repository.jpa;

import com.netflix.genie.common.model.Cluster;
import com.netflix.genie.common.model.Cluster_;
import com.netflix.genie.common.model.ClusterStatus;
import com.netflix.genie.common.model.Command;
//import com.netflix.genie.common.model.Command_;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Tests for the application specifications.
 *
 * @author tgianos
 */
public class TestClusterSpecs {

    private static final String NAME = "h2prod";
    private static final String TAG_1 = "prod";
    private static final String TAG_2 = "yarn";
    private static final String TAG_3 = "hadoop";
    private static final ClusterStatus STATUS_1 = ClusterStatus.UP;
    private static final ClusterStatus STATUS_2 = ClusterStatus.OUT_OF_SERVICE;
    private static final List<String> TAGS = new ArrayList<String>();
    private static final List<ClusterStatus> STATUSES = new ArrayList<ClusterStatus>();
    private static final Long MIN_UPDATE_TIME = 123467L;
    private static final Long MAX_UPDATE_TIME = 1234643L;

    private Root<Cluster> root;
    private CriteriaQuery<?> cq;
    private CriteriaBuilder cb;
    private ListJoin<Cluster, Command> commands;

    /**
     * Setup test wide variables.
     */
    @BeforeClass
    public static void setupClass() {
        TAGS.add(TAG_1);
        TAGS.add(TAG_2);
        TAGS.add(TAG_3);

        STATUSES.add(STATUS_1);
        STATUSES.add(STATUS_2);
    }

    /**
     * Setup some variables.
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        this.root = (Root<Cluster>) Mockito.mock(Root.class);
        this.cq = Mockito.mock(CriteriaQuery.class);
        this.cb = Mockito.mock(CriteriaBuilder.class);
        this.commands = (ListJoin<Cluster, Command>) Mockito.mock(ListJoin.class);

        final Path<String> clusterNamePath = (Path<String>) Mockito.mock(Path.class);
        final Predicate likeNamePredicate = Mockito.mock(Predicate.class);
        Mockito.when(this.root.get(Cluster_.name)).thenReturn(clusterNamePath);
        Mockito.when(this.cb.like(clusterNamePath, NAME))
                .thenReturn(likeNamePredicate);

        final Path<Date> minUpdatePath = (Path<Date>) Mockito.mock(Path.class);
        final Predicate greaterThanOrEqualToPredicate = Mockito.mock(Predicate.class);
        Mockito.when(this.root.get(Cluster_.updated)).thenReturn(minUpdatePath);
        Mockito.when(this.cb.greaterThanOrEqualTo(minUpdatePath, new Date(MIN_UPDATE_TIME)))
                .thenReturn(greaterThanOrEqualToPredicate);

        final Path<Date> maxUpdatePath = (Path<Date>) Mockito.mock(Path.class);
        final Predicate lessThanPredicate = Mockito.mock(Predicate.class);
        Mockito.when(this.root.get(Cluster_.updated)).thenReturn(maxUpdatePath);
        Mockito.when(this.cb.lessThan(maxUpdatePath, new Date(MAX_UPDATE_TIME)))
                .thenReturn(lessThanPredicate);

        final Expression<Set<String>> tagExpression = (Expression<Set<String>>) Mockito.mock(Expression.class);
        final Predicate isMemberTagPredicate = Mockito.mock(Predicate.class);
        Mockito.when(this.root.get(Cluster_.tags)).thenReturn(tagExpression);
        Mockito.when(this.cb.isMember(Mockito.any(String.class), Mockito.eq(tagExpression)))
                .thenReturn(isMemberTagPredicate);

        final Path<ClusterStatus> statusPath = (Path<ClusterStatus>) Mockito.mock(Path.class);
        final Predicate equalStatusPredicate = Mockito.mock(Predicate.class);
        Mockito.when(this.root.get(Cluster_.status)).thenReturn(statusPath);
        Mockito.when(this.cb.equal(Mockito.eq(statusPath), Mockito.any(ClusterStatus.class)))
                .thenReturn(equalStatusPredicate);

        // Setup for findByClusterAndCommandCriteria
        Mockito.when(this.root.join(Cluster_.commands)).thenReturn(this.commands);
    }

    /**
     * Test the findByNameAndStatusesAndTagsAndUpdateTime specification.
     */
    @Test
    public void testFindByNameAndStatusesAndTagsAndUpdateTimeAll() {
        final Specification<Cluster> spec = ClusterSpecs
                .findByNameAndStatusesAndTagsAndUpdateTime(
                        NAME,
                        STATUSES,
                        TAGS,
                        MIN_UPDATE_TIME,
                        MAX_UPDATE_TIME
                );

        spec.toPredicate(this.root, this.cq, this.cb);
        Mockito.verify(this.cb, Mockito.times(1))
                .like(this.root.get(Cluster_.name), NAME);
        Mockito.verify(this.cb, Mockito.times(1))
                .greaterThanOrEqualTo(
                        this.root.get(Cluster_.updated), new Date(MIN_UPDATE_TIME)
                );
        Mockito.verify(this.cb, Mockito.times(1))
                .lessThan(
                        this.root.get(Cluster_.updated), new Date(MAX_UPDATE_TIME)
                );
        for (final String tag : TAGS) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .isMember(tag, this.root.get(Cluster_.tags));
        }
        for (final ClusterStatus status : STATUSES) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .equal(this.root.get(Cluster_.status), status);
        }
    }

    /**
     * Test the findByNameAndStatusesAndTagsAndUpdateTime specification.
     */
    @Test
    public void testFindByNameAndStatusesAndTagsAndUpdateTimeNoName() {
        final Specification<Cluster> spec = ClusterSpecs
                .findByNameAndStatusesAndTagsAndUpdateTime(
                        null,
                        STATUSES,
                        TAGS,
                        MIN_UPDATE_TIME,
                        MAX_UPDATE_TIME
                );

        spec.toPredicate(this.root, this.cq, this.cb);
        Mockito.verify(this.cb, Mockito.never())
                .like(this.root.get(Cluster_.name), NAME);
        Mockito.verify(this.cb, Mockito.times(1))
                .greaterThanOrEqualTo(
                        this.root.get(Cluster_.updated), new Date(MIN_UPDATE_TIME)
                );
        Mockito.verify(this.cb, Mockito.times(1))
                .lessThan(
                        this.root.get(Cluster_.updated), new Date(MAX_UPDATE_TIME)
                );
        for (final String tag : TAGS) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .isMember(tag, this.root.get(Cluster_.tags));
        }
        for (final ClusterStatus status : STATUSES) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .equal(this.root.get(Cluster_.status), status);
        }
    }

    /**
     * Test the findByNameAndStatusesAndTagsAndUpdateTime specification.
     */
    @Test
    public void testFindByNameAndStatusesAndTagsAndUpdateTimeNoStatuses() {
        final Specification<Cluster> spec = ClusterSpecs
                .findByNameAndStatusesAndTagsAndUpdateTime(
                        NAME,
                        null,
                        TAGS,
                        MIN_UPDATE_TIME,
                        MAX_UPDATE_TIME
                );

        spec.toPredicate(this.root, this.cq, this.cb);
        Mockito.verify(this.cb, Mockito.times(1))
                .like(this.root.get(Cluster_.name), NAME);
        Mockito.verify(this.cb, Mockito.times(1))
                .greaterThanOrEqualTo(
                        this.root.get(Cluster_.updated), new Date(MIN_UPDATE_TIME)
                );
        Mockito.verify(this.cb, Mockito.times(1))
                .lessThan(
                        this.root.get(Cluster_.updated), new Date(MAX_UPDATE_TIME)
                );
        for (final String tag : TAGS) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .isMember(tag, this.root.get(Cluster_.tags));
        }
        for (final ClusterStatus status : STATUSES) {
            Mockito.verify(this.cb, Mockito.never())
                    .equal(this.root.get(Cluster_.status), status);
        }
    }

    /**
     * Test the findByNameAndStatusesAndTagsAndUpdateTime specification.
     */
    @Test
    public void testFindByNameAndStatusesAndTagsAndUpdateTimeEmptyStatuses() {
        final Specification<Cluster> spec = ClusterSpecs
                .findByNameAndStatusesAndTagsAndUpdateTime(
                        NAME,
                        new ArrayList<ClusterStatus>(),
                        TAGS,
                        MIN_UPDATE_TIME,
                        MAX_UPDATE_TIME
                );

        spec.toPredicate(this.root, this.cq, this.cb);
        Mockito.verify(this.cb, Mockito.times(1))
                .like(this.root.get(Cluster_.name), NAME);
        Mockito.verify(this.cb, Mockito.times(1))
                .greaterThanOrEqualTo(
                        this.root.get(Cluster_.updated), new Date(MIN_UPDATE_TIME)
                );
        Mockito.verify(this.cb, Mockito.times(1))
                .lessThan(
                        this.root.get(Cluster_.updated), new Date(MAX_UPDATE_TIME)
                );
        for (final String tag : TAGS) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .isMember(tag, this.root.get(Cluster_.tags));
        }
        for (final ClusterStatus status : STATUSES) {
            Mockito.verify(this.cb, Mockito.never())
                    .equal(this.root.get(Cluster_.status), status);
        }
    }

    /**
     * Test the findByNameAndStatusesAndTagsAndUpdateTime specification.
     */
    @Test
    public void testFindByNameAndStatusesAndTagsAndUpdateTimeNoTags() {
        final Specification<Cluster> spec = ClusterSpecs
                .findByNameAndStatusesAndTagsAndUpdateTime(
                        NAME,
                        STATUSES,
                        null,
                        MIN_UPDATE_TIME,
                        MAX_UPDATE_TIME
                );

        spec.toPredicate(this.root, this.cq, this.cb);
        Mockito.verify(this.cb, Mockito.times(1))
                .like(this.root.get(Cluster_.name), NAME);
        Mockito.verify(this.cb, Mockito.times(1))
                .greaterThanOrEqualTo(
                        this.root.get(Cluster_.updated), new Date(MIN_UPDATE_TIME)
                );
        Mockito.verify(this.cb, Mockito.times(1))
                .lessThan(
                        this.root.get(Cluster_.updated), new Date(MAX_UPDATE_TIME)
                );
        for (final String tag : TAGS) {
            Mockito.verify(this.cb, Mockito.never())
                    .isMember(tag, this.root.get(Cluster_.tags));
        }
        for (final ClusterStatus status : STATUSES) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .equal(this.root.get(Cluster_.status), status);
        }
    }

    /**
     * Test the findByNameAndStatusesAndTagsAndUpdateTime specification.
     */
    @Test
    public void testFindByNameAndStatusesAndTagsAndUpdateTimeNoMinTime() {
        final Specification<Cluster> spec = ClusterSpecs
                .findByNameAndStatusesAndTagsAndUpdateTime(
                        NAME,
                        STATUSES,
                        TAGS,
                        null,
                        MAX_UPDATE_TIME
                );

        spec.toPredicate(this.root, this.cq, this.cb);
        Mockito.verify(this.cb, Mockito.times(1))
                .like(this.root.get(Cluster_.name), NAME);
        Mockito.verify(this.cb, Mockito.never())
                .greaterThanOrEqualTo(
                        this.root.get(Cluster_.updated), new Date(MIN_UPDATE_TIME)
                );
        Mockito.verify(this.cb, Mockito.times(1))
                .lessThan(
                        this.root.get(Cluster_.updated), new Date(MAX_UPDATE_TIME)
                );
        for (final String tag : TAGS) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .isMember(tag, this.root.get(Cluster_.tags));
        }
        for (final ClusterStatus status : STATUSES) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .equal(this.root.get(Cluster_.status), status);
        }
    }

    /**
     * Test the findByNameAndStatusesAndTagsAndUpdateTime specification.
     */
    @Test
    public void testFindByNameAndStatusesAndTagsAndUpdateTimeNoMax() {
        final Specification<Cluster> spec = ClusterSpecs
                .findByNameAndStatusesAndTagsAndUpdateTime(
                        NAME,
                        STATUSES,
                        TAGS,
                        MIN_UPDATE_TIME,
                        null
                );

        spec.toPredicate(this.root, this.cq, this.cb);
        Mockito.verify(this.cb, Mockito.times(1))
                .like(this.root.get(Cluster_.name), NAME);
        Mockito.verify(this.cb, Mockito.times(1))
                .greaterThanOrEqualTo(
                        this.root.get(Cluster_.updated), new Date(MIN_UPDATE_TIME)
                );
        Mockito.verify(this.cb, Mockito.never())
                .lessThan(
                        this.root.get(Cluster_.updated), new Date(MAX_UPDATE_TIME)
                );
        for (final String tag : TAGS) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .isMember(tag, this.root.get(Cluster_.tags));
        }
        for (final ClusterStatus status : STATUSES) {
            Mockito.verify(this.cb, Mockito.times(1))
                    .equal(this.root.get(Cluster_.status), status);
        }
    }

//    @Test
//    public void testFindByClusterAndCommandCriteriaNoCriteria() {
//        final Specification<Cluster> spec = ClusterSpecs
//                .findByClusterAndCommandCriteria(null, null);
//
//        spec.toPredicate(this.root, this.cq, this.cb);
//        Mockito.verify(this.cq, Mockito.times(1)).distinct(true);
//    }

    /**
     * Here for completeness.
     */
    @Test
    public void testProtectedConstructor() {
        final ClusterSpecs specs = new ClusterSpecs();
        Assert.assertEquals(ClusterSpecs.class, specs.getClass());
    }
}