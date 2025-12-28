module Main (main) where

import Control.Concurrent (threadDelay)
import Control.Exception (catch)
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

pollForPuns :: [Text] -> IO [Text]
pollForPuns targets = do
  threadDelay 1000000
  catch
    (generatePuns targets)
    (\(_ :: SomeException) -> pollForPuns targets)

main :: IO ()
main = do
  input <- getContents
  puns <-
    catch
      (generatePuns (lines input))
      ( \(_ :: SomeException) -> do
          startServer
          pollForPuns (lines input)
      )
  traverse_ putTextLn puns
