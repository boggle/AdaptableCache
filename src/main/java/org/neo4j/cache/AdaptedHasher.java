package org.neo4j.cache;

public class AdaptedHasher
{
    private final int[] order;
    private static final int BitsToKeep = 10;

    public AdaptedHasher()
    {
        order = Generator.getInstance().getOrder();
    }

    public int hashLong( long toHash )
    {
        Generator.getInstance().updateCounts( toHash );
        int result = 0;
        for ( int i = 0; i < BitsToKeep; i++ )
        {
            long mask = 1 << order[i];
            long bitValue = toHash & mask;
            bitValue = bitValue >> ( order[i] );
            bitValue = bitValue << i;
            result = (int) ( result | bitValue );
        }
        return result;
    }
}
