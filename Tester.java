/*
 * Throwaway test harness for Lab 2 (Autocomplete).
 * NOT part of the submission. Delete Tester.java and Tester.class before you submit.
 *
 * HOW TO RUN (from the project folder):
 *     javac *.java
 *     java Tester
 *
 * What passing looks like at each stage:
 *   - After Step 1 (Term.java):               sections 1a-1e pass; sections 2 and 3 fail.
 *   - After Step 2 (RangeBinarySearch.java):  sections 1 and 2 pass; section 3 still fails.
 *   - After Step 3 (Autocomplete.java):       everything passes.
 *   - Then verify with the CLI checks from the guide (Step 4).
 *
 * Every check prints PASS or FAIL with the expected and actual value, so a failure
 * points straight at the method that is wrong.
 */

import java.util.Arrays;
import java.util.Comparator;

public class Tester {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Lab 2 test harness. Sections fail until the matching methods exist:");
        System.out.println("  1b-1e until the Term comparators, 2 until RangeBinarySearch, 3 until Autocomplete.");

        // ------------------------------------------------------------------
        // Part 1: Term
        // ------------------------------------------------------------------

        section("1a. Term.getPrefix", () -> {
            checkEquals("getPrefix(3) of 'camera'", "cam", new Term("camera", 5).getPrefix(3));
            checkEquals("getPrefix(3) of 'cat'", "cat", new Term("cat", 5).getPrefix(3));
            checkEquals("getPrefix(3) of 'a'", "a", new Term("a", 5).getPrefix(3));
            checkEquals("getPrefix(0) of 'cat'", "", new Term("cat", 5).getPrefix(0));
        });

        section("1b. Term.byLexicographicOrder", () -> {
            Comparator<Term> c = Term.byLexicographicOrder();
            Term apple = new Term("apple", 1);
            Term banana = new Term("banana", 2);
            Term zebra = new Term("zebra", 3);
            Term yak = new Term("Yak", 4);
            Term upperApple = new Term("Apple", 5);

            isTrue("'apple' before 'banana'", c.compare(apple, banana) < 0);
            isTrue("'banana' after 'apple'", c.compare(banana, apple) > 0);
            isTrue("'Apple' equals 'apple' (case-insensitive)", c.compare(upperApple, apple) == 0);
            isTrue("'zebra' after 'Yak'", c.compare(zebra, yak) > 0);
            isTrue("'Yak' before 'zebra'", c.compare(yak, zebra) < 0);
        });

        section("1c. Term.byReverseWeightOrder", () -> {
            Comparator<Term> c = Term.byReverseWeightOrder();
            Term heavy = new Term("a", 100);
            Term light = new Term("b", 20);
            Term sameA = new Term("x", 50);
            Term sameB = new Term("y", 50);
            Term huge = new Term("big", 5000000000L);
            Term small = new Term("small", 1000000000L);

            isTrue("weight 100 comes before weight 20", c.compare(heavy, light) < 0);
            isTrue("weight 20 comes after weight 100", c.compare(light, heavy) > 0);
            isTrue("equal weights compare equal", c.compare(sameA, sameB) == 0);
            isTrue("huge weights: 5e9 before 1e9 (catches int overflow)", c.compare(huge, small) < 0);
        });

        section("1d. Term.byPrefixOrder", () -> {
            Term kampala = new Term("Kampala", 1);
            Term kansas = new Term("Kansas", 2);
            Term london = new Term("London", 3);
            Term kampalaLower = new Term("kampala", 4);

            Comparator<Term> c2 = Term.byPrefixOrder(2);
            isTrue("k=2: 'Kampala' equals 'Kansas'", c2.compare(kampala, kansas) == 0);
            isTrue("k=2: 'Kampala' equals 'kampala' (case-insensitive)", c2.compare(kampala, kampalaLower) == 0);
            isTrue("k=2: 'Kampala' before 'London'", c2.compare(kampala, london) < 0);
            isTrue("k=2: 'London' after 'Kampala'", c2.compare(london, kampala) > 0);

            Comparator<Term> c3 = Term.byPrefixOrder(3);
            isTrue("k=3: 'Kam...' before 'Kan...'",
                   c3.compare(new Term("Kampala", 0), new Term("Kansas", 0)) < 0);

            Comparator<Term> c5 = Term.byPrefixOrder(5);
            isTrue("k=5 on short words: 'a' before 'ab' (no crash)",
                   c5.compare(new Term("a", 0), new Term("ab", 0)) < 0);
            isTrue("k=5 on short words: 'ab' equals 'ab'",
                   c5.compare(new Term("ab", 0), new Term("ab", 0)) == 0);
        });

        section("1e. comparator contract (signs must be opposite, or both zero)", () -> {
            Term a = new Term("Anders", 700);
            Term b = new Term("andra", 7659);
            contractCheck("byLexicographicOrder()", Term.byLexicographicOrder(), a, b);
            contractCheck("byReverseWeightOrder()", Term.byReverseWeightOrder(), a, b);
            contractCheck("byPrefixOrder(2)", Term.byPrefixOrder(2), a, b);
            contractCheck("byPrefixOrder(6)", Term.byPrefixOrder(6), a, b);
        });

        // ------------------------------------------------------------------
        // Part 2: RangeBinarySearch
        // Note: these check correctness and the boundaries. The "no linear scan after
        // a match" performance rule cannot be tested from outside; review the code.
        // ------------------------------------------------------------------

        section("2a. exact example: [apple, banana, banana, banana, carrot]", () -> {
            Term[] a = terms("apple", "banana", "banana", "banana", "carrot");
            Comparator<Term> c = Term.byLexicographicOrder();
            Term key = new Term("banana", 0);

            checkEquals("firstIndexOf(banana)", 1, RangeBinarySearch.firstIndexOf(a, key, c));
            checkEquals("lastIndexOf(banana)", 3, RangeBinarySearch.lastIndexOf(a, key, c));
        });

        section("2b. boundary cases (empty, single, first, last, all, none)", () -> {
            Comparator<Term> c = Term.byLexicographicOrder();

            Term[] empty = new Term[0];
            checkEquals("empty array: firstIndexOf", -1,
                        RangeBinarySearch.firstIndexOf(empty, new Term("x", 0), c));
            checkEquals("empty array: lastIndexOf", -1,
                        RangeBinarySearch.lastIndexOf(empty, new Term("x", 0), c));

            Term[] one = terms("banana");
            checkEquals("one element, present: firstIndexOf", 0,
                        RangeBinarySearch.firstIndexOf(one, new Term("banana", 0), c));
            checkEquals("one element, present: lastIndexOf", 0,
                        RangeBinarySearch.lastIndexOf(one, new Term("banana", 0), c));
            checkEquals("one element, absent: firstIndexOf", -1,
                        RangeBinarySearch.firstIndexOf(one, new Term("apple", 0), c));
            checkEquals("one element, absent: lastIndexOf", -1,
                        RangeBinarySearch.lastIndexOf(one, new Term("apple", 0), c));

            Term[] atStart = terms("banana", "banana", "carrot");
            checkEquals("match at index 0: firstIndexOf must not go below 0", 0,
                        RangeBinarySearch.firstIndexOf(atStart, new Term("banana", 0), c));
            checkEquals("match at index 0: lastIndexOf", 1,
                        RangeBinarySearch.lastIndexOf(atStart, new Term("banana", 0), c));

            Term[] atEnd = terms("apple", "banana", "banana");
            checkEquals("match at last index: firstIndexOf", 1,
                        RangeBinarySearch.firstIndexOf(atEnd, new Term("banana", 0), c));
            checkEquals("match at last index: lastIndexOf must not go past the end", 2,
                        RangeBinarySearch.lastIndexOf(atEnd, new Term("banana", 0), c));

            Term[] allSame = terms("banana", "banana", "banana");
            checkEquals("all elements match: firstIndexOf", 0,
                        RangeBinarySearch.firstIndexOf(allSame, new Term("banana", 0), c));
            checkEquals("all elements match: lastIndexOf", 2,
                        RangeBinarySearch.lastIndexOf(allSame, new Term("banana", 0), c));

            Term[] none = terms("apple", "banana", "carrot");
            checkEquals("key absent: firstIndexOf", -1,
                        RangeBinarySearch.firstIndexOf(none, new Term("zebra", 0), c));
            checkEquals("key absent: lastIndexOf", -1,
                        RangeBinarySearch.lastIndexOf(none, new Term("zebra", 0), c));
            checkEquals("key smaller than all: firstIndexOf", -1,
                        RangeBinarySearch.firstIndexOf(none, new Term("aardvark", 0), c));
            checkEquals("key larger than all: lastIndexOf", -1,
                        RangeBinarySearch.lastIndexOf(none, new Term("zzz", 0), c));
        });

        section("2c. prefix range on the small dictionary (uses Term.byPrefixOrder)", () -> {
            // The small-dictionary words in the order they sit after the alphabetical sort.
            Term[] dict = terms("apple", "application", "apply", "banana", "band",
                                "camera", "candle", "car", "cat");

            checkEquals("prefix 'ca': firstIndexOf", 5,
                        RangeBinarySearch.firstIndexOf(dict, new Term("ca", 0), Term.byPrefixOrder(2)));
            checkEquals("prefix 'ca': lastIndexOf", 8,
                        RangeBinarySearch.lastIndexOf(dict, new Term("ca", 0), Term.byPrefixOrder(2)));
            checkEquals("prefix 'ba': firstIndexOf", 3,
                        RangeBinarySearch.firstIndexOf(dict, new Term("ba", 0), Term.byPrefixOrder(2)));
            checkEquals("prefix 'ba': lastIndexOf", 4,
                        RangeBinarySearch.lastIndexOf(dict, new Term("ba", 0), Term.byPrefixOrder(2)));
            checkEquals("prefix 'z': firstIndexOf", -1,
                        RangeBinarySearch.firstIndexOf(dict, new Term("z", 0), Term.byPrefixOrder(1)));
            checkEquals("prefix 'z': lastIndexOf", -1,
                        RangeBinarySearch.lastIndexOf(dict, new Term("z", 0), Term.byPrefixOrder(1)));
            checkEquals("prefix 'catx' longer than the word 'cat': firstIndexOf", -1,
                        RangeBinarySearch.firstIndexOf(dict, new Term("catx", 0), Term.byPrefixOrder(4)));
            checkEquals("prefix 'catx' longer than the word 'cat': lastIndexOf", -1,
                        RangeBinarySearch.lastIndexOf(dict, new Term("catx", 0), Term.byPrefixOrder(4)));
        });

        section("2d. randomized cross-check against a linear scan", () -> {
            Comparator<Term> c = Term.byLexicographicOrder();
            java.util.Random rnd = new java.util.Random(42);   // fixed seed: same run every time
            int checked = 0;
            String bad = null;

            for (int trial = 0; trial < 500 && bad == null; trial++) {
                int n = rnd.nextInt(31);                       // 0 to 30 elements
                Term[] a = new Term[n];
                for (int i = 0; i < n; i++) {
                    a[i] = new Term(randomWord(rnd), 1);
                }
                Arrays.sort(a, c);

                Term key = new Term(randomWord(rnd), 1);
                int expectedFirst = linearFirst(a, key, c);
                int expectedLast = linearLast(a, key, c);
                int actualFirst = RangeBinarySearch.firstIndexOf(a, key, c);
                int actualLast = RangeBinarySearch.lastIndexOf(a, key, c);
                checked++;

                if (actualFirst != expectedFirst || actualLast != expectedLast) {
                    bad = "n=" + n + ", key='" + key.getWord() + "', expected first/last "
                        + expectedFirst + "/" + expectedLast + ", got "
                        + actualFirst + "/" + actualLast;
                }
            }

            isTrue("500 random arrays agree with a linear scan (" + checked + " checked"
                   + (bad == null ? "" : ", first mismatch: " + bad) + ")", bad == null);
        });

        // ------------------------------------------------------------------
        // Part 3: Autocomplete (bonus: only passes after Step 3)
        // ------------------------------------------------------------------

        section("3. Autocomplete on the small dictionary (bonus, after Step 3)", () -> {
            Autocomplete ac = new Autocomplete(smallDictionary());

            checkEquals("numberOfMatches('ca')", 4, ac.numberOfMatches("ca"));
            checkEquals("numberOfMatches('CA') is case-insensitive", 4, ac.numberOfMatches("CA"));
            checkEquals("numberOfMatches('z')", 0, ac.numberOfMatches("z"));
            checkEquals("numberOfMatches('ba') after other queries", 2, ac.numberOfMatches("ba"));

            checkWords("allMatches('ca') order", "car, camera, candle, cat", ac.allMatches("ca"));
            checkWords("allMatches('z') should be empty", "", ac.allMatches("z"));
            checkWords("allMatches('ba') order", "banana, band", ac.allMatches("ba"));
        });

        System.out.println();
        System.out.println("--------------------------------------------------");
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        if (failed == 0) {
            System.out.println("All checks passed. Next: the CLI checks in the guide (Step 4).");
        } else {
            System.out.println("See the FAIL lines above: each shows expected vs actual.");
            System.out.println("Sections 2 and 3 fail until Steps 2 and 3 are done.");
        }
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    // Builds an array of terms. The words must be passed already in the order
    // they should sit in the array (this whole file assumes arrays are sorted).
    private static Term[] terms(String... words) {
        Term[] a = new Term[words.length];
        for (int i = 0; i < words.length; i++) {
            a[i] = new Term(words[i], i + 1);
        }
        return a;
    }

    // The words from small-dictionary.txt, in the order the CLI reads them.
    private static Term[] smallDictionary() {
        return new Term[] {
            new Term("apple", 100),
            new Term("application", 80),
            new Term("apply", 70),
            new Term("banana", 200),
            new Term("band", 50),
            new Term("car", 90),
            new Term("camera", 60),
            new Term("candle", 40),
            new Term("cat", 30),
        };
    }

    // 0 to 3 letters from a..d, so duplicates and tight boundaries are frequent.
    private static String randomWord(java.util.Random rnd) {
        int len = rnd.nextInt(4);
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < len; j++) {
            sb.append((char) ('a' + rnd.nextInt(4)));
        }
        return sb.toString();
    }

    private static int linearFirst(Term[] a, Term key, Comparator<Term> c) {
        for (int i = 0; i < a.length; i++) {
            if (c.compare(a[i], key) == 0) return i;
        }
        return -1;
    }

    private static int linearLast(Term[] a, Term key, Comparator<Term> c) {
        for (int i = a.length - 1; i >= 0; i--) {
            if (c.compare(a[i], key) == 0) return i;
        }
        return -1;
    }

    private static void contractCheck(String name, Comparator<Term> c, Term a, Term b) {
        int ab = c.compare(a, b);
        int ba = c.compare(b, a);
        boolean ok = (ab == 0 && ba == 0) || (ab < 0 && ba > 0) || (ab > 0 && ba < 0);
        pass(name + " [compare(a,b)=" + ab + ", compare(b,a)=" + ba + "]", ok,
             "signs must be opposite, or both zero");
    }

    private static void section(String name, Runnable body) {
        System.out.println();
        System.out.println("== " + name + " ==");
        try {
            body.run();
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL (crashed before finishing this section: " + t + ")");
        }
    }

    private static void checkEquals(String name, Object expected, Object actual) {
        boolean ok = (expected == null) ? (actual == null) : expected.equals(actual);
        pass(name, ok, "expected <" + expected + ">, got <" + actual + ">");
    }

    private static void checkEquals(String name, int expected, int actual) {
        pass(name, expected == actual, "expected " + expected + ", got " + actual);
    }

    private static void isTrue(String name, boolean ok) {
        pass(name, ok, "expected true");
    }

    private static void checkWords(String name, String expectedCsv, Term[] actual) {
        if (actual == null) {
            pass(name, false, "returned null");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Term t : actual) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.getWord());
        }
        pass(name, expectedCsv.equals(sb.toString()),
             "expected <" + expectedCsv + ">, got <" + sb + ">");
    }

    private static void pass(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + "  (" + detail + ")");
        }
    }
}
