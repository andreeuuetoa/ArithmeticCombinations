# ArithmeticCombinations

This was a quick hobby project made to answer a question a friend of mine
asked in December 2022:
> Using 4 different numbers from the set {2, 3, 4, 5, 7, 8, 9, 44, 55},
in how many different ways can these numbers be combined using the arithmetic operators
`+`, `-`, `/`, and `*`, to get a result of 14?  
> Up to the answerer whether using parentheses is allowed or not.  
> Note: `2 + 3 + 4 + 5`, `(2 + 3) + 4 + 5`, and `3 + 2 + 4 + 5` do not count as different from each other.

At first, I thought this would take a few hours to solve,
but I ended up spending about three days during Christmas working almost non-stop to solve this.  
My initial underestimation of this task also caused me to put off writing unit tests for longer than I should have,
which caused quite a lot of pain over repeatedly breaking previously-working functionality.

This was a fascinating and fun little problem to work on, and I am fairly confident I arrived at
a correct solution by the end.


The program doesn't take arguments, so you'll need to modify the source code if you want to change options such as: 
* the initial set of numbers
* the target number
* whether parentheses are allowed or not
* whether a number can be used multiple times