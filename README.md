# pun

## Stick to Your Puns

> What is this tool about?

`pun` generates obvious puns.

## Setup

> How do I set up `pun`?

1. Install [devenv](https://github.com/cachix/devenv/blob/fc49bf8b75b747aba83df74c9b6406c9f4a65614/docs/getting-started.md#installation).

1. Install [direnv](https://github.com/cachix/devenv/blob/fc49bf8b75b747aba83df74c9b6406c9f4a65614/docs/automatic-shell-activation.md#installing-direnv).

1. Install [flite](https://github.com/dmort27/epitran/blob/cb61a07cf6f17eea8daaf15923628483f0c70526/README.md#installation-of-flite-for-english-g2p).

1. Run the following commands:

   ```sh
   git clone git@github.com:8ta4/pun.git
   cd pun
   direnv allow
   download-pun
   ```

## Usage

> How do I generate puns?

1. Ensure you are on macOS

1. Copy your list of keywords you want puns for to the clipboard

1. Pop open a terminal

1. Run this:

```bash
pbpaste | pun
```
