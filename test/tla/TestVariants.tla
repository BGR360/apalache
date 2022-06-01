---------------------------- MODULE TestVariants -------------------------------
(*
 * Functional tests for operators over variants.
 * We introduce a trivial state machine and write tests as state invariants.
 *)

EXTENDS Integers, FiniteSets, Apalache, Variants

Init == TRUE
Next == TRUE

(* DEFINITIONS *)

VarA == Variant("A", 1)

VarB == Variant("B", [ value |-> "hello" ])

TestVariant ==
    VarA \in { VarA, VarB }

TestVariantFilter ==
    \E v \in VariantFilter("B", { VarA, VarB }):
        v.value = "hello"

TestVariantGet ==
    VariantGet("B", VarB) = [ value |-> "hello" ]

TestVariantMatch ==
    VariantMatch(
        "A",
        VarB,
        LAMBDA i: i > 0,
        LAMBDA v: FALSE
    )

AllTests ==
    /\ TestVariant
    /\ TestVariantFilter
    /\ TestVariantGet
    /\ TestVariantMatch

===============================================================================
