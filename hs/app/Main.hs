module Main (main) where

import Data.Text.IO (getContents)
import Network.HTTP.Client.Conduit (parseRequest_)
import Network.HTTP.Simple (getResponseBody, httpJSON, setRequestBodyJSON, setRequestMethod)
import Relude hiding (lookupEnv)
import System.Environment (lookupEnv)
import System.Process (CreateProcess (cwd), createProcess, proc)

generatePuns :: [Text] -> IO [Text]
generatePuns targets = getResponseBody <$> (httpJSON $ setRequestBodyJSON targets $ setRequestMethod "POST" $ parseRequest_ "http://localhost:3000")

startServer :: IO ()
startServer = do
  maybeRoot <- lookupEnv "DEVENV_ROOT"
  void $ createProcess (proc "clj" ["-M", "-m", "server"]) {cwd = (<> "/clj") <$> maybeRoot}

main :: IO ()
main = do
  input <- getContents
  puns <- generatePuns (lines input)
  traverse_ putTextLn puns
