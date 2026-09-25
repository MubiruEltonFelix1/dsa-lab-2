# How to Run, Implement, Test, and Complete the `dsa-lab-2` Autocomplete Lab

## Purpose of this guide

This is a practical workflow for completing the repository from the starter code. It assumes you have read the mental-model document or understand the basic idea: sort the dictionary alphabetically, find the prefix range with boundary binary searches, and rank matches by weight.

The repository is an assessed lab and its README says that submitted code must be the work of the student group. Therefore, this guide gives you the execution plan, pseudocode, invariants, tests, debugging method, and submission checklist. You should write the final Java implementation yourself rather than copying a submission-ready answer.

Repository: [MubiruEltonFelix1/dsa-lab-2](https://github.com/MubiruEltonFelix1/dsa-lab-2)

---

## 1. What you are expected to complete

The starter repository contains TODO methods in three core files.

### `Term.java`

You need to complete:

```java
public String getPrefix(int len)
public static Comparator<Term> byLexicographicOrder()
public static Comparator<Term> byReverseWeightOrder()
public static Comparator<Term> byPrefixOrder(int k)
```

### `RangeBinarySearch.java`

You need to complete:

```java
public static int firstIndexOf(
    Term[] terms, Term key, Comparator<Term> comparator)

public static int lastIndexOf(
    Term[] terms, Term key, Comparator<Term> comparator)
```

### `Autocomplete.java`

You need to complete:

```java
private void sortDictionary()
public Term[] allMatches(String prefix)
public int numberOfMatches(String prefix)
```

### `answers.txt`

You need to answer the written questions about:

- how `firstIndexOf` works;
- the complexity of each `Autocomplete` operation;
- time spent, bugs, collaboration, problems, and comments.

The CLI and GUI are provided clients. They are useful for testing, but the core submission is the three implementation files plus `answers.txt`. [1]

---

## 2. Prepare your Java environment

You need a Java Development Kit, not only a Java Runtime Environment. The important commands are:

```bash
java -version
javac -version
```

If both commands print version information, your environment is ready.

On Ubuntu or Debian-based Linux, install a JDK if necessary:

```bash
sudo apt update
sudo apt install default-jdk
```

On Windows or macOS, install a current JDK from a trusted distribution, then reopen your terminal and verify both commands again.

The sandbox used to inspect this repository did not have `javac` installed, so a local machine with a JDK may be required for actual compilation.

---

## 3. Get the repository and enter its directory

Using Git:

```bash
git clone https://github.com/MubiruEltonFelix1/dsa-lab-2.git
cd dsa-lab-2
```

If you downloaded a ZIP file, unzip it and enter the extracted folder:

```bash
cd path/to/dsa-lab-2
```

Confirm that the expected files exist:

```bash
ls
find dictionaries -maxdepth 1 -type f | sort
```

You should see files including:

```text
Autocomplete.java
AutocompleteCLI.java
AutocompleteGUI.java
RangeBinarySearch.java
Term.java
answers.txt
dictionaries/
```

Create a backup branch before editing:

```bash
git checkout -b complete-autocomplete-lab
git status
```

The status should initially show a clean working tree.

---

## 4. Understand how the starter code fails

Before implementing anything, compile the starter code:

```bash
javac AutocompleteCLI.java
```

Java compiles referenced source files automatically when they are in the same directory. You can also compile all source files explicitly:

```bash
javac *.java
```

Run the CLI with one of the supplied dictionaries:

```bash
java AutocompleteCLI dictionaries/romaner.txt 5
```

When prompted, type a prefix such as:

```text
and
```

The starter code may print a match count of zero and then fail because unfinished methods return placeholder values such as `null` or `0`. That failure is expected. It tells you that the CLI is wired to the API, but the API has not been implemented.

Do not begin debugging `AutocompleteCLI.java`. The missing behavior belongs in the TODO methods.

---

## 5. Implement in the safest order

Implement one conceptual layer at a time:

```text
Term
  -> RangeBinarySearch
      -> Autocomplete
          -> CLI and GUI testing
```

This order matters. `Autocomplete` depends on both the `Term` comparators and the boundary searches. If you implement the top-level class first, errors become difficult to locate.

---

## 6. Implement `Term.java`

### 6.1 `getPrefix(int len)`

Required behavior:

```text
word       len     result
"camera"   3       "cam"
"cat"      3       "cat"
"a"        3       "a"
""          3       ""
```

The important edge case is a word shorter than `len`. Use the smaller of the requested length and the word length. Also decide how you want to handle invalid negative lengths according to the assignment’s expected behavior; do not silently create an unrelated behavior.

Test it with a small temporary Java program or a unit-test-style class before moving on.

### 6.2 `byLexicographicOrder()`

Create and return a comparator that compares the `word` field case-insensitively.

Check the following expected relationships:

```text
"apple" comes before "banana"
"Apple" compares equal to "apple"
"zebra" comes after "Yak"
```

Do not compare the weights in this comparator. Its only job is alphabetical word order.

### 6.3 `byReverseWeightOrder()`

Create and return a comparator that places larger weights first.

Check:

```text
weight 100 comes before weight 20
weight 20 comes after weight 100
weight 50 compares equal to weight 50
```

Use a safe comparison for `long` values. Avoid subtracting two weights and converting the result to `int`, because large values can overflow or lose information.

### 6.4 `byPrefixOrder(int k)`

Create a comparator that:

1. extracts the first `k` characters from each word;
2. compares those extracted prefixes without case sensitivity;
3. ignores the weights.

For `k = 2`, you should expect:

```text
"Kampala" and "Kansas" compare equal
"Kampala" comes before "London"
"London" comes after "Kampala"
```

Think carefully about a word shorter than `k`. Its available characters are its entire prefix. Avoid creating an invalid substring.

### 6.5 Comparator sanity test

For every comparator, test the three-way behavior:

```text
compare(a, b) < 0
compare(a, a) == 0
compare(b, a) > 0
```

The Java comparator contract requires consistent ordering. A comparator that sometimes reverses its answer can make both sorting and binary search unreliable. [2]

---

## 7. Implement `RangeBinarySearch.java`

Start with a small sorted array of terms and a simple comparator. Do not use the full dictionaries yet.

### 7.1 First-index pseudocode

Write the method yourself from this logic:

```text
low = 0
high = length - 1
answer = -1

while low <= high:
    mid = low + (high - low) / 2
    comparison = comparator.compare(terms[mid], key)

    if comparison < 0:
        low = mid + 1
    else if comparison > 0:
        high = mid - 1
    else:
        answer = mid
        high = mid - 1

return answer
```

The key line is the final branch. When a match is found, do not stop. Save the match and continue to the left.

### 7.2 Last-index pseudocode

The second method is almost the mirror image:

```text
low = 0
high = length - 1
answer = -1

while low <= high:
    mid = low + (high - low) / 2
    comparison = comparator.compare(terms[mid], key)

    if comparison < 0:
        low = mid + 1
    else if comparison > 0:
        high = mid - 1
    else:
        answer = mid
        low = mid + 1

return answer
```

When a match is found, save it and continue to the right.

### 7.3 Test cases for both methods

Use an array in which several values compare equal:

```text
apple
banana
banana
banana
carrot
```

Expected results for the key `banana`:

```text
first index = 1
last index  = 3
```

Also test:

| Case | Expected behavior |
| --- | --- |
| Empty array | Return `-1`. |
| No match | Return `-1`. |
| One match | First and last are the same index. |
| Match at index `0` | Boundary search must not move below zero. |
| Match at final index | Boundary search must not move beyond the array. |
| Every item matches | First is `0`; last is `length - 1`. |

### 7.4 How to debug a wrong boundary

Print the following inside a temporary test version:

```text
low, mid, high, comparison, answer
```

Then ask:

```text
When comparison is negative, which side is impossible?
When comparison is positive, which side is impossible?
When comparison is zero, which direction must continue for this method?
```

Remove temporary debug prints after the method is correct.

### 7.5 Complexity requirement

Each iteration removes roughly half the remaining search interval. Therefore both methods must be `O(log N)` in comparisons. Do not scan left or right after finding a match. That would violate the lab’s performance requirement.

---

## 8. Implement `Autocomplete.java`

### 8.1 `sortDictionary()`

Sort the `dictionary` array using the case-insensitive lexicographic comparator from `Term`.

The array must remain in this ordering after construction. Do not sort it by weight inside `allMatches`, because binary search relies on alphabetical order.

The intended cost is `O(N log N)`. Java’s array sorting methods accept a comparator to define the ordering. [2] [3]

### 8.2 `numberOfMatches(String prefix)`

The method should do the following:

```text
k = prefix length
key = a Term whose word is prefix
comparator = Term.byPrefixOrder(k)
first = RangeBinarySearch.firstIndexOf(dictionary, key, comparator)
last = RangeBinarySearch.lastIndexOf(dictionary, key, comparator)

if first == -1:
    return 0
else:
    return last - first + 1
```

The key details are:

- Use the prefix length when constructing the prefix comparator.
- Use the same comparator for both boundary searches.
- Return zero when no match exists.
- Do not loop through the matching terms to count them.

The required complexity is `O(log N)`.

### 8.3 `allMatches(String prefix)`

Use the same boundary logic as `numberOfMatches`.

Then:

```text
if there is no match:
    return an empty Term array

copy the inclusive range first..last into a new array
sort the new array using reverse-weight order
return the new array
```

Do not return `null` for no results. The CLI and GUI expect an array and iterate over it.

The intended complexity is:

```text
O(log N + M log M)
```

The logarithmic part finds the interval. The `M log M` part sorts the matching terms by weight.

### 8.4 Inclusive range size

If the first match is `first` and the last match is `last`, the number of terms is:

```text
last - first + 1
```

The array-copy endpoint in Java is often exclusive, so be precise when converting an inclusive last index into a copy range. An off-by-one error here causes missing results or an out-of-bounds exception.

### 8.5 Preserve the public behavior

The clients expect these behaviors:

```text
numberOfMatches("unknown") -> 0
allMatches("unknown")       -> empty array
allMatches("prefix")        -> non-null array
returned results             -> descending weight order
matching                     -> case-insensitive
```

Do not change method names, parameters, return types, or the client code unless your instructor explicitly permits it.

---

## 9. Build a small test dictionary first

Before testing large data files, create a file named `small-dictionary.txt` in the project directory:

```text
100	apple
80	application
70	apply
200	banana
50	band
90	car
60	camera
40	candle
30	cat
```

The CLI expects a weight, a tab, and a string. Run:

```bash
java AutocompleteCLI small-dictionary.txt 10
```

Try these prefixes:

```text
app
ban
ca
z
A
```

Expected counts:

```text
app -> 3
ban -> 2
ca  -> 4
z   -> 0
A   -> 3, under case-insensitive matching
```

For `ca`, the displayed order should be:

```text
car       90
camera    60
candle    40
cat       30
```

This tiny dataset makes errors visible. If you start with a dictionary containing tens or hundreds of thousands of terms, you may not know whether the problem is parsing, sorting, searching, or ranking.

---

## 10. Run the supplied dictionaries

After the small dictionary works, test the provided files.

### Swedish novel words

```bash
java AutocompleteCLI dictionaries/romaner.txt 5
```

Try:

```text
and
42
c
flaggstångsknopp
```

The README provides example counts and results. Your exact output should be consistent with those examples if the implementation is correct. [1]

### Cities

```bash
java AutocompleteCLI dictionaries/cities.txt 7
```

Try Unicode input such as:

```text
Gö
Al M
```

This checks that the program can process non-ASCII characters in the supplied data.

### Other dictionaries

```bash
java AutocompleteCLI dictionaries/wiktionary.txt 10
java AutocompleteCLI dictionaries/gp2011.txt 10
java AutocompleteCLI dictionaries/nordsamiska.txt 10
```

Use a prefix relevant to each dictionary and compare the number of results against a simple independent scan if you need to verify correctness.

---

## 11. Validate with an independent slow implementation

A reliable way to test a fast algorithm is to compare it with a deliberately simple reference algorithm.

For each prefix:

```text
scan every term
keep terms whose words begin with the prefix, ignoring case
sort the kept terms by descending weight
compare the count and sequence with Autocomplete
```

The reference algorithm can be slow because it is used only for testing. It gives you a trustworthy answer against which to compare the optimized implementation.

Test at least:

```text
""
"a"
"A"
"the"
"xyz-not-present"
"Gö"
```

The lab’s CLI uses an empty line to quit, so the interactive client does not normally ask the engine to search an empty prefix. You should still decide deliberately how your core API behaves for an empty string and test it consistently.

---

## 12. Check the required complexities

Use the lab’s variables:

```text
N = total number of dictionary terms
M = number of terms matching the prefix
```

Write these explanations in your own words in `answers.txt`:

| Method | Complexity | Explanation |
| --- | ---: | --- |
| `sortDictionary()` | `O(N log N)` | The full dictionary is sorted once. |
| `allMatches()` | `O(log N + M log M)` | Two boundary searches locate the range, then `M` matches are sorted by weight. |
| `numberOfMatches()` | `O(log N)` | Two boundary searches are performed; the range size is calculated directly. |

The lab counts comparator calls as the unit of comparison, so do not describe `numberOfMatches` as `O(M)`. It must not inspect each matching term.

---

## 13. Common mistakes and their symptoms

### Mistake: using case-sensitive comparison

**Symptom:** Searching for `Gö` or `A` produces fewer results than expected.

**Cause:** Using `compareTo` instead of the required case-insensitive comparison.

**Fix:** Ensure all relevant word comparisons use the case-insensitive rule.

### Mistake: stopping after the first binary-search match

**Symptom:** A prefix count is too small or changes unpredictably.

**Cause:** Returning immediately when `comparison == 0`.

**Fix:** Record the match and continue left for the first-index method or right for the last-index method.

### Mistake: sorting the dictionary by weight

**Symptom:** Ranking may look correct, but later prefix searches fail.

**Cause:** The main dictionary no longer has alphabetical order.

**Fix:** Keep the dictionary alphabetical. Sort only a copied matching range by weight.

### Mistake: returning `null` for no matches

**Symptom:** The CLI or GUI throws `NullPointerException`.

**Cause:** The clients expect a non-null array.

**Fix:** Return an empty `Term[]`.

### Mistake: copying the range incorrectly

**Symptom:** The first or last match is missing, or an exception occurs.

**Cause:** Confusing inclusive and exclusive endpoints.

**Fix:** Write down the first and last index and calculate the length explicitly.

### Mistake: unsafe weight subtraction

**Symptom:** Very large weights sort incorrectly.

**Cause:** Subtracting `long` values and narrowing to `int`.

**Fix:** Use a safe long comparison or explicit greater-than/less-than logic.

### Mistake: scanning all matches in `numberOfMatches`

**Symptom:** The answer is functionally correct but violates the performance requirement.

**Cause:** Counting by looping over the dictionary or matching interval.

**Fix:** Use `last - first + 1`.

### Mistake: changing the client instead of fixing the API

**Symptom:** Your custom demo works, but the provided CLI or GUI fails.

**Cause:** The implementation no longer matches the required method contracts.

**Fix:** Keep the given public API unchanged.

---

## 14. A disciplined debugging sequence

When a test fails, isolate the layer.

### If a term test fails

Inspect only:

```text
getPrefix
byLexicographicOrder
byReverseWeightOrder
byPrefixOrder
```

### If a boundary test fails

Use a tiny sorted array and print:

```text
low, mid, high, comparison, answer
```

### If counts are wrong but boundaries are correct

Inspect:

```text
prefix length
prefix comparator
last - first + 1
```

### If counts are correct but order is wrong

Inspect:

```text
range copy
reverse-weight comparator
sorting of the copied range
```

### If the CLI throws an exception

Check whether:

```text
allMatches returned null
an array contains a null element
file parsing failed
the command-line arguments are missing
```

Do not change several layers at the same time. Make one change, rerun one focused test, and record what changed.

---

## 15. Optional GUI execution

The GUI is optional but useful after the CLI works.

Compile and run:

```bash
javac AutocompleteGUI.java
java AutocompleteGUI dictionaries/gp2011.txt 10
```

On a machine without a graphical desktop, the GUI may fail with a display-related error. That does not necessarily indicate a problem with the autocomplete engine. Use the CLI for algorithm verification.

In the GUI, test:

- typing one character at a time;
- selecting a suggestion with the mouse;
- selecting with the keyboard;
- toggling weight display;
- entering Unicode text;
- entering a prefix with no results.

The GUI calls the engine repeatedly as the text changes. Therefore, a null result or malformed array is immediately visible.

---

## 16. Complete `answers.txt` honestly

The answer file asks about the algorithm, complexity, and your group’s experience.

### Question 1: explain `firstIndexOf`

Your answer should mention:

1. The search interval begins at the entire array.
2. The middle element is compared with the key.
3. A smaller middle value means search right.
4. A larger middle value means search left.
5. On equality, store the index and continue left.
6. The stored index is the first match after the interval is exhausted.
7. Return `-1` if no equality was found.

Describe the logic in your own words and refer to the invariant, not only the final code.

### Question 2: complexity

State the three complexities and explain the role of `N` and `M`.

### Appendix questions

Answer the group-experience questions truthfully:

- approximate hours spent;
- known bugs or limitations;
- collaborators and resources;
- serious problems encountered;
- other comments.

Do not leave placeholders such as `[...]` or `[..hours..]` in the submitted file.

---

## 17. Final validation checklist

Before considering the lab complete, run through this checklist.

### Code behavior

- [ ] `Term` stores the word and weight correctly.
- [ ] `getPrefix` handles short words safely.
- [ ] Alphabetical comparison is case-insensitive.
- [ ] Weight comparison is descending and safe for `long` values.
- [ ] Prefix comparison uses exactly the requested prefix length.
- [ ] First-index binary search returns the leftmost match.
- [ ] Last-index binary search returns the rightmost match.
- [ ] No-match searches return `-1` from the boundary methods.
- [ ] The dictionary is sorted alphabetically once.
- [ ] `numberOfMatches` returns zero when appropriate.
- [ ] `numberOfMatches` uses boundary indices rather than scanning matches.
- [ ] `allMatches` returns an empty array rather than `null`.
- [ ] `allMatches` returns results in descending weight order.
- [ ] Returned arrays contain no null entries.

### Testing

- [ ] Tiny custom dictionary tested.
- [ ] Empty result tested.
- [ ] One result tested.
- [ ] Multiple equal-prefix results tested.
- [ ] Prefix at beginning of dictionary tested.
- [ ] Prefix at end of dictionary tested.
- [ ] Case-insensitive input tested.
- [ ] Unicode input tested.
- [ ] Supplied dictionaries tested.
- [ ] CLI tested from a clean terminal.
- [ ] GUI tested if a graphical environment is available.

### Submission quality

- [ ] `answers.txt` has no placeholders.
- [ ] Complexity explanations use `N` and `M` correctly.
- [ ] Temporary debug prints and scratch files are removed.
- [ ] Only intended files are modified.
- [ ] Code is formatted and readable.
- [ ] `git diff` has been reviewed.
- [ ] The final program was run after the last change.

Review the changed files:

```bash
git status
git diff -- Term.java RangeBinarySearch.java Autocomplete.java answers.txt
```

Compile once more:

```bash
rm -f *.class
javac *.java
```

Run a final CLI smoke test:

```bash
java AutocompleteCLI dictionaries/romaner.txt 5
```

If your course requires a ZIP file, create it only after validation:

```bash
zip dsa-lab-2-submission.zip Term.java RangeBinarySearch.java Autocomplete.java answers.txt
```

Open the ZIP or list its contents to verify that it contains exactly what your instructor requested.

---

## 18. What “passing the lab” really means

Passing is not just making the CLI print plausible suggestions. A correct submission must satisfy four dimensions:

1. **Functional correctness:** prefixes, counts, ordering, case handling, and empty results work.
2. **Algorithmic correctness:** first and last boundary searches are genuinely logarithmic.
3. **API correctness:** method signatures and return contracts match the supplied clients.
4. **Explanation quality:** `answers.txt` accurately explains the algorithm and complexity.

If you focus only on output examples, you may accidentally write a linear scan that appears correct but fails the performance requirements. If you focus only on complexity, you may miss empty arrays, Unicode, or off-by-one errors. Test behavior and inspect the algorithm together.

## References

[1]: https://github.com/MubiruEltonFelix1/dsa-lab-2 "MubiruEltonFelix1/dsa-lab-2 GitHub repository and README"

[2]: https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html "Java Platform SE 8 Comparator documentation"

[3]: https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html "Java Platform SE 8 Arrays documentation"
