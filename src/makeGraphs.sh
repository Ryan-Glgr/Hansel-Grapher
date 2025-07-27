#!/bin/bash
dot -Tpdf Expansions.dot -o expansions.pdf
dot -Tpdf HanselChains.dot -o chains.pdf
rm Expansions.dot
rm HanselChains.dot
open *.pdf