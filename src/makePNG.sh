java Main.java
dot -Tpng Expansions.dot -o expansions.png
dot -Tpng HanselChains.dot -o chains.png
rm Expansions.dot
rm HanselChains.dot
open *.png