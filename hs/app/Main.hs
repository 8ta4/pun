module Main (main) where

import Data.Text.IO (getContents)
import Network.HTTP.Client.Conduit (parseRequest_)
import Network.HTTP.Simple (getResponseBody, httpJSON, setRequestBodyJSON, setRequestMethod)
import Relude

generatePuns :: [Text] -> IO [Text]
generatePuns targets = getResponseBody <$> (httpJSON $ setRequestBodyJSON targets $ setRequestMethod "POST" $ parseRequest_ "http://localhost:3000")

main :: IO ()
main = do
  input <- getContents
  puns <- generatePuns (lines input)
  traverse_ putTextLn puns
