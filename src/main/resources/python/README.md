# Python Oracle for Interview Questions

## Structure

```
src/main/resources/python/
├── interview.py          # Main oracle script
└── README.md            # This file
```

## How It Works

1. **Java launches Python once** via `PythonInterpreter.java`
2. **Python loads ML model once** at startup (expensive operation done once)
3. **Java sends questions** via stdin as JSON: `{"values": [[1,2,3], [4,5,6]]}`
4. **Python responds** via stdout as JSON: `{"predictions": [0, 1]}`
5. **Process stays alive** for entire interview (N questions, 1 process)

## Communication Protocol

### Input (Java → Python):
```json
{"values": [[1,2,3], [4,5,6], [7,8,9]]}
```

### Output (Python → Java):
```json
{"predictions": [0, 1, 2]}
```

## Adding ML Models

```python
def load_model(model_type: MLModel, dataset_path: str):
    if model_type == MLModel.NEURAL_NETWORK:
        import torch
        return torch.load(f"{dataset_path}/model.pt")
    elif model_type == MLModel.DECISION_TREE:
        import joblib
        return joblib.load(f"{dataset_path}/tree.pkl")
    # ... etc

def predict(model, node_values: list) -> int:
    # Use your loaded model to predict
    return model.predict([node_values])[0]
```

## Build Process

**No Gradle changes needed!** Python scripts are treated as resources:
- Gradle automatically copies `src/main/resources/` to build output
- Java launches Python at runtime via `ProcessBuilder`
- No compilation or special build steps required