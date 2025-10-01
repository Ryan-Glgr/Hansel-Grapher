# Makefile: simple, compiles all Java sources together (supports packages)
DEBUG ?= 0

SRC := src
BIN := bin
OUT := out

JAVAS := $(shell find $(SRC) -name '*.java')

.PHONY: all run clean

all: $(BIN) $(OUT) compile

$(BIN):
	mkdir -p $(BIN)

$(OUT):
	mkdir -p $(OUT)

# Compile all sources together. This resolves inter-file dependencies and supports packages.
# We recompile when any .java changes (simple and reliable).
compile: $(JAVAS)
	@echo "Compiling Java sources..."
ifeq ($(DEBUG),1)
	javac -d $(BIN) $(JAVAS)
else
	javac -d $(BIN) $(JAVAS) >/dev/null 2>&1 || (echo "javac failed; run 'make DEBUG=1' to see errors" && false)
endif

# DOT rendering rule: write errors to out/dot_errors.log unless DEBUG=1
ifeq ($(DEBUG),1)
$(OUT)/%.pdf: %.dot | $(OUT)
	@echo "Rendering $< -> $@"
	dot -Tpdf $< -o $@
else
$(OUT)/%.pdf: %.dot | $(OUT)
	@echo "Rendering $< -> $@"
	dot -Tpdf $< -o $@ 2>> $(OUT)/dot_errors.log || true
endif

# Convenience run (adjust MAIN if needed)
MAIN := Main
run: all
	@echo "Running: java -cp $(BIN) $(MAIN)"
	java -cp $(BIN) $(MAIN)

clean:
	rm -rf $(BIN) $(OUT)
	rm -f *.pdf
