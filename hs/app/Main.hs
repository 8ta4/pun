module Main (main) where

import Data.Text.IO (getContents)
import Relude

main :: IO ()
main = do
  input <- getContents
  print $ lines input
