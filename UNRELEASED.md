<!-- NOTE:
     Release notes for unreleased changes go here, following this format:

        ### Features

         * Change description, see #123

        ### Bug fixes

         * Some bug fix, see #124

     DO NOT LEAVE A BLANK LINE BELOW THIS PREAMBLE -->
### Bug fixes

 * Fixed a heisenbug caused by EXCEPT on records, which used unsorted keys, see #987
 * Fixed unsound skolemization that applied to let-definitions, see #985
 * Fixed a bug caused by big numbers on annotations, see #990