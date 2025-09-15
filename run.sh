rm debug

cd src/
javac *.java
java Main.java
cat debug

echo "Compiling PDFs\n"
dot -Tpdf expansions.dot -o expansions.pdf
rm expansions.dot
mv expansions.pdf ../

dot -Tpdf HanselChains.dot -o HanselChains.pdf
rm HanselChains.dot
mv HanselChains.pdf ../

cd ..
open *.pdf

