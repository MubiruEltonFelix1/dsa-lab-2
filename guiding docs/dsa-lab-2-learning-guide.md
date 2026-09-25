# Understanding `dsa-lab-2`: From Binary Search to a Real Autocomplete System

## Executive summary

This repository is a small Java data-structures project about **weighted prefix autocomplete**. Given a dictionary of strings and a nonnegative weight for each string, the program accepts a prefix such as `go` and returns every dictionary entry beginning with that prefix, ordered from highest weight to lowest weight. The repository is intentionally incomplete: the three core files contain `TODO` methods, while the command-line client, optional GUI, sample dictionaries, and answer template provide the environment in which you implement and test the algorithms. [1]

The central lesson is not merely “how to make a search box.” It is how to design a fast query operation by choosing an appropriate ordering of data and then exploiting that ordering:

1. Represent each suggestion as a `Term` containing a string and a weight.
2. Sort the dictionary once by case-insensitive alphabetical order.
3. Use two boundary-finding binary searches to locate the contiguous block of terms sharing a prefix.
4. Sort only that matching block by descending weight.
5. Expose the result through a CLI or GUI.

In other words, the repository is a compact example of **preprocessing plus fast repeated queries**.

## What is in the repository?

| File or directory | Purpose | What it teaches |
| --- | --- | --- |
| `Term.java` | Models one suggestion and defines three comparison rules. | Classes, encapsulation, `Comparator`, string comparison, prefixes. |
| `RangeBinarySearch.java` | Finds the first and last occurrence of a key in a sorted array. | Binary search, lower/upper bounds, loop invariants, `O(log N)`. |
| `Autocomplete.java` | Owns the sorted dictionary and implements the public search operations. | Composition, array ranges, sorting, complexity analysis. |
| `AutocompleteCLI.java` | Reads a dictionary and repeatedly accepts prefixes from the terminal. | File parsing, streams, a read-evaluate-print loop, API use. |
| `AutocompleteGUI.java` | Provides live suggestions while the user types and can open a browser search. | Event-driven programming, Swing, UI-to-algorithm integration. |
| `dictionaries/` | Contains weighted word, city, and language datasets. | Real data, Unicode, scale, frequency/population ranking. |
| `answers.txt` | Contains questions about the algorithm and its complexity. | Explaining correctness and asymptotic performance. |

The GitHub repository has one initial commit and is a teaching starter project rather than a production application. [1]

## The mental model: one dictionary, two different orders

The most important intuition is that the same `Term` objects are viewed through different comparison rules.

### Order A: alphabetical order for finding a prefix range

Suppose the dictionary contains:

```text
algebra
algebraic
alice
alloy
banana
```

After case-insensitive alphabetical sorting, all words beginning with `al` form one contiguous interval. That means the program does not need to scan every term. It can jump into the middle of the array and repeatedly discard half of the remaining search space.

If the prefix is `al`, the goal is not to find one exact word. The goal is to find two boundaries:

```text
first index whose first 2 characters equal "al"
last index whose first 2 characters equal "al"
```

This is why ordinary “find any matching item” binary search is not sufficient. The algorithm must continue searching left after a match to find the first position and continue searching right after a match to find the last position.

### Order B: descending weight for displaying useful suggestions

The alphabetical order is useful for locating matches, but it is not the order the user wants to see. A user normally wants the most likely, popular, or important completion first. Therefore, after the matching interval is extracted, the program sorts that smaller interval by reverse weight order.

This separation is a broadly useful design pattern:

> Keep the data ordered for the operation that must be fast, then rank the small result set for presentation.

Java comparators are the mechanism that lets the same object type participate in several orderings. Oracle describes a comparator as a function that imposes an ordering and can be supplied to sorting methods and ordered data structures. [2]

## How the code should work, file by file

### 1. `Term.java`: the data model and comparison rules

A `Term` has two fields:

```java
private String word;
private long weight;
```

The constructor stores them, and the getters expose them. The `long` type matters because the input weights can be larger than the range of a 32-bit `int`.

The unfinished methods have three distinct jobs.

#### `getPrefix(int len)`

This returns the first `len` characters of the word, except that a word shorter than `len` must be handled safely. The conceptual behavior is:

```text
getPrefix(3) for "carpet" -> "car"
getPrefix(3) for "cat"    -> "cat"
getPrefix(3) for "a"      -> "a"
```

The method should not attempt to take a substring beyond the word length.

#### `byLexicographicOrder()`

This comparator orders terms by their words without considering letter case. Thus `Apple` and `apple` compare as equal under the assignment’s required rule. Java provides `compareToIgnoreCase` and `String.CASE_INSENSITIVE_ORDER` for this style of comparison. The Java documentation also warns that this simple ordering is not locale-aware, which matters for languages where alphabetical rules differ from English-like ordering. [4]

#### `byReverseWeightOrder()`

This comparator orders larger weights first. If `A` has weight `100` and `B` has weight `20`, then `A` must come before `B`.

A common mistake is subtracting two `long` values and casting the result to `int`. That can overflow or lose information. A comparison should instead use a safe long comparison operation or explicit conditions.

#### `byPrefixOrder(int k)`

This comparator compares only the first `k` characters of each word, ignoring case. For example, with `k = 2`, the terms `Kampala`, `Kansas`, and `kangaroo` all compare equal because their first two characters are `ka` under the case-insensitive rule.

This comparator is the bridge between “prefix matching” and “binary search.” A search key can be represented by a `Term` whose word is the prefix, then the comparator can treat every dictionary word sharing that prefix as equal to the key.

The comparator must obey the comparator contract: comparison must be antisymmetric and transitive. If those properties fail, sorting and binary search are no longer reliable. [2]

### 2. `RangeBinarySearch.java`: two boundary searches

The unfinished class needs two related algorithms.

#### First index

Maintain a search interval `[low, high]`. At each step:

1. Compute `mid` safely as `low + (high - low) / 2`.
2. Compare `terms[mid]` with the key.
3. If the middle value is smaller, discard the left half.
4. If it is larger, discard the right half.
5. If it matches, record `mid` as a candidate and continue searching left.

When the loop ends, the recorded candidate is the first matching index, or `-1` if no match was found.

#### Last index

Use the same structure, but when a match is found, record it and continue searching right. This produces the last matching index.

The invariant to remember is:

> For the first-index search, a match does not finish the search; it only proves that a valid answer exists at or to the left. For the last-index search, it proves that a valid answer exists at or to the right.

This is the same general idea as lower-bound and upper-bound searches. The important performance property is that each iteration removes about half of the remaining candidates, giving `O(log N)` comparisons. Java’s array binary-search APIs also require the input to be sorted according to the same ordering used by the search. [3]

### 3. `Autocomplete.java`: the algorithmic core

The constructor stores the dictionary and calls `sortDictionary()`. The dictionary must be in case-insensitive lexicographic order before any prefix search occurs. Sorting is a one-time preprocessing step with target complexity `O(N log N)`.

#### `numberOfMatches(prefix)`

The intended flow is:

```text
prefix length = k
key = a Term containing prefix
first = firstIndexOf(dictionary, key, byPrefixOrder(k))
last  = lastIndexOf(dictionary, key, byPrefixOrder(k))

if first == -1: return 0
else: return last - first + 1
```

This method should not scan all matches. Its required complexity is `O(log N)`, independent of the number of matching terms `M`.

#### `allMatches(prefix)`

The intended flow is:

1. Find the first and last matching positions using the same prefix comparator.
2. If there is no match, return an empty `Term[]`, not `null`.
3. Copy the matching interval into a new array.
4. Sort that new array by descending weight.
5. Return it.

If `N` is the dictionary size and `M` is the number of matches, the target complexity is `O(log N + M log M)`. The first two boundary searches cost logarithmic time. Sorting the matching block costs `M log M`. Java’s `Arrays.sort` accepts a comparator to define the ordering of object arrays. [2] [3]

A useful implementation principle is to keep the dictionary permanently alphabetical. Do not reorder the main dictionary by weight after each query, because that would destroy the property that makes binary search possible.

### 4. `AutocompleteCLI.java`: a complete vertical slice

The CLI demonstrates how the algorithm becomes an application:

1. It reads a file path and maximum result count from command-line arguments.
2. It parses each line into a weight and a tab-separated string.
3. It constructs `Autocomplete` once.
4. It repeatedly reads a prefix.
5. It calls `numberOfMatches` and `allMatches`.
6. It prints only the top requested number of results.

The provided code is deliberately useful for debugging because it reports the dictionary size and expected match counts. The `NullPointerException` shown in the README is the result of calling unfinished methods that return `null`, not evidence that the CLI design is fundamentally broken. [1]

### 5. `AutocompleteGUI.java`: the same engine behind an event-driven interface

The GUI loads the same dictionary and constructs the same `Autocomplete` object. The difference is when queries happen. A document listener reacts to each change in the text field, calls `allMatches`, and refreshes the suggestion list.

This is an important architectural boundary:

```text
Swing event listener -> Autocomplete API -> binary-search/ranking algorithms -> JList display
```

The user interface should not know how binary search works. It only needs the public operations. This is the beginning of separation between the **domain layer** and the **presentation layer**.

The GUI also illustrates production concerns that the lab does not solve completely. It performs work on every keystroke, displays only the top `k` results after the engine has ranked all matches, assumes a simple case-insensitive ordering, and opens a Google search in the desktop browser. For a larger application, you would consider debouncing input, caching recent prefixes, limiting ranking work to the top `k`, and handling locale and Unicode rules more deliberately.

## The complete request lifecycle

For a query such as `ka`, think through the following sequence:

```text
User types "ka"
        |
        v
GUI or CLI calls allMatches("ka")
        |
        v
Create a search key and prefix comparator with k = 2
        |
        v
Binary search for the left boundary of all "ka*" terms
        |
        v
Binary search for the right boundary of all "ka*" terms
        |
        v
Copy the contiguous matching range
        |
        v
Sort only those matches by descending weight
        |
        v
Display the first max_matches terms
```

The crucial fact is that matching terms are contiguous **because the dictionary was sorted by the same prefix-compatible alphabetical order before the query arrived**.

## What to learn, in the right order

Do not begin by trying to fill every `TODO` at once. Learn the repository in layers.

### Stage 1: Java foundations

Make sure you can explain classes, constructors, private fields, getters, arrays, `static` methods, and exceptions. Then review `String.substring`, `compareToIgnoreCase`, anonymous classes or lambdas, and `Comparator<T>`.

Your checkpoint is that you can write a ten-line program that creates several `Term` objects and sorts them using a comparator.

### Stage 2: Comparators as policies

Implement or mentally simulate the three term orders. Write down sample comparisons before coding them. Ask:

```text
Which object comes first?
What does compare(...) == 0 mean for this comparator?
Is the ordering case-sensitive?
Is the ordering based on the entire word, a prefix, or the weight?
```

Your checkpoint is that you can explain why the dictionary can be alphabetical while the output is ranked by weight.

### Stage 3: Ordinary binary search

Before first/last occurrence search, implement a standard binary search on integers. Draw the `low`, `mid`, and `high` positions for a small array. Practice stating what part of the array has already been ruled out after every comparison.

Your checkpoint is that you can explain why binary search is `O(log N)` rather than `O(N)`.

### Stage 4: Boundary binary search

Modify ordinary binary search so that a match does not immediately return. For the first position, move left after a match. For the last position, move right after a match.

Test at least these cases:

```text
empty array
one item
no match
one matching item
matching range at the beginning
matching range at the end
all items match
multiple terms that compare equal under a prefix comparator
```

Your checkpoint is that you can state the loop invariant in one sentence.

### Stage 5: Connect the range to autocomplete

Implement `numberOfMatches` first. It is easier because it only returns an integer. Then implement `allMatches`, initially returning all matches in alphabetical order. Only after the range is correct should you add weight sorting.

Your checkpoint is that both methods return the same match count for every tested prefix.

### Stage 6: Understand complexity by separating `N` and `M`

Use two variables consistently:

- `N`: total number of dictionary terms.
- `M`: number of terms matching the current prefix.

Then classify each operation:

| Operation | Complexity target | Why |
| --- | ---: | --- |
| Initial dictionary sort | `O(N log N)` | Sort all terms once. |
| One boundary search | `O(log N)` | Halve the remaining interval each step. |
| `numberOfMatches` | `O(log N)` | Two boundary searches and arithmetic. |
| `allMatches` | `O(log N + M log M)` | Find the range, copy `M`, sort `M`. |

Your checkpoint is that you can explain why `numberOfMatches` must not iterate over all matching terms.

### Stage 7: Integrate the CLI and GUI

Use the CLI first because it removes UI complexity. Test the same prefixes on several dictionaries. Then inspect the GUI to see how a stable, reusable algorithmic API can power a live interface.

Your checkpoint is that you can trace one keystroke from the text field to the final displayed suggestion.

## How to study it intuitively

Use a small hand-built dictionary before the large files. For example:

```text
100  apple
80   application
70   apply
200  banana
50   band
```

Sort alphabetically, draw the array indices, and ask where the `app` interval begins and ends. Then change the weights and observe that the interval does not change, while the display order does.

A productive study loop is:

1. Predict the output before running the program.
2. Trace one binary-search iteration on paper.
3. Run a tiny test.
4. Compare the result with the prediction.
5. Change one edge case.
6. Explain the result aloud in plain language.

Do not memorize “binary search goes left” or “binary search goes right” as isolated rules. Tie every movement to the comparator result and to the invariant: which answers are still possible?

## Real-world applications

The lab’s weighted dictionary model appears whenever a system has a stable set of possible strings and a signal that says which strings should be preferred.

| Application | String | Weight |
| --- | --- | --- |
| Web search suggestions | A previous query or popular phrase | Query frequency, freshness, or personalization score |
| City or address lookup | A place name | Population, relevance, or distance |
| Messaging and SMS | A likely next word or phrase | Personal typing frequency or language-model score |
| Product search | A product name or category | Sales, relevance, stock, or user history |
| Command palette | A command or file path | Usage frequency, recency, or permission-aware relevance |
| Campus portal search | A course, room, department, or service | Enrollment, popularity, or contextual relevance |
| Multilingual search | A word or phrase in a local language | Corpus frequency and language-specific ranking |

The sample dictionaries make this connection concrete: city weights represent population, while word weights approximate frequency. The same algorithm can therefore power a city selector, a dictionary, a search box, or a command launcher.

## Important limitations of this lab design

This is a good teaching implementation, but it is not a complete production autocomplete service.

First, the dictionary is static. Real systems often ingest new events, remove stale suggestions, and recompute weights continuously. Second, `allMatches` sorts every matching term even when the interface displays only the top few. If a prefix matches millions of terms and the UI needs ten, a bounded top-`k` heap or precomputed top suggestions can be more efficient. Third, the lab uses a flat sorted array. A trie can make prefix navigation natural and can store top suggestions at each prefix node, although it has different memory costs. Fourth, the assignment’s case-insensitive comparison is not locale-sensitive. The README explicitly identifies locale-aware ordering and fuzzy autocomplete as extensions. [1] [4]

These limitations are valuable learning opportunities because they show how an algorithm that is correct for a lab can be evolved for a real workload.

## The best next project: a multilingual campus and local-places search assistant

Build a small **Campus and Local Places Autocomplete** application for your university or town. A user types a prefix and receives the most useful suggestions for lecture rooms, departments, services, nearby places, or common local terms.

### Version 1: reproduce the lab with your own data

Create a dataset with fields such as:

```text
weight<TAB>display name<TAB>category<TAB>description
```

Start with the original two-field `Term` API so that you understand the algorithm. Use realistic weights, such as the number of visits, enrollment, search frequency, or manually assigned importance.

Deliverables:

- A Java CLI that searches your dataset.
- A small web or desktop UI.
- Tests for empty prefixes, unknown prefixes, case differences, Unicode names, and ties.
- A benchmark comparing linear scan with sorted-range search.

### Version 2: make the ranking useful

Add a stable tie-breaker. If two terms have the same weight, order them alphabetically. Add categories and let the user filter by category. Add a recent-search boost so that a user’s own history can outrank a globally popular result.

At this point, separate the code into:

```text
Data loading -> Autocomplete engine -> Ranking policy -> UI
```

### Version 3: add typo tolerance

Implement a limited fuzzy mode for mistakes such as one missing, inserted, or substituted character. Begin with a small edit distance and a strict candidate limit. Do not generate every possible deletion for a huge dictionary without measuring memory growth; the original README warns that multiple deletion levels can explode the number of terms. [1]

### Version 4: compare data structures

Keep the sorted-array implementation as a baseline. Then implement a trie and compare:

- build time,
- memory usage,
- query latency,
- number of results,
- update cost,
- behavior on long prefixes.

This comparison turns the lab into a genuine data-structures project rather than a copied exercise.

## Other strong project ideas

If the campus/local-places theme is not appealing, the same engine can become a **personal command palette** that searches files, scripts, and common actions; a **recipe and ingredient finder** ranked by frequency or personal preference; a **local-language dictionary assistant** based on a corpus; or a **product catalog search box** with category and stock-aware ranking.

The best choice is the one for which you can collect your own data and observe real users. A project becomes educational when the ranking is imperfect, the data contains edge cases, and you can measure whether an algorithmic change improves the experience.

## A practical four-week plan

### Week 1: understand and implement the primitives

Study `Term.java`, write comparator examples, and implement `getPrefix`. Then implement and test ordinary and boundary binary search using tiny arrays.

### Week 2: complete the engine

Implement dictionary sorting, `numberOfMatches`, and `allMatches`. Add tests for the edge cases listed above. Use the CLI with a small custom dictionary before trying the large datasets.

### Week 3: measure and improve

Record query times for prefixes with zero, few, many, and extremely many matches. Compare a naive linear scan with the sorted-array method. Explain the measured behavior using `N`, `M`, and the theoretical complexity.

### Week 4: build your own application

Collect a domain-specific dataset, add a user-facing interface, implement tie-breaking and filters, and document your design. Finish by comparing the original sorted-array approach with one alternative, such as a trie or a bounded top-`k` ranking structure.

## Final understanding check

You understand the repository when you can answer all of these questions without looking at the code:

1. Why must the dictionary be sorted before a query arrives?
2. Why are two binary searches needed for a prefix?
3. Why can the alphabetical comparator say that many different words are equal?
4. Why does `numberOfMatches` run in `O(log N)` even when `M` is large?
5. Why does `allMatches` depend on `M`?
6. Why should the main dictionary remain alphabetical after a query?
7. What would change if the weights updated every second?
8. When would a trie or a bounded top-`k` structure be better than this array design?

If you can explain those points and build the campus/local-places version, you have learned the transferable idea: **organize data once so that repeated queries can avoid unnecessary work**.

## References

[1]: https://github.com/MubiruEltonFelix1/dsa-lab-2 "MubiruEltonFelix1/dsa-lab-2 GitHub repository and README"

[2]: https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html "Java Platform SE 8 Comparator documentation"

[3]: https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html "Java Platform SE 8 Arrays documentation"

[4]: https://docs.oracle.com/javase/8/docs/api/java/lang/String.html "Java Platform SE 8 String documentation"
