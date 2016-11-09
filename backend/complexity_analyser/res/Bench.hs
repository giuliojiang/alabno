module Bench where
import IC.TestSuite

import Tests hiding (main)

import Criterion.Main

runTests' = map $ \t@(TestCase s func arg) -> bench s $ whnf goTest t
main = defaultMain [bgroup "tests" (runTests' allTestCases)]
