package org.neo4j.cache;

import java.util.HashSet;
import java.util.Set;


public class Generator
{
    // 63 bit - 64th is sign, not used
    private static final int BitsToCount = Long.SIZE - 1;

    private final int[] counts;

    private static final Generator Instance = new Generator();

    public static Generator getInstance()
    {
        return Instance;
    }

    private Generator()
    {
        counts = new int[BitsToCount];
    }

    // TODO : Heavily optimize this, called often
    void updateCounts( long forLong )
    {
        long mask = 1;
        for ( int i = 0; i < BitsToCount; i++ )
        {
            // This should be count += [(bit value)*2 - 1] - lose the if, bad if
            if ( ( forLong & ( mask << i ) ) != 0 )
            { // Increase for 1s
                counts[i]++;
            }
            else
            { // Decrease for 0s
                counts[i]--;
            }
        }
    }

    public int[] getOrder()
    {
        return discoverOrder();
    }

    /**
     * Recreates the order array by doing an insertion sort and keeping track
     * of where each element is left. Comparison is done with absolute values,
     * so -6 will be regarded as "larger than" 2, as will 3, of course.
     *
     * After this is done, order[i] contains the position
     * of the i-th order element in counts[]. This means the element with the
     * least absolute value is counts[order[0]], the next is counts[order[1]]
     * etc. This way, the m least significant bits can be found - they are
     * order[0] to order[m-1].
     *
     * This is the most expensive operation here - use sparingly.
     */
    private int[] discoverOrder()
    {
        // TODO: Do this in a smarter manner - use less memory but more
        // importantly, less moves. Insertion sort seems to fit nicely here
        // though

        /*
         *  We do not want to mess with the actual counts, make copy.
         *  Also, we want absolute values, so do that here.
         */
        int[] order = new int[counts.length];
        int[] temp = new int[counts.length];
        int currentMask;
        for ( int i = 0; i < counts.length; i++ )
        {
            order[i] = i;
            /*
             * The next two lines take the absolute value without an if - great
             * for CPU optimizations.
             * The mask is all one for negative, all zero for positive numbers.
             * Adding that to the value subtracts 1 if negative, 0 if positive.
             * Then, if negative, flips all bits, if positive leaves as is. This
             * ends up being the reverse of negation in two's complement - voila!
             * absolute value.
             * Of course this still returns Integer.MIN_VALUE for Integer.MIN_VALUE
             * but what can you do, eh?
             */
            currentMask = counts[i] >> ( Integer.SIZE - 1 );
            temp[i] = ( counts[i] + currentMask ) ^ currentMask;
        }

        /*
         * TODO: Because of two's complement, it is possible to have negative
         * values here - especially for long running apps. We should fix this
         * by subtracting periodically the minimum value from everything.
         */

        // Insertion sort + ordering parallel array
        for ( int i = 0; i < temp.length; i++ )
        {
            int current = temp[i];
            int currentOrder = order[i];
            int j = i - 1;
            for ( ; j >= 0 && current < temp[j]; j-- )
            {
                temp[j + 1] = temp[j];
                order[j + 1] = order[j];
            }
            temp[j + 1] = current;
            order[j + 1] = currentOrder;
        }
        return order;
    }

    public static void main( String[] args )
    {
        /*
        Random r = new Random();
        Generator hash = new Generator();
        for ( int i = 0; i < 63; i++ )
        {
            hash.counts[i] = r.nextInt();
        }
        for ( int i : hash.discoverOrder() )
        {
            System.out.println( i );
        }
        */
        Set<Integer> run1 = new HashSet<Integer>();
        Set<Integer> run2 = new HashSet<Integer>();
        int conflicts1 = 0, conflicts2 = 0;
        AdaptedHasher cache = new AdaptedHasher();
        for ( int i = 0; i < 1000; i++ )
        {
            int current = cache.hashLong(i);
            if ( !run1.add( current ) ) conflicts1++;

        }
        System.out.println( "conflicts for 1 are " + conflicts1 );
        for ( int i = 1 << 11; i < 1 << 20; i += 1024 )
        {
            int current = cache.hashLong( i );
            if ( !run1.add( current ) ) conflicts1++;
        }
        System.out.println( "conflicts for 1 are " + conflicts1 );
        cache = new AdaptedHasher();
        for ( int i = 0; i < 1000; i++ )
        {
            int current = cache.hashLong( i );
            if ( !run2.add( current ) ) conflicts2++;
        }
        System.out.println( "conflicts for 2 are " + conflicts2 );
        for ( int i = 1 << 11; i < 1 << 20; i += 1024 )
        {
            int current = cache.hashLong( i );
            if ( !run2.add( current ) ) conflicts2++;
        }
        System.out.println( "conflicts for 2 are " + conflicts2 );
    }
}
