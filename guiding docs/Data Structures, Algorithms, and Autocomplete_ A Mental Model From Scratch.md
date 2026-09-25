# Data Structures, Algorithms, and Autocomplete: A Mental Model From Scratch

## How to use this document

This document assumes you are new to data structures and algorithms. It does not begin with Java syntax or binary-search formulas. It begins with a practical question:

> If a computer has thousands of possible words and a person types only a few letters, how can the computer quickly suggest the most likely complete words?

By the end, you should be able to picture what data structures and algorithms are, why this repository exists, how autocomplete works, and why the code is designed in separate layers.

---

## 1. What is a program really doing?

A program receives **information**, transforms it using **rules**, and produces an **answer**.

For this repository:

```text
Input:     a dictionary of words and weights, plus a typed prefix
Rules:     find words beginning with the prefix and rank them by weight
Output:    the best matching suggestions
```

Imagine a user types:

```text
ca
```

The dictionary might contain:

```text
cat       weight 90
car       weight 80
camera    weight 70
banana    weight 200
candle    weight 40
```

The correct matches are `cat`, `car`, `camera`, and `candle`. The word `banana` is not a match because it does not begin with `ca`. If the program displays only the most popular results, it should show the matching words in descending weight order.

That is the whole problem in ordinary language.

The rest of the repository is about performing those two jobs efficiently:

1. **Find the correct group of words.**
2. **Put that group in a useful order.**

---

## 2. What is DSA?

**DSA** means **Data Structures and Algorithms**.

A **data structure** is a way of organizing information so that certain operations are easy or fast.

An **algorithm** is a precise sequence of steps for solving a problem.

These are not separate worlds. They work together:

```text
Data structure: how the dictionary is organized
Algorithm:     how the program searches that organization
```

### Everyday example: finding a name

Suppose 10,000 names are written on loose pieces of paper. Finding one name may require looking at almost every paper. That is a slow organization.

Now suppose the names are arranged alphabetically in a book. You can open near the middle, decide whether to move earlier or later, and eliminate half of the remaining pages at a time. That is a better organization plus a better algorithm.

The names are the data. The book’s alphabetical structure is the data structure. The “open in the middle and eliminate half” procedure is binary search.

### The key question in DSA

When learning DSA, repeatedly ask:

> What operation do I need to perform often, and how should I organize the data so that operation is cheap?

For autocomplete, the frequent operation is:

```text
Given a prefix, find all matching terms quickly.
```

That requirement determines the design.

---

## 3. Why a simple scan is not enough

The most obvious autocomplete algorithm is a **linear scan**:

```text
for every term in the dictionary:
    if the term begins with the prefix:
        keep it
```

This is easy to understand and may work for a small dictionary. If the dictionary has `N` terms, however, the program may inspect all `N` terms for every keystroke.

With a large dictionary and many users, the cost repeats:

```text
user types one character      scan the dictionary
user types a second character scan the dictionary again
user types a third character  scan the dictionary again
```

The lab teaches a better strategy:

1. Sort the dictionary once.
2. Use the sorted order to find the matching region quickly.
3. Work only with that region.

This is a general DSA pattern called **preprocessing**. You spend time preparing the data so that future queries are faster.

---

## 4. What does “sorted” buy us?

Consider this alphabetically sorted dictionary:

```text
apple
application
apply
banana
band
car
camera
candle
cat
```

All words beginning with `ca` appear next to each other:

```text
car
camera
candle
cat
```

They form a continuous **range** or **interval** in the array.

This is the central picture for the repository:

```text
alphabetical array

apple | application | apply | banana | band | [car | camera | candle | cat] | ...
                                                   ^                       ^
                                             first match              last match
```

Once the program knows the first and last positions, it knows every matching term without checking unrelated terms.

### Why are prefix matches contiguous?

Alphabetical order groups similar beginnings together. Words beginning with `ca` come before words beginning with `cb`, and after words beginning with `bz`. Therefore, the `ca` words cannot be scattered randomly throughout the sorted array.

This is why sorting is not cosmetic. It creates a structure that the search algorithm can exploit.

---

## 5. What is an array?

An **array** is a numbered row of values.

For example:

```text
index:  0       1       2       3       4
value: apple   band    car   camera  cat
```

The index lets the program jump directly to a position. It does not have to start at index `0` every time.

The repository stores its dictionary as a `Term[]`, which means an array of `Term` objects.

Each `Term` contains two pieces of information:

```text
word   -> the suggestion text
weight -> how strongly it should be preferred
```

For example:

```text
Term("Kampala", 1800000)
```

The weight might represent population, query frequency, sales, or any other ranking signal.

---

## 6. What is a search algorithm?

A search algorithm answers questions such as:

```text
Does this value exist?
Where is it?
Where does a group of matching values begin and end?
```

### Linear search

Linear search checks items from left to right:

```text
check item 0
check item 1
check item 2
...
```

It is simple but may inspect almost everything.

### Binary search

Binary search requires sorted data. It works like this:

1. Look at the middle item.
2. Compare the target with the middle item.
3. If the target should appear earlier, discard the later half.
4. If the target should appear later, discard the earlier half.
5. Repeat with the remaining half.

Example:

```text
[apple, banana, band, car, camera, candle, cat, dog]
                         ^
                       middle
```

If the target begins with `ca` and the middle item is `car`, the algorithm knows that matches may exist at or around this position. If the middle item were `banana`, it would know to move right.

The number of remaining candidates is roughly cut in half each step:

```text
N -> N/2 -> N/4 -> N/8 -> ...
```

That is why binary search takes `O(log N)` time.

---

## 7. What does `O(log N)` mean?

Big-O notation describes how the amount of work grows as the input becomes larger.

You do not need to treat it as mysterious mathematics. Read it as a growth-rate label.

| Complexity | Intuitive meaning |
| --- | --- |
| `O(1)` | Work stays roughly constant. |
| `O(log N)` | Each step eliminates a large fraction, usually half. |
| `O(N)` | Work grows directly with the number of items. |
| `O(N log N)` | Common cost for comparison-based sorting. |
| `O(N²)` | Work can grow very quickly because items are repeatedly compared with many others. |

For the lab:

- Sorting the dictionary once costs approximately `O(N log N)`.
- Finding a prefix range costs `O(log N)`.
- Sorting the matching results costs approximately `O(M log M)`, where `M` is the number of matches.

The distinction between `N` and `M` is important:

```text
N = all terms in the dictionary
M = terms matching this particular prefix
```

A prefix such as `xylophone` may have a tiny `M`, while a prefix such as `a` may have a large `M`.

---

## 8. Why does the lab need two binary searches?

Ordinary binary search usually answers:

```text
Is there a matching item, and where is one example?
```

Autocomplete needs a stronger answer:

```text
Where does the matching block begin?
Where does the matching block end?
```

Suppose the sorted array contains:

```text
index:  0       1       2       3       4       5       6
value: apple   car   camera  candle  cat    dog    zebra
```

For prefix `ca`, the first match is index `1`, and the last match is index `4`.

The first-index algorithm behaves like this:

```text
When it finds a match, keep the match but continue left.
```

The last-index algorithm behaves like this:

```text
When it finds a match, keep the match but continue right.
```

Once the boundaries are known, the number of matches is:

```text
last - first + 1
```

The `+1` exists because both endpoints are included.

---

## 9. What is a comparator?

A **comparator** is a rule for deciding which of two objects should come first.

For two values `a` and `b`, a comparator returns:

```text
negative -> a comes before b
zero     -> they are equal according to this ordering
positive -> a comes after b
```

The important phrase is **according to this ordering**. Two terms can be equal under one comparator and different under another.

The repository uses three ordering policies.

### Alphabetical comparator

Compare the words without considering uppercase versus lowercase.

```text
Apple and apple compare as equal under the lab rule.
```

This ordering prepares the dictionary for prefix search.

### Reverse-weight comparator

Compare the weights so that larger values come first.

```text
weight 900 comes before weight 100
```

This ordering prepares the results for display.

### Prefix comparator

Compare only the first `k` characters.

If `k = 2`, these terms have the same prefix key:

```text
Kampala
Kansas
kangaroo
```

They all begin with `ka` when compared without case sensitivity.

This is the clever part of the lab. Binary search is normally described as searching for an exact value, but here the comparator deliberately treats all words sharing the requested prefix as equivalent.

---

## 10. The complete autocomplete picture

Here is the full pipeline:

```text
raw dictionary
      |
      v
create Term objects
      |
      v
sort once by alphabetical word order
      |
      v
user types a prefix
      |
      v
use prefix comparator and binary search
      |
      v
find first and last matching indices
      |
      v
extract only the matching terms
      |
      v
sort those terms by descending weight
      |
      v
display suggestions
```

The program has two different notions of “best”:

```text
best for locating: alphabetical order
best for showing:  highest weight first
```

Confusing those two purposes is one of the most common mistakes when learning this lab.

---

## 11. How the Java files fit together

### `Term.java`

This file answers:

```text
What is one autocomplete item?
How can two items be compared?
```

It is the vocabulary of the program.

### `RangeBinarySearch.java`

This file answers:

```text
Where is the matching block inside a sorted array?
```

It is the navigation mechanism.

### `Autocomplete.java`

This file combines the model and the search algorithm:

```text
sort dictionary
find matching range
rank matching terms
```

It is the main domain logic.

### `AutocompleteCLI.java`

This file lets a human use the engine in a terminal.

### `AutocompleteGUI.java`

This file lets a human use the engine through a graphical interface. The GUI reacts to each text change and asks the `Autocomplete` object for suggestions.

The GUI should not need to know how binary search works. That separation is a basic software-design idea:

> The interface asks for a service; the algorithm provides the service.

---

## 12. What happens when the user types one character at a time?

Suppose the user wants to search for `kampala`.

### After typing `k`

The program finds every term beginning with `k`, ranks them, and shows the most useful few.

### After typing `ka`

The matching interval becomes smaller. The program searches again using a two-character prefix.

### After typing `kam`

The interval becomes smaller again.

The algorithm does not edit the entire dictionary into a new structure for every keystroke. It reuses the sorted dictionary and changes only the prefix length used by the comparator.

This is why the feature feels instantaneous when the implementation is efficient enough.

---

## 13. Real-world meaning of a weight

The algorithm does not know what a weight means. It only knows that higher weights should appear earlier.

A weight could represent:

- how often a search phrase was used;
- how many people live in a city;
- how often a word appears in a language corpus;
- how many times a product was purchased;
- how recently a command was used;
- how relevant an item is to the current user.

This is a powerful abstraction:

```text
The search mechanism stays the same.
Only the source and meaning of the weights change.
```

That is why one algorithm can support search suggestions, city lookup, product search, and command palettes.

---

## 14. Why the lab is useful even though real systems are more complex

The lab uses a static array because arrays make the relationship between sorting and binary search visible. Production systems may use tries, databases, indexes, caches, heaps, distributed services, or machine-learning ranking models.

Those systems still contain the same underlying questions:

```text
How is the data organized?
What query must be fast?
What is the ranking signal?
How often does the data change?
How many results must be returned?
What happens for unknown or misspelled input?
```

The lab gives you a small, understandable system in which you can see every part.

---

## 15. The mental model you should remember

Keep this five-part picture in your head:

```text
1. Data:       Terms with words and weights.
2. Preparation:Sort the terms alphabetically.
3. Search:     Find the prefix interval with binary search.
4. Ranking:    Order that interval by descending weight.
5. Interface:  Show the best few results to a user.
```

Or in one sentence:

> Organize the dictionary by word order so that a prefix becomes a contiguous range, locate that range quickly, and then rank the range according to what the user is likely to want.

Once this picture is clear, the individual methods in the repository stop looking like unrelated TODOs. Each method has one responsibility inside the larger story.

## Final beginner checkpoint

You are ready to implement the lab when you can explain these questions in your own words:

1. What problem is autocomplete solving?
2. Why is a linear scan simple but potentially slow?
3. Why does alphabetical sorting make prefix matches contiguous?
4. What does binary search eliminate at every step?
5. Why does autocomplete need both a first index and a last index?
6. Why are there separate alphabetical and weight comparators?
7. What is the difference between `N`, the dictionary size, and `M`, the number of matches?
8. Which file represents a term, which file searches ranges, and which file combines everything?

If you can answer those questions, you already understand the conceptual structure of the repository. The next step is translating that picture into Java carefully and testing each piece in isolation.

## References

[1]: https://github.com/MubiruEltonFelix1/dsa-lab-2 "MubiruEltonFelix1/dsa-lab-2 GitHub repository"

[2]: https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html "Java Platform SE 8 Comparator documentation"

[3]: https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html "Java Platform SE 8 Arrays documentation"

[4]: https://docs.oracle.com/javase/8/docs/api/java/lang/String.html "Java Platform SE 8 String documentation"
