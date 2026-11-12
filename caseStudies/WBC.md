## Wisconsin Breast Cancer (9 Dimensional Dataset)
Our case study into the Wisconsin Breast Cancer (9D) Dataset [CITATION] is used to explore the potential of using Black Box models to create interpretable MOEKA rules. In this case study, we have trained a Monotone Neural Network [LINK TO WHERE THE MNN COMES FROM] as our 
"expert predictor" in place of a real Subject-Matter Expert. This allows us to expand our experimentation to a much larger model size than is feasible with an SME. In this dataset, we are given 9 Attributes which are numbered
values from 1-10. the Benign class is class 0, and Malignant class is class 1, and thus increasing the values of a given attribute should make the case more likely to be a Malignant form of Cancer by the monotonicity principle. There are 699 total cases in this training data, 458 datapoints of Benign class, and 241 of Malignant class. 16 Of these
training cases are dropped because they are missing attributes, this reduces the dataset which we use to 444 benign cases, 239 malignant, for 683 total.

Because MOEKA techniques map the entire feature space, this 9 dimensional dataset with 10 possible values generates 10^9 possible Nodes to explore. At this size, we create one billion Nodes. It is a current implementation challenge to be able to process such a high dimensionality. 
This work makes progress on this challenge, allowing us to consider the dataset with attributes truncated into 4 unique values rather than 10. This allows us to reduce the 
total size of the feature space to 4^9, or 262,144 Nodes which is newly tractable with our updated techniques. The methodology to map attributes was done by bucketing the unique values 1-10, mapping as follows: 

| Original values | Bucket index |
|-----------------|--------------|
| 1, 2, 3         | 0            |
| 4, 5, 6         | 1            |
| 7, 8, 9         | 2            |
| 10              | 3            |

### Monotone Neural Network:
The underlying black-box model is an important part of this study. Thus we should discuss the prediction performance of this model, before going further. Only then can we justify how useful our MOEKA model is. Training the 
Monotone Neural Network on the normalized values as mapped with [TABLE NUMBER], the MNN achieves training results given in [TABLES CITATIONS]. Our MOEKA model is then derived by querying invididual points one by one, the same way that an
SME would conduct such an interview, querying one Node which is believed to be the most optimal in terms of information gain, updating all Nodes which we have learned information for, and repeating.

### MNN Derived MOEKA Model: 
This MOEKA model is an example where we have found both Inclusive Low Units, alongside Exclusive Low Units. For this experiment, we find 255 total Low Units, where 151 are inclusive, and 104 are exclusive. From the
Inclusive Low Units, we needed 247 literals in our compressed DNF, and from the Exclusive Low Units, we find 255 literals in our compressed DNF. This brings us to a total of 502 literals (or clauses) to represent the decision function across the feature space.
Importantly, we are not just covering the 683 training cases, but rather the entire feature space of **all possible** combinations of attributes which a datapoint can fall into. This spans 4^9 (262144) possible cases for this dataset. It is not meaningful, or discouraging to us that we have
used 502 literals in our DNF prediction function, for a meager 683 datapoints, because our goal is to predict all possible values in the feature space, of which the training data is a very small subset.

### Justification: 
This result shows us that we can simplify even a black box model in a very large feature space into a tiny fraction of that size. We were able to create an explainable decision function which uses just one clause for every 522.19 possible values in this feature space. 
Using an explainable model allows confidence from an SME that the prediction is not only correct, but explains exactly **why** that prediction is being made. At a size of a few Low Units, it is trivial for an SME to be shown the nearest Low Units which helped to inform a decision
and determine whether they trust a given prediction. If no Low Units are near a given case in the feature space, or a given case is too near a decision boundary, that can also allow an SME to know when to apply their own judgement, a black box model would again struggle to warn an SME of this issue.

#### Evaluation Summary:

| Metric                 | Value  |
|------------------------|--------|
| Test Score             | 0.9708 |
| CV Mean                | 0.9693 |
| CV Std Dev             | 0.0308 |
| Accuracy               | 0.97   |
| Macro Avg Precision    | 0.97   |
| Macro Avg Recall       | 0.97   |
| Macro Avg F1           | 0.97   |
| Weighted Avg Precision | 0.97   |
| Weighted Avg Recall    | 0.97   |
| Weighted Avg F1        | 0.97   |

#### Class-wise Metrics:

| Class | Precision | Recall | F1-Score | Support |
|-------|-----------|--------|----------|---------|
| 0     | 0.97      | 0.97   | 0.97     | 79      |
| 1     | 0.97      | 0.97   | 0.97     | 58      |

#### Cross‑Validation Fold Accuracies:

| Fold | Accuracy |
|------|----------|
| 1    | 0.8986   |
| 2    | 0.9855   |
| 3    | 0.9855   |
| 4    | 0.9412   |
| 5    | 0.9559   |
| 6    | 1.0000   |
| 7    | 0.9559   |
| 8    | 0.9706   |
| 9    | 1.0000   |
| 10   | 1.0000   |

#### RESULTING DNFs: 
```
{
  "inclusiveRuleset": [
    {
      "attributeIndex": 7,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 3
        }
      ]
    },
    {
      "attributeIndex": 7,
      "attributeValue": 2,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 2
        }
      ]
    },
    {
      "attributeIndex": 7,
      "attributeValue": 3,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 1
        }
      ]
    },
    {
      "attributeIndex": 0,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 3
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 3
        }
      ]
    },
    {
      "attributeIndex": 0,
      "attributeValue": 2,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 2
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 2
        }
      ]
    },
    {
      "attributeIndex": 0,
      "attributeValue": 3,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 1
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1
        }
      ]
    },
    {
      "attributeIndex": 3,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 3
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 3,
      "attributeValue": 2,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 2
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 2
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 7,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 2
        }
      ]
    },
    {
      "attributeIndex": 3,
      "attributeValue": 3,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 1
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1
        }
      ]
    },
    {
      "attributeIndex": 6,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 3
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 3
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            },
            {
              "attributeIndex": 7,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 7,
              "attributeValue": 3
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                },
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 6,
      "attributeValue": 2,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 2
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 3
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 7,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 2
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 2
        }
      ]
    },
    {
      "attributeIndex": 6,
      "attributeValue": 3,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 1
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1
        }
      ]
    },
    {
      "attributeIndex": 2,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 3
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 3
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 2
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            },
            {
              "attributeIndex": 3,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 3,
          "inclusiveRuleset": [
            {
              "attributeIndex": 3,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 3
            },
            {
              "attributeIndex": 3,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 3,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            },
            {
              "attributeIndex": 8,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 0,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 2
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 2,
      "attributeValue": 2,
      "inclusiveRuleset": [
        {
          "attributeIndex": 7,
          "attributeValue": 3
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 3
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 2
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 6,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 3
        },
        {
          "attributeIndex": 8,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 6,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 2
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 2,
      "attributeValue": 3,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 2
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 2
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 7,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 2
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 7,
              "attributeValue": 1
            },
            {
              "attributeIndex": 3,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 2
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 5,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 7,
          "attributeValue": 3
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 3
        },
        {
          "attributeIndex": 2,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 2
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 6,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 8,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1
            },
            {
              "attributeIndex": 2,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 8,
          "attributeValue": 2
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 7,
              "attributeValue": 1
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1
            },
            {
              "attributeIndex": 2,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 2
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 2
            },
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 2
            },
            {
              "attributeIndex": 8,
              "attributeValue": 1
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 5,
      "attributeValue": 2,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 1
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1
        },
        {
          "attributeIndex": 2,
          "attributeValue": 1
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1
        }
      ]
    },
    {
      "attributeIndex": 5,
      "attributeValue": 3
    },
    {
      "attributeIndex": 4,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 2,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 3,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 3,
              "attributeValue": 3
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 3,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 3,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 3,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 6,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 7,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 8,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 7,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 6,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 4,
      "attributeValue": 2,
      "inclusiveRuleset": [
        {
          "attributeIndex": 5,
          "attributeValue": 2
        },
        {
          "attributeIndex": 0,
          "attributeValue": 3
        },
        {
          "attributeIndex": 2,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 5,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 5,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 4,
      "attributeValue": 3,
      "inclusiveRuleset": [
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 5,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "exclusiveRuleset": [
    {
      "attributeIndex": 6,
      "attributeValue": 3,
      "exclusiveRuleset": [
        {
          "attributeIndex": 7,
          "attributeValue": 1
        }
      ]
    },
    {
      "attributeIndex": 4,
      "attributeValue": 1,
      "exclusiveRuleset": [
        {
          "attributeIndex": 6,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 2
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 4,
      "attributeValue": 3,
      "exclusiveRuleset": [
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 3
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 3,
      "attributeValue": 1,
      "exclusiveRuleset": [
        {
          "attributeIndex": 6,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 4,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 3
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 2
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 3,
      "attributeValue": 3,
      "exclusiveRuleset": [
        {
          "attributeIndex": 6,
          "attributeValue": 1
        }
      ]
    },
    {
      "attributeIndex": 2,
      "attributeValue": 1,
      "exclusiveRuleset": [
        {
          "attributeIndex": 4,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 3
            },
            {
              "attributeIndex": 3,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 3,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 7,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 3,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 4,
          "attributeValue": 3,
          "exclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 3,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 6,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 2,
      "attributeValue": 3,
      "exclusiveRuleset": [
        {
          "attributeIndex": 6,
          "attributeValue": 2
        },
        {
          "attributeIndex": 4,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 0,
      "attributeValue": 1,
      "exclusiveRuleset": [
        {
          "attributeIndex": 2,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 2
            },
            {
              "attributeIndex": 3,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 3,
              "attributeValue": 2
            },
            {
              "attributeIndex": 7,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 3,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 3,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 0,
      "attributeValue": 2,
      "exclusiveRuleset": [
        {
          "attributeIndex": 4,
          "attributeValue": 3,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 1
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 8,
      "attributeValue": 1,
      "exclusiveRuleset": [
        {
          "attributeIndex": 2,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 3,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 3,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 2
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 8,
      "attributeValue": 2,
      "exclusiveRuleset": [
        {
          "attributeIndex": 2,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 3
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 2
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 1,
      "attributeValue": 1,
      "exclusiveRuleset": [
        {
          "attributeIndex": 0,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 4,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 3
            }
          ]
        },
        {
          "attributeIndex": 4,
          "attributeValue": 3,
          "exclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 3
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 0,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 0,
                  "attributeValue": 2
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 4,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 3
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1
                    },
                    {
                      "attributeIndex": 0,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 2,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 6,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 3,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 6,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 3
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 8,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 6,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 0,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 2
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 3,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 3
            },
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 7,
                      "attributeValue": 2
                    },
                    {
                      "attributeIndex": 6,
                      "attributeValue": 2
                    }
                  ]
                },
                {
                  "attributeIndex": 4,
                  "attributeValue": 2,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 6,
                      "attributeValue": 1,
                      "exclusiveRuleset": [
                        {
                          "attributeIndex": 7,
                          "attributeValue": 1
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 3,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 8,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 8,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3
                }
              ]
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1
                },
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 8,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 1,
      "attributeValue": 2,
      "exclusiveRuleset": [
        {
          "attributeIndex": 4,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 3
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 3
        },
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            },
            {
              "attributeIndex": 4,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 2
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 3,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 2,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 3,
          "exclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 4,
              "attributeValue": 3,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 7,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 6,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 7,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 0,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 4,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 0,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 6,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 1,
                      "exclusiveRuleset": [
                        {
                          "attributeIndex": 7,
                          "attributeValue": 1
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 3,
          "attributeValue": 3,
          "exclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 5,
      "attributeValue": 1,
      "exclusiveRuleset": [
        {
          "attributeIndex": 3,
          "attributeValue": 2
        },
        {
          "attributeIndex": 1,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 1,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 3,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 4,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 6,
              "attributeValue": 2
            },
            {
              "attributeIndex": 3,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 6,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 1,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 3,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 1,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 3,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 6,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 2,
          "attributeValue": 3,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 2
            },
            {
              "attributeIndex": 1,
              "attributeValue": 2,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 3,
              "attributeValue": 1
            },
            {
              "attributeIndex": 1,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 6,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 3
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 8,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 1,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "exclusiveRuleset": [
            {
              "attributeIndex": 1,
              "attributeValue": 1,
              "exclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "exclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 5,
      "attributeValue": 2,
      "exclusiveRuleset": [
        {
          "attributeIndex": 1,
          "attributeValue": 2,
          "exclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 1
            }
          ]
        }
      ]
    }
  ]
}

```

Found: [255] total low units. [151] were inclusive, [104] were exclusive.INTERVIEW COMPLETE!
BEST_MINIMUM_CONFIRMED MODE
TOTAL NUMBER OF NODES: 262144

TOTAL NUMBER OF CHAINS: 30276

NUMBER OF QUESTIONS ASKED: 3093

TOTAL NUMBER OF ADJUSTED LOW UNITS:     255

TOTAL NUMBER OF INCLUSIVE CLAUSES NEEDED:       247
TOTAL NUMBER OF EXCLUSIVE CLAUSES NEEDED:       255


