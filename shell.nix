{ pkgs ? import <nixpkgs> {} }:

with pkgs;
let yosys-slang =
      stdenv.mkDerivation (finalAttrs: {
        pname = "yosys-slang";
        version = "64b44616a3798f07453b14ea03e4ac8a16b77313";

        src = fetchFromGitHub {
          owner  = "povik";
          repo   = "yosys-slang";
          rev    = "${finalAttrs.version}";
          sha256 = "sha256-kfu59/M3+IM+5ZMd+Oy4IZf4JWuVtPDlkHprk0FB8t4=";
          fetchSubmodules = true;
        };

        buildInputs = [
          yosys
          cmake
          python3
        ];

        installPhase = ''
           mkdir -p $out/lib
           cp slang.so $out/lib/slang.so
        '';
      });
in
mkShell {
  packages = [
    gnumake boost zlib patchelf mill verilator haskellPackages.sv2v
    yosys yosys-slang nextpnr python313Packages.apycula openfpgaloader
  ];

  shellHook = ''
    ## Shell name
    export NIX_SHELL_NAME="[hny2026]"
    export YOSYS_SLANG_SO="${yosys-slang}/lib/slang.so"

    find $HOME/.cache/llvm-firtool/ -type f -executable -exec patchelf --set-interpreter ${pkgs.glibc}/lib64/ld-linux-x86-64.so.2 {} \;
    find $HOME/.cache/llvm-firtool/ -type f -executable -exec patchelf --add-needed ${zlib}/lib/libz.so.1 {} \;

    find $HOME/.cache/mill/ -type f -executable -exec patchelf --set-interpreter ${pkgs.glibc}/lib64/ld-linux-x86-64.so.2 {} \;
    find $HOME/.cache/mill/ -type f -executable -exec patchelf --add-needed ${zlib}/lib/libz.so.1 {} \;
  '';
}
