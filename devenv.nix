{
  pkgs,
  lib,
  config,
  inputs,
  ...
}:

{
  # https://devenv.sh/basics/
  env.GREET = "devenv";

  # https://devenv.sh/packages/
  packages = [
    pkgs.ghcid
    pkgs.git
    pkgs.gitleaks
    pkgs.wget
  ];

  # https://devenv.sh/languages/
  # languages.rust.enable = true;
  languages.clojure.enable = true;
  languages.haskell = {
    enable = true;
    stack.enable = true;
  };
  languages.python = {
    enable = true;
    uv.enable = true;
    uv.sync.enable = true;
  };

  # https://devenv.sh/processes/
  # processes.cargo-watch.exec = "cargo-watch";

  # https://devenv.sh/services/
  # services.postgres.enable = true;

  # https://devenv.sh/scripts/
  scripts.download-wiktionary.exec = scripts/download-wiktionary.sh;
  scripts.download-pun.exec = scripts/download-pun.sh;
  scripts.hello.exec = ''
    echo hello from $GREET
  '';
  scripts.install.exec = ''
    cd "$DEVENV_ROOT/hs" && stack install
  '';
  scripts.pun.exec = ''
    cd "$DEVENV_ROOT/hs" && stack run -- pun "$@"
  '';
  scripts.watch.exec = ''
    cd "$DEVENV_ROOT/hs" && ghcid -a \
    --no-height-limit \
    -r \
    -s ":set args $@" \
    -s ':set -Wprepositive-qualified-module' \
    -W
  '';

  enterShell = ''
    hello
    git --version
    # https://github.com/astral-sh/uv/blob/e006a69fe83808d5eaebaa27f535914cf1b36105/docs/guides/projects.md?plain=1#L231
    source .venv/bin/activate
  '';

  # https://devenv.sh/tasks/
  # tasks = {
  #   "myproj:setup".exec = "mytool build";
  #   "devenv:enterShell".after = [ "myproj:setup" ];
  # };

  # https://devenv.sh/tests/
  enterTest = ''
    echo "Running tests"
    git --version | grep --color=auto "${pkgs.git.version}"
  '';

  # https://devenv.sh/git-hooks/
  # git-hooks.hooks.shellcheck.enable = true;
  git-hooks.hooks = {
    cljfmt.enable = true;
    gitleaks = {
      enable = true;
      # https://github.com/gitleaks/gitleaks/blob/6f967cad68d7ce015f45f4545dca2ec27c34e906/.pre-commit-hooks.yaml#L4
      # Direct execution of gitleaks here results in '[git] fatal: cannot change to 'devenv.nix': Not a directory'.
      entry = "bash -c 'exec gitleaks git --redact --staged --verbose'";
    };
    # https://github.com/NixOS/nixfmt/blob/1acdae8b49c1c5d7f22fed7398d7f6f3dbce4c8a/README.md?plain=1#L16
    nixfmt-rfc-style.enable = true;
    ormolu.enable = true;
    prettier.enable = true;
    shellcheck.enable = true;
    # https://github.com/cachix/git-hooks.nix/issues/31#issuecomment-744657870
    trailing-whitespace = {
      enable = true;
      # https://github.com/pre-commit/pre-commit-hooks/blob/5c514f85cc9be49324a6e3664e891ac2fc8a8609/.pre-commit-hooks.yaml#L205-L212
      entry = "${pkgs.python3Packages.pre-commit-hooks}/bin/trailing-whitespace-fixer";
      types = [ "text" ];
    };
  };

  # See full reference at https://devenv.sh/reference/options/
}
