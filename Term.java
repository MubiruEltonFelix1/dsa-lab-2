import java.util.Comparator;

public class Term {
    private String word;
    private long weight;

    // Initializes a term with a given word and weight.
    public Term(String word, long weight) {
        if (word == null) {
            throw new IllegalArgumentException("word must not be null");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weight must be nonnegative");
        }
        this.word = word;
        this.weight = weight;
    }

    // Gets the word.
    public String getWord() {
        return word;
    }

    // Gets the weight.
    public long getWeight() {
        return weight;
    }

    // Extracts a prefix from the word.
    public String getPrefix(int len) {
        if (len < 0) {
            throw new IllegalArgumentException("prefix length must be nonnegative");
        }
        return word.substring(0, Math.min(len, word.length()));
    }

    // Compares the two terms in case-insensitive lexicographic order.
    public static Comparator<Term> byLexicographicOrder() {
        return new Comparator<Term>() {
            @Override
            public int compare(Term first, Term second) {
                return first.word.compareToIgnoreCase(second.word);
            }
        };
    }

    // Compares the two terms in descending order by weight.
    public static Comparator<Term> byReverseWeightOrder() {
        return new Comparator<Term>() {
            @Override
            public int compare(Term first, Term second) {
                return Long.compare(second.weight, first.weight);
            }
        };
    }

    // Compares the two terms in case-insensitive lexicographic order,
    // but using only the first k characters of each word.
    public static Comparator<Term> byPrefixOrder(final int k) {
        if (k < 0) {
            throw new IllegalArgumentException("prefix length must be nonnegative");
        }
        return new Comparator<Term>() {
            @Override
            public int compare(Term first, Term second) {
                return first.getPrefix(k).compareToIgnoreCase(second.getPrefix(k));
            }
        };
    }

    // Returns a string representation of this term in the following format:
    // the weight, followed by whitespace, followed by the word.
    public String toString() {
        return String.format("%12d    %s", this.getWeight(), this.getWord());
    }
}