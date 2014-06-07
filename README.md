simplelang-antlr4
=================

Example project for ANTLR4. Simple language parsing and execution via stack machine.

SimpleLang
----------

Programs are written using the toy language, SimpleLang. It has primitive structure that can be easily understood by looking on `SimpleLang.g4` ANTLR grammar.

Stack machine
----------------------

Stack machine recieves input in the following form:

```
CN DN C1 ... Cn D1 ... Dn
```

Here CN is a number of commands in the sequence, DN - number of data in the data stack. C1-Cn are commands, D1-Dn - data. Each data element is a number between -9999 and 9999. If there's such number in command sequence, it gets pushed on top of data stack.

Code | Command | Data before | Data after | Effect | Description 
:---:|:-------:|:-----------:|:----------:|:------:|:---------------------
-10000 | Add | [1,2,3]  |  [1,5]  | | Take 2 items from data stack and push their sum
-10001 | Mult | [1,2,3]  |  [1,6] | | Take 2 items from data stack and push their product
-10002 | Minus | [1,2,3]  |  [1,2,-3] | | Take 1 item from data stack and push it's negation
-10003 | Div | [1,2,4]  |  [1, 2] | | Take 2 items from data stack, divide the first to second and push result
-10004 | If= | [1, 3, 4, 4]  |   [1] | cc = 3 | Take 3 items from data stack. If the first item equals second, set command counter to the third one
-10005 | If< | [1, 3, 5, 6]  |   [1] | cc = 3 | Take 3 items from data stack. If the first item  is less than second, set command counter to the third one
-10006 | Goto | [1, 2, 3]  |  [1, 2] | cc = 3 | Take 1 item from data stack and set command counter to it's value
-10007 | Load | [1, 2, 3, 1]  |  [1, 2, 3, 2] | | Take 1 item from data stack and push back item on data stack with index equal to it's value
-10008 | Free | [1, 2, 3]  |  [1, 2] | | Remove 1 item from data stack
-10009 | Store | [1, 2, 3, 1, 4]  |  [1, 4, 3] | | Take 2 items from data stack and assign value of second item to item with index equal to value of the first one
-10010 | Count | [1, 2, 3]  |  [1, 2, 3, 4] | | Push count of items on data stack to it
-10011 | Print | [1, 2, 3]  |  [1, 2, 3] | 3 printed | Print item on top of data stack. The stack remains unchanged
-10012 | Read | [1, 2, 3]  | [1, 2, 3, 4] | 4 read | Read item from console and push it to the stack
-10013 | Stop | [1, 2, 3]  |  [1, 2, 3] | program exits | Stop program execution and exit
-10014 | Storec | [1, 2, 3] | [1, 2, 3, 5] | if cc = 5 | Push current command counter value to data stack

Here `cc` stands for command counter.
