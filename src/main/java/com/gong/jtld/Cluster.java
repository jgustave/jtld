package com.gong.jtld;

import jpaul.DataStructs.UnionFind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class Cluster {


    public List<Jtdl.SubResult> cluster (List<Jtdl.SubResult> subResults) {
        List<Jtdl.SubResult>       result      = new ArrayList<Jtdl.SubResult>();
        UnionFind<Jtdl.SubResult>  unionFinder = new UnionFind<Jtdl.SubResult>();

        for( int x=0;x<subResults.size();x++) {
            for( int y=x+1;y<subResults.size();y++) {
                float overlap = subResults.get(x).boundingBox.overlap(subResults.get(y).boundingBox);
                if( overlap > 0.5f ) {
                    unionFinder.union(subResults.get(x), subResults.get(y));
                }
            }
        }

        Collection<Set<Jtdl.SubResult>> equivClasses = unionFinder.allNonTrivialEquivalenceClasses();
        //Averge clusters > 1
        for(Set<Jtdl.SubResult> partition : equivClasses ) {
            float x1 = 0.0f;
            float y1 = 0.0f;
            float x2 = 0.0f;
            float y2 = 0.0f;
            float fern = 0.0f;
            double conf = 0.0;
            for( Jtdl.SubResult subResult : partition ) {
                x1 += subResult.boundingBox.x1;
                y1 += subResult.boundingBox.y1;
                x2 += subResult.boundingBox.x2;
                y2 += subResult.boundingBox.y2;
                fern += subResult.fernValue;
                conf += subResult.similarity;
            }
            new BoundingBox( x1/partition.size(),
                             y1/partition.size(),
                             x2/partition.size(),
                             y2/partition.size() );
            result.add( new Jtdl.SubResult( fern/partition.size(),
                                            conf/partition.size(),
                                            new BoundingBox(x1/partition.size(),
                                                           y1/partition.size(),
                                                           x2/partition.size(),
                                                           y2/partition.size()) ) );

        }

        //Add single clusters
        for(Jtdl.SubResult subResult : subResults ) {
            if( unionFinder.equivalenceClass(subResult).size() == 1 ) {
                result.add(subResult);
            }
        }

        return( result );
    }
}
