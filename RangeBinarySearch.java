import java.util.Comparator;

public class RangeBinarySearch {

    // Returns the index of the *first* element in terms[] that equals the search key,
    // according to the given comparator, or -1 if there are no matching elements.
    // Complexity: O(log N), where N is the length of the array
    public static int firstIndexOf(Term[] terms, Term key, Comparator<Term> comparator) {
        int low = 0;
        int high = terms.length - 1;
        int result = -1;

        while (low <= high) {
            int middle = low + (high - low) / 2;
            int comparison = comparator.compare(terms[middle], key);
            if (comparison < 0) {
                low = middle + 1;
            } else if (comparison > 0) {
                high = middle - 1;
            } else {
                result = middle;
                high = middle - 1;
            }
        }
        return result;
    }

    // Returns the index of the *last* element in terms[] that equals the search key,
    // according to the given comparator, or -1 if there are no matching elements.
    // Complexity: O(log N)
    public static int lastIndexOf(Term[] terms, Term key, Comparator<Term> comparator) {
        int low = 0;
        int high = terms.length - 1;
        int result = -1;

        while (low <= high) {
            int middle = low + (high - low) / 2;
            int comparison = comparator.compare(terms[middle], key);
            if (comparison < 0) {
                low = middle + 1;
            } else if (comparison > 0) {
                high = middle - 1;
            } else {
                result = middle;
                low = middle + 1;
            }
        }
        return result;
    }
}