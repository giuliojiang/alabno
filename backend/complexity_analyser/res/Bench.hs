module Bench where
import IC.TestSuite hiding (goTest, goTestOne)

import Control.Exception
import Control.Monad
import Data.List

import Tests hiding (main)

import Criterion.Main


goTest (TestCase name f cases) = do
  counts <- forM cases (handle majorExceptionHandler . goTestOne name f)
  return True
  where
    majorExceptionHandler :: SomeException -> IO Bool
    majorExceptionHandler e = return False

goTestOne name f (input, expected) = handle exceptionHandler $ do
      r <- evaluate (f input)
      return True
      where
        failedStanza :: Show x => Bool -> x -> IO Bool
        failedStanza _ _ = do
          return False

        exceptionHandler :: SomeException -> IO Bool
        exceptionHandler = failedStanza True


runTests' = map $ \t@(TestCase s func arg) -> bench s $ whnfIO (goTest t)
main = defaultMain [bgroup "tests" (runTests' allTestCases)]
