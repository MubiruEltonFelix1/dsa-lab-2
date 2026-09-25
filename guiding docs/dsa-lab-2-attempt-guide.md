# How to Attempt Lab 2: Autocomplete (from zero to submission)

CSC 1203 Data Structures and Algorithms.

This document explains the lab from first principles, then gives a step-by-step attempt plan with a test checkpoint after every step. It is written to be read cold, either before you start or when you get stuck halfway.

State of the repository when this was written: all `TODO` methods unimplemented, the CLI fails with a `NullPointerException` on startup use, exactly as the README says. Everything else (clients, dictionaries, templates) is given to you and does not need changes.

---

## 1. What you are building, in one sentence

You are building the engine behind a search bar's suggestion dropdown: you type `and`, and the program instantly returns the most popular words in the dictionary that start with `and`, most popular first.

That is the whole deliverable. Everything else in the lab is machinery to make it fast.

Real-world examples: phone keyboard predictions, Google's search suggestions, Wikipedia's title dropdown. The difference here is that the dictionary is fixed (a file of words and weights), so the program can pre-process it once and then answer many queries quickly.

---

## 2. The two ingredients: words and weights

Every dictionary file is a list of lines like this:

```text
       190569  och
          699  Anders
           10  42
```

Two things per line:

- a **word** (any string, including spaces, but not newlines);
- a **weight** (a nonnegative integer). Weight means importance, popularity, or frequency. Higher weight gets shown first.

Weights can be as large as 2^63 - 1, so they must be stored as `long`, not `int`. In `cities.txt` the weight is population; in `romaner.txt` it is how often the word appeared in 69 Swedish novels.

The class [Term](../Term.java) models one (word, weight) pair. Nothing there needs fixing except the methods marked `TODO`.

---

## 3. The central idea (read this twice)

The naive approach: for every query, scan all 83,000+ words and check which start with the prefix. That is correct but slow, and the lab's complexity requirements forbid it.

The intended approach rests on one observation:

> If you sort the words alphabetically, every word sharing a prefix sits next to the others in one contiguous block.

Example, sorted:

```text
algebra
algebraic
alice      <- all "al" words live together here
alloy
banana
```

So the pipeline is:

```text
dictionary file
  -> Term[] array
  -> sort once, alphabetically, case-insensitive        [Autocomplete constructor]
  -> per query: binary search for the prefix block      [RangeBinarySearch]
  -> copy that block, sort the copy by descending weight [Autocomplete.allMatches]
  -> print the top matches                              [AutocompleteCLI]
```

You pay O(N log N) once at startup, and each query costs O(log N + M log M) instead of O(N).

Two tricks make it work, and every part of the lab is built around them:

1. **Sorted alphabetically means the matches are contiguous.** Find the block's two boundaries and you have all M matches without looking at anything else.
2. **`byPrefixOrder(k)` turns "starts with" into "equals".** Binary search only knows how to look for equality. By comparing only the first `k` characters (where `k` is the query prefix length), a dictionary word is "equal" to the search key exactly when it starts with that prefix. The search key is a fake `Term` whose word is the prefix itself.

---

## 4. The files and who owns what

| File | Role | Your job |
| --- | --- | --- |
| [Term.java](../Term.java) | Models one suggestion; defines three comparison orders | Implement 4 methods |
| [RangeBinarySearch.java](../RangeBinarySearch.java) | Finds first/last index of a key in a sorted array | Implement 2 methods |
| [Autocomplete.java](../Autocomplete.java) | Owns the sorted dictionary; answers queries | Implement 3 methods |
| [AutocompleteCLI.java](../AutocompleteCLI.java) | Command-line client (READ: prints prompts, calls your code) | Do not touch |
| [AutocompleteGUI.java](../AutocompleteGUI.java) | Optional graphical client | Do not touch |
| `dictionaries/` | 5 real datasets (romaner, cities, wiktionary, gp2011, nordsamiska) | Test data |
| [answers.txt](../answers.txt) | Written questions + group info | Fill in completely |
| [Tester.java](../Tester.java) | Throwaway test harness for Parts 1, 2 and a bonus Part 3 check | Run it after each step; delete before submitting |

The submission is exactly four files: `Term.java`, `RangeBinarySearch.java`, `Autocomplete.java`, `answers.txt`.

---

## 5. Your starting point (verified on this machine)

Checked and confirmed:

- Java 25 (LTS) with `javac` is installed and on the PATH.
- All given files compile cleanly with no changes.
- Running the CLI with prefix `and` prints `Number of matches: 0` and then throws `NullPointerException` at [AutocompleteCLI.java:59](../AutocompleteCLI.java#L59), which matches the README. That null is the result of `allMatches` returning `null` while unimplemented.
- The exit code 1 after that error is the CLI's own error handler, not something to fix.
- `javac *.java` works in this PowerShell, so that is the compile command to use.

Basic commands (run from the project folder):

```powershell
cd "c:\Users\Administrator\Downloads\Lab2-java\Lab2-java"
javac *.java
java AutocompleteCLI dictionaries/romaner.txt 5
```

Notes:

- The CLI reads a prefix and prints the top `max_matches` results. Quit by pressing Enter on an empty line.
- The dictionary parser splits each line on a **TAB** character (`line.trim().split("\t")`). If you create your own test dictionary with spaces between weight and word, the CLI throws an exception on startup. Use tabs.
- The file format is: weight first, then tab, then the word (`190569<TAB>och`).

---

## 6. The rules of the game

From the README, before anything else:

- Labs are done **in groups**. If you cannot work with your previous partners, contact David Sabiiti Bamutura.
- There are **two deadlines**. The first is for at least a partial attempt (you get marked and get feedback); the second is for a complete, correct solution. If you are reading this near the first deadline, submit whatever works rather than nothing.
- The lab is part of the examination. You may discuss ideas with other groups, but the submitted work must be your group's own. No copying code in either direction.
- Only classes from `java.lang` and `java.util` may be used in your implementation.
- If you are unsure whether your solution is correct, ask at a lab session.

What "correct" means to a marker, in four dimensions:

1. **Functional correctness**: counts, ordering, case handling, empty results.
2. **Algorithmic correctness**: the boundary searches are genuinely logarithmic; no hidden linear scans.
3. **API correctness**: method names, signatures and return contracts match the given clients.
4. **Explanation quality**: `answers.txt` accurately explains the algorithm and its complexity.

---

## 7. The attempt plan

Work in this order. Each step ends with a checkpoint; do not move past a step that fails. The order matters because `Autocomplete` depends on the other two files, so errors stay easy to localize.

Suggested budget: about 5 hours of work, leaving an hour of buffer.

### Step 1. `Term.java` (about 45 minutes)

Four methods, all small.

**`getPrefix(int len)`**: return the first `len` characters of the word. If the word is shorter than `len`, return the whole word. Do not call `substring` with an out-of-range end index. `Math.min(len, word.length())` handles the dangerous case.

```text
"camera", 3  ->  "cam"
"cat",    3  ->  "cat"
"a",      3  ->  "a"
```

**`byLexicographicOrder()`**: A to Z, ignoring case.

```java
return (a, b) -> a.getWord().compareToIgnoreCase(b.getWord());
```

**`byReverseWeightOrder()`**: heavier first. Compare with `Long.compare` so huge weights cannot overflow.

```java
return (a, b) -> Long.compare(b.getWeight(), a.getWeight());
```

(Note the swapped arguments: reversing the comparison is what reverses the order. Subtracting the two weights and casting to `int` is a classic wrong answer that breaks on large weights.)

**`byPrefixOrder(int k)`**: compare only the first `k` characters, case-insensitively. This is the method the whole binary search strategy depends on.

```java
return (a, b) -> a.getPrefix(k).compareToIgnoreCase(b.getPrefix(k));
```

**Checkpoint.** A pre-written throwaway [Tester.java](../Tester.java) is in the project folder. Run `javac *.java` then `java Tester`: section 1 must pass. Delete the file before submitting. It verifies (among other checks):

| Test | Expected |
| --- | --- |
| `new Term("camera", 0).getPrefix(3)` | `"cam"` |
| `new Term("cat", 0).getPrefix(3)` | `"cat"` |
| `byLexicographicOrder()`, `"apple"` vs `"banana"` | negative |
| `byLexicographicOrder()`, `"Apple"` vs `"apple"` | zero |
| `byLexicographicOrder()`, `"zebra"` vs `"Yak"` | positive |
| `byReverseWeightOrder()`, weight 100 vs weight 20 | negative (100 first) |
| `byReverseWeightOrder()`, equal weights | zero |
| `byPrefixOrder(2)`, `"Kampala"` vs `"Kansas"` | zero |
| `byPrefixOrder(2)`, `"Kampala"` vs `"London"` | negative |

Also check the comparator contract on each comparator, using any two terms `a` and `b`: `compare(a, b)` and `compare(b, a)` must have opposite signs (or both be zero). A comparator that sometimes flips its answer makes sorting and binary search silently unreliable.

### Step 2. `RangeBinarySearch.java` (about 90 minutes; the hard part)

**What "equal" means here**: the methods receive a comparator. Two elements are equal in this search when `comparator.compare(a, b) == 0`. With `byPrefixOrder(k)`, many array elements can be "equal" to the key at once, and the array is guaranteed sorted by that same comparator. You never sort anything in this file.

Ordinary binary search finds *a* match and stops. That is not enough: you need the boundary indices.

```text
index:   ... 100 101 102 103  ... 203 204 205
value:   ... Anders Andro  and  andan ...    X   Y
               ^                              ^
        firstIndexOf("and")            lastIndexOf("and")
```

**`firstIndexOf` pattern**: keep a variable `answer = -1`. Every time you land on an element equal to the key, record `answer = mid` and keep searching **left** (an earlier match could still be hiding). Only if the comparison says smaller do you go right, and only if it says greater do you go left. When the interval is empty, `answer` is the first index, or -1 if nothing matched.

```text
low = 0;  high = length - 1;  answer = -1
while low <= high:
    mid = low + (high - low) / 2
    c = comparator.compare(terms[mid], key)
    if c < 0:      low = mid + 1
    else if c > 0: high = mid - 1
    else:          answer = mid;  high = mid - 1     // save and continue LEFT

return answer
```

**`lastIndexOf`** is the mirror image: on equality, `answer = mid` and `low = mid + 1` (continue **right**).

The invariant to keep in your head while debugging: *`answer` always holds the best known index, and any better (earlier/later) match must lie inside `[low, high]`.*

Two traps that fail submissions:

- **Walking outward with a loop after finding a match.** That is O(N) worst case. The requirement is at most 1 + ceil(log2 N) compares.
- **Returning immediately on the first equality.** Counts come out too small or jump around depending on array contents.

**Checkpoint.** Recompile and run `java Tester` again: section 2 covers the `[apple, banana, banana, banana, carrot]` example (first index 1, last index 3), all of the cases below, and 500 randomized arrays cross-checked against a linear scan. Among other checks, it verifies:

| Case | Expected |
| --- | --- |
| Empty array | -1 from both |
| Key not present | -1 from both |
| Exactly one match | first == last == that index |
| Match at index 0 | first = 0 (search must not go below zero) |
| Match at last index | last = length - 1 |
| Every element matches | first = 0, last = length - 1 |

If a boundary comes out wrong, print `low, mid, high, comparison, answer` for each iteration in a temporary copy and watch which side gets discarded. Remove the prints afterward.

### Step 3. `Autocomplete.java` (about 90 minutes)

**`sortDictionary()`**: one call. The array is sorted in place, once, and must stay alphabetical forever after; every later query depends on it.

```java
java.util.Arrays.sort(dictionary, Term.byLexicographicOrder());
```

**`numberOfMatches(String prefix)`**: must be O(log N), so it may not loop over matches or over the dictionary.

```text
key     = new Term(prefix, 0)                 // weight is never inspected
comp    = Term.byPrefixOrder(prefix.length())
first   = RangeBinarySearch.firstIndexOf(dictionary, key, comp)
last    = RangeBinarySearch.lastIndexOf(dictionary, key, comp)
returns 0 if first == -1, otherwise last - first + 1
```

The fake key Term is the second half of the trick from section 3: the comparator only looks at the first `k` characters, so the weight of the key is irrelevant.

**`allMatches(String prefix)`**: same key and comparator, same two boundary searches, then:

1. If there is no match, return an empty `Term[]` (length 0, **never null**).
2. Copy the inclusive range into a new array: `java.util.Arrays.copyOfRange(dictionary, first, last + 1)`. Note the exclusive endpoint: passing `last` instead of `last + 1` silently drops the last match.
3. Sort the copy with `Term.byReverseWeightOrder()`.
4. Return the copy.

The single biggest trap in the lab: **never sort the main dictionary by weight.** If you do, the alphabetical order is destroyed and every subsequent query returns garbage, even though the first query looks fine. Copy first, sort the copy. Copying also protects you from a GUI that might reorder the array it receives; you hand out your own array every time.

Worked example with the small dictionary from section 8, prefix `ca`:

```text
sorted dictionary:  apple, application, apply, banana, band,
                    camera, candle, car, cat
"ca" block:         camera, candle, car, cat       (indices 5..8, contiguous)
count:              4
sorted by weight:   car 90, camera 60, candle 40, cat 30
```

**Checkpoint.** Run `java Tester` again: section 3 (bonus) checks the small dictionary automatically. Then run the CLI and try prefix `ca`. Expect 4 matches in the order above.

### Step 4. Verify against the README's numbers (about 30 minutes)

The README provides exact expected outputs. The counts are the most important check because they do not depend on your result ordering at all.

```powershell
cd "c:\Users\Administrator\Downloads\Lab2-java\Lab2-java"
javac *.java
java AutocompleteCLI dictionaries/romaner.txt 5
```

| File | Prefix | Must show |
| --- | --- | --- |
| romaner | `and` | Number of matches: 104, `andra` (7659) first |
| romaner | `42` | 2 matches (`42`, `425`) |
| romaner | `c` | 929 matches, `Charles` (1032) first |
| romaner | `flaggstångsknopp` | 0 matches |
| cities (max 7) | `Gö` | 64 matches, `Göteborg, Sweden` (504084) first |
| cities (max 7) | `Al M` | 39 matches, `Al Maḩallah al Kubrá, Egypt` (431052) first |

Also test:

- mixed case: `A` should behave like `a` (case-insensitive);
- Unicode: `Gö` and `Al M` above; `čohkiidus` in nordsamiska.txt;
- a prefix longer than any word: 0 matches, no exception;
- the other dictionaries: `wiktionary.txt`, `gp2011.txt`, `nordsamiska.txt`.

Optional but strong verification: for one or two prefixes, write the brute-force version on purpose (scan the whole array, keep words that start with the prefix ignoring case, sort by weight) and confirm it produces the same count and sequence as your fast version.

### Step 5. `answers.txt` (about 30 minutes)

Replace every `[...]` placeholder. Put the group name and all group member names at the top.

**Question 1, how `firstIndexOf` works.** In your own words, cover:

1. The search interval starts as the entire array.
2. The middle element is compared with the key, using the supplied comparator.
3. A smaller middle value means the first match, if any, is to the right; discard the left half.
4. A larger middle value means it must be to the left; discard the right half.
5. On equality, save the index and continue searching left, because an earlier equal element may exist.
6. When the interval is exhausted, the saved index is the first match by definition of the invariant.
7. Return -1 if no equality was ever found.

**Question 2, complexities.** Use the lab's variables: n = number of terms in the dictionary, m = number of matching terms.

| Method | Complexity | One-line reason |
| --- | --- | --- |
| `sortDictionary()` | O(n log n) compares | The whole dictionary is sorted once with a comparison sort. |
| `allMatches()` | O(log n + m log m) | Two boundary searches (O(log n)), then m matches copied and sorted by weight (O(m log m)). |
| `numberOfMatches()` | O(log n) | Two boundary searches, then arithmetic on the two indices. It must not inspect any matching term. |

**Appendix A to E.** Answer honestly: hours spent per member, known bugs or limitations, collaboration and resources, serious problems encountered, and any comments. A known-limitations answer of "none" is fine if it is true, but be sure it is true.

### Step 6. Final pass and submission (about 20 minutes)

- Delete `Tester.java` and `Tester.class`, any debug prints, and scratch files you created. Only the four submission files should carry your changes.
- Confirm `answers.txt` has no placeholders left.
- Compile from clean and run once more:

```powershell
javac *.java
java AutocompleteCLI dictionaries/romaner.txt 5
```

then type `and` and confirm 104.

- Submit the four files the way your course collects them: `Term.java`, `RangeBinarySearch.java`, `Autocomplete.java`, `answers.txt`.

---

## 8. A small test dictionary

Create `small-dictionary.txt` in the project folder. Separate each weight and word with a **TAB**, not spaces:

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

Run `java AutocompleteCLI small-dictionary.txt 10` and try:

| Prefix | Expected matches | Expected order |
| --- | --- | --- |
| `app` | 3 | apple 100, application 80, apply 70 |
| `ban` | 2 | banana 200, band 50 |
| `ca` | 4 | car 90, camera 60, candle 40, cat 30 |
| `z` | 0 | (nothing) |
| `A` | 3 | apple 100, application 80, apply 70 (case-insensitive) |

Small data makes errors visible. If you debug on 83,334 terms, you cannot tell whether the fault is parsing, sorting, searching, or ranking.

---

## 9. Common mistakes and their symptoms

| Mistake | Symptom | Fix |
| --- | --- | --- |
| Case-sensitive comparison (`compareTo`) | Fewer results than expected for `A`, `Gö`, `c` | Use `compareToIgnoreCase` in all three word comparisons |
| Stopping at the first binary search match | Counts too small or unstable | Save the match and keep searching left (first) or right (last) |
| Walking outward with a loop after a match | Correct results, fails the O(log N) requirement | Stay inside the halving loop |
| Sorting the dictionary by weight | First query looks right; later queries return nonsense | Copy the matching range, sort the copy |
| Returning `null` when nothing matches | NullPointerException in CLI/GUI | Return `new Term[0]` |
| Off-by-one in the range copy | First or last match missing, or a bounds exception | `Arrays.copyOfRange(dictionary, first, last + 1)` |
| Subtracting weights and casting to int | Wrong order for very large weights | `Long.compare(b.getWeight(), a.getWeight())` |
| Looping in `numberOfMatches` | Functionally correct, violates the complexity requirement | `last - first + 1` |
| Space-separated custom dictionary | Exception on startup, before any query | Use TAB characters in the file |
| Editing the CLI or GUI to make things work | Your own demo passes, the provided client fails | Restore the clients; keep the public API unchanged |

---

## 10. Debugging decision tree

When something is wrong, isolate the layer and change one thing at a time.

- **Counts wrong** (but the program runs): inspect `getPrefix`, `byPrefixOrder`, the prefix length passed in, the two boundary searches, and `last - first + 1`.
- **Counts right, order wrong**: inspect the range copy and `byReverseWeightOrder`, and confirm you sorted the copy, not the dictionary.
- **`NullPointerException` from the CLI**: `allMatches` returned null, or your array contains a null element.
- **Exception at startup**: dictionary file format (tabs), missing file, or missing command-line arguments.
- **Exception from `substring`**: `getPrefix` was called with a length beyond the word; clamp with `Math.min`.
- **Boundary values wrong**: print `low, mid, high, comparison, answer` per iteration and ask: when the comparison was negative, which half did I discard? Which half should it have been?

---

## 11. Edge case checklist

- [ ] Empty array (numberOfMatches must return 0, allMatches an empty array)
- [ ] One element array
- [ ] Every element matches
- [ ] Match at index 0
- [ ] Match at the final index
- [ ] No matches at all
- [ ] Prefix longer than the words
- [ ] Mixed case input
- [ ] Unicode input (`Gö`, `čohkiidus`)
- [ ] Weights at the top of the range (2^63 - 1)
- [ ] Empty prefix: decide what your API does and be consistent (the CLI never sends one, since Enter quits)

---

## 12. Reference outputs

`java AutocompleteCLI dictionaries/romaner.txt 5`, prefix `and`:

```text
Number of matches: 104
        7659    andra
         699    Anders
         625    Andro
         371    andre
         295    andan
```

`java AutocompleteCLI dictionaries/cities.txt 7`, prefix `Gö`:

```text
Number of matches: 64
      504084    Göteborg, Sweden
      122149    Göttingen, Germany
       58040    Göppingen, Germany
       57751    Görlitz, Germany
       40763    Gönen, Turkey
       34243    Göksun, Turkey
       32374    Gödöllő, Hungary
```

Note the output format comes from `Term.toString()`: the weight right-aligned in 12 columns, four spaces, then the word.

---

## 13. Schedule at a glance

| Step | Time |
| --- | --- |
| Read this document and the README once | 20 min |
| Step 1: `Term.java` + tester | 45 min |
| Step 2: `RangeBinarySearch.java` + tests | 90 min |
| Step 3: `Autocomplete.java` | 90 min |
| Step 4: verify vs README on all dictionaries | 30 min |
| Step 5: `answers.txt` | 30 min |
| Step 6: cleanup + final run + submit | 20 min |
| **Total** | **approximately 5 hours** |

If the clock is short, the minimum viable submission is steps 1 to 3 plus a partially filled `answers.txt`. A working engine with an honest partial write-up is worth far more than a perfect write-up with no engine.

---

## 14. Where to read more

Documents in this same folder:

- `dsa-lab-2-learning-guide.md`: the same mental model with more theory and background.
- `dsa-lab-2-run-implement-pass-guide.md`: pseudocode, invariants, and a longer test checklist.
- `Data Structures, Algorithms, and Autocomplete_ A Mental Model From Scratch.md`: the big-picture "why" of data structures in this kind of problem.

In the repository itself: the README ([../README.md](../README.md)) is the authoritative source for the lab's requirements, examples, and submission list.

If you are unsure whether something is correct, the README says to ask at a lab session. With the CLI reproducing the expected example outputs in section 12, you are almost certainly fine.
