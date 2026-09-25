import java.util.Arrays;

public class Autocomplete {
    private Term[] dictionary;

    // Initializes the dictionary from the given array of terms.
    public Autocomplete(Term[] dictionary) {
        if (dictionary == null) {
            throw new IllegalArgumentException("dictionary must not be null");
        }
        this.dictionary = dictionary.clone();
        sortDictionary();
    }

    // Sorts the dictionary in *case-insensitive* lexicographic order.
    // Complexity: O(N log N), where N is the number of terms
    private void sortDictionary() {
        Arrays.sort(dictionary, Term.byLexicographicOrder());
    }

    // Returns all terms that start with the given prefix, in descending order of weight.
    // Complexity: O(log N + M log M), where M is the number of matching terms
    public Term[] allMatches(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix must not be null");
        }

        int first = RangeBinarySearch.firstIndexOf(
                dictionary,
                new Term(prefix, 0),
                Term.byPrefixOrder(prefix.length()));
        if (first == -1) {
            return new Term[0];
        }

        int last = RangeBinarySearch.lastIndexOf(
                dictionary,
                new Term(prefix, 0),
                Term.byPrefixOrder(prefix.length()));
        Term[] matches = Arrays.copyOfRange(dictionary, first, last + 1);
        Arrays.sort(matches, Term.byReverseWeightOrder());
        return matches;
    }

    // Returns the number of terms that start with the given prefix.
    // Complexity: O(log N)
    public int numberOfMatches(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix must not be null");
        }

        Term key = new Term(prefix, 0);
        int first = RangeBinarySearch.firstIndexOf(
                dictionary, key, Term.byPrefixOrder(prefix.length()));
        if (first == -1) {
            return 0;
        }

        int last = RangeBinarySearch.lastIndexOf(
                dictionary, key, Term.byPrefixOrder(prefix.length()));
        return last - first + 1;
    }
}