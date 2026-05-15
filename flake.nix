{
  description = "Nix environment for Visualising Data Structures (Java 25)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, utils }:
    utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        jdk21 = pkgs.openjdk21;
        jdk25 = pkgs.openjdk25;
        javaHome21 = "${jdk21}/lib/openjdk";
        javaHome25 = "${jdk25}/lib/openjdk";

        # Define the run script as a package
        runVisualiser = pkgs.writeShellScriptBin "visualiser" ''
          export JAVA_HOME=${javaHome21}
          ./gradlew \
            -Porg.gradle.java.installations.paths=${javaHome25} \
            -Porg.gradle.java.installations.auto-download=false \
            run "$@"
        '';
      in
      {
        # This allows 'nix run'
        packages.default = runVisualiser;

        # This allows 'nix develop'
        devShells.default = pkgs.mkShell {
          buildInputs = [
            jdk21
            jdk25
            pkgs.jdt-language-server
          ];

          shellHook = ''
            export JAVA_HOME=${javaHome21}
            export PATH="${jdk21}/bin:${jdk25}/bin:$PATH"
            export JDK25_PATH="${javaHome25}"

            alias gradle='./gradlew -Porg.gradle.java.installations.paths=${javaHome25} -Porg.gradle.java.installations.auto-download=false'

            echo "❄️  Nix Environment Active"
            echo "❄️  Type 'gradle' (wrapper) to build or 'nix run' to execute"
          '';
        };
      });
}
