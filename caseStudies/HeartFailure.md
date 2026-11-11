

#### Question Minimization Results:

| Technique - Tiebreaker                                    | Questions |
|-----------------------------------------------------------|----------:|
| Traditional Binary Search - None                          |  **1587** |
| Traditional Binary Search - Balance Ratio Unity           |      1627 |
| Traditional Binary Search - Best Min Confirmed            |      1653 |
| Traditional Binary Search - Balance Ratio Shannon Entropy |      1653 |
| Traditional Binary Search - Smallest Difference           |      1709 |
| Traditional Binary Search - Highest Total Umbrella Sort   |      1786 |
| Standalone - Balance Ratio Quadratic                      |      1840 |
| Traditional Binary Search - Balance Ratio Quadratic       |      1857 |
| Binary Search Chunks - Smallest Difference                |      1936 |
| Nonbinary Search - Smallest Difference                    |      1936 |
| Binary Search Chunks - Balance Ratio Unity                |      2055 |
| Nonbinary Search - Balance Ratio Unity                    |      2055 |
| Binary Search Chunks - Best Min Confirmed                 |      2119 |
| Binary Search Chunks - Balance Ratio Shannon Entropy      |      2119 |
| Nonbinary Search - Best Min Confirmed                     |      2119 |
| Nonbinary Search - Balance Ratio Shannon Entropy          |      2119 |
| Standalone - Best Min Confirmed                           |      2121 |
| Binary Search Chunks - Balance Ratio Quadratic            |      2226 |
| Nonbinary Search - Balance Ratio Quadratic                |      2226 |
| Binary Search Chunks - Highest Total Umbrella Sort        |      2297 |
| Nonbinary Search - Highest Total Umbrella Sort            |      2297 |
| Binary Search Chunks - None                               |      2364 |
| Nonbinary Search - Chains                                 |      2364 |
| Standalone - Balance Ratio Unity                          |      2448 |
| Standalone - Smallest Difference Umbrella Sort            |      2494 |
| Longest String of Expansions - None                       |      3036 |
| Standalone - Highest Total Umbrella Sort                  |      3606 |
| Standalone - Balance Ratio Shannon Entropy                |      3797 |

#### Question Count Improvement:

Our results are much improved in terms of queries for the following reason: MOEKA 1.0 did not allow the information of Hansel Chain's Node being confirmed, to update Nodes in other chains.

This means that in MOEKA 1.0, essentially we always had to perform the binary search on every Hansel Chain, whereas now, we can use information derived in one chain to inform the decision making of another chain. 

This result shows the power of this optimization. We see in the table that in this dataset, a Traditional Binary Search was clearly the best query technique, and in fact the method which uses no special tiebreaking performs the best. The binary search, using the aforementioned optimization functions as follows:

1. Begin with all Hansel Chains, all nodes are considered to be able to take on either class 0 - No Heart Failure, or class 1 - Heart Failure.
2. Choose the middlepoint of the longest existing Hansel Chain, and query that Node, Node X. 
3. Once Node X's classification is known, iterate remaining Nodes in dataset with the following procedure:
- For a Node Y which is NOT Node X in the dataset, if Y is < X in all attributes, we know that now Y's maximum possible classification is whatever classification Node X has taken on. 
- If Node Y is strictly greater than Node X in all attributes, we know that it must be AT LEAST Node X's new classification.
- If Node Y is not strictly greater or less than Node X, we have learned nothing about it in this turn.
- If Node Y has updated, and the lowest possible value for Node Y is now = it's greatest possible value, we have confirmed it's exact classification without ever querying Node Y directly, by inferring via the properties of monotonicity.
4. Iterate all Hansel Chains in the dataset, and update to slice those chains in half which have had a Node confirmed in this step. Thus the new list of Hansel Chains may be 2x as many chains as the previous, with each being half their previous length (in the worst case).
5. Repeat from Step 2.

#### Number of Low Units:
For this result, our MOEKA model was directly trained by using the previous MOEKA program's reduced MBF given in [HARLOW's THESIS CITATION APPENDIX]. This allows us to ensure that our result is identical in terms of outputs. However, our MOEKA 2.0 is able to find less Low Units than the previous work, by a score of 116 nD points as compared to 137 nD points.

We have a new important distinction in the Low Units definition which has allowed us to create this result. Generally, a Low Unit would be defined as that point in a Hansel Chain where a classification transitions, in this case the lowest datapoint where we see a transition from No Heart Failure, to Heart Failure. 

This definition is not wrong, but rather, too permissive. It can be the situation (and often is) that the nD where we see this transition happen, is strictly greater than or equal to another Low Unit of the very same classification, which happens to lie in another Hansel Chain. 

Thus by tightening the definition of Low Unit in this way, we are able to remove 21 Low Units from this model while maintaining the exact same decision boundary.

#### Size of Final DNF:
Finally, the most important result we can show as an improvement against the previous MOEKA 1.0 techniques is our reduction in the size of the produced DNF.

While still reproducing the exact same decision function for this feature space, we were able to use 307 clauses in our factored DNF.

This is driven by two factors. First and more simply, the fact that we reduced the number of Low Units which we are putting into the DNF as we have detailed. Secondly, we have introduced a logic which parses and creates a decision tree greedily based on those values which are shared by multiple Low Units. 

This algorithm seeks out those values of a given attribute which are repeated the most often, and will factor it out, thus allowing us to have a situation like this:
Low Units:
(3, 3, 1, 0),
(3, 3, 0, 5)
`X1 >= 3 & X2 >= 3 & ((X3 >= 1 & X4 >= 0) || (X4 >= 5))`

In this example, we would reduce the final DNF size from 6 to 5 by factoring out the shared value of X1 and X2. The savings of this technique compound as more low units share values.

These two techniques in combination allow us to produce a much smaller DNF for analysis, and can be easily visualized. 

#### DNF: 
```json
{
  "inclusiveRuleset": [
    {
      "attributeIndex": 11,
      "attributeValue": 2,
      "inclusiveRuleset": [
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 3
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
      "attributeIndex": 8,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 11,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 3
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
          "attributeIndex": 11,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
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
      "attributeIndex": 7,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 11,
          "attributeValue": 2
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
                  "attributeIndex": 11,
                  "attributeValue": 1
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 11,
              "attributeValue": 1
            },
            {
              "attributeIndex": 2,
              "attributeValue": 3
            }
          ]
        },
        {
          "attributeIndex": 8,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 3
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2
            },
            {
              "attributeIndex": 11,
              "attributeValue": 1
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 4,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 11,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 11,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 3
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1
            }
          ]
        },
        {
          "attributeIndex": 8,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 11,
              "attributeValue": 2
            },
            {
              "attributeIndex": 2,
              "attributeValue": 3,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 11,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 11,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 3
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 1
                }
              ]
            },
            {
              "attributeIndex": 11,
              "attributeValue": 1,
              "inclusiveRuleset": [
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
              "attributeIndex": 8,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
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
      "attributeIndex": 9,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 8,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 11,
              "attributeValue": 2
            }
          ]
        },
        {
          "attributeIndex": 0,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 11,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 8,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
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
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 11,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 3
                },
                {
                  "attributeIndex": 0,
                  "attributeValue": 1
                }
              ]
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
          "attributeIndex": 4,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 11,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 1
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
                  "attributeIndex": 0,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 3
                    }
                  ]
                },
                {
                  "attributeIndex": 0,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 11,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 7,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 11,
                  "attributeValue": 1
                },
                {
                  "attributeIndex": 8,
                  "attributeValue": 1
                },
                {
                  "attributeIndex": 0,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 0,
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
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 4,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 11,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 8,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
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
                  "attributeIndex": 11,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 8,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 9,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 2
                    },
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
                      "attributeIndex": 8,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
                  "attributeIndex": 11,
                  "attributeValue": 2
                },
                {
                  "attributeIndex": 8,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 0,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 1
                    },
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 0,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
          "attributeIndex": 7,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 4,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1
                },
                {
                  "attributeIndex": 2,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 11,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 3
                }
              ]
            },
            {
              "attributeIndex": 9,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 1
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
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 9,
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
      "attributeIndex": 1,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 2,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 9,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "attributeIndex": 11,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 7,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 9,
                      "attributeValue": 1
                    }
                  ]
                },
                {
                  "attributeIndex": 2,
                  "attributeValue": 3
                }
              ]
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 9,
                          "attributeValue": 1
                        }
                      ]
                    },
                    {
                      "attributeIndex": 2,
                      "attributeValue": 2
                    }
                  ]
                }
              ]
            }
          ]
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
                  "attributeIndex": 9,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 2
                    },
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 8,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
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
                  "attributeValue": 3,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 9,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
              "inclusiveRuleset": [
                {
                  "attributeIndex": 0,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
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
          "attributeIndex": 5,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 11,
                  "attributeValue": 2
                }
              ]
            },
            {
              "attributeIndex": 2,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 9,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 0,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
                          "attributeValue": 2
                        },
                        {
                          "attributeIndex": 4,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
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
                      "attributeValue": 2,
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
            }
          ]
        }
      ]
    },
    {
      "attributeIndex": 3,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 0,
          "attributeValue": 2,
          "inclusiveRuleset": [
            {
              "attributeIndex": 1,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
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
          "attributeIndex": 9,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 8,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
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
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 3
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 1,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 0,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
                          "attributeValue": 2
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
          "attributeIndex": 4,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 1,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 7,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 9,
                              "attributeValue": 1
                            }
                          ]
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
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 9,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 0,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 4,
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
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 0,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
                          "attributeValue": 2
                        },
                        {
                          "attributeIndex": 4,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
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
                      "attributeValue": 2,
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
              "attributeIndex": 1,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 7,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 2,
                          "attributeValue": 2
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
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 7,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
                          "attributeValue": 1
                        },
                        {
                          "attributeIndex": 8,
                          "attributeValue": 1
                        }
                      ]
                    },
                    {
                      "attributeIndex": 0,
                      "attributeValue": 2,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 2,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 11,
                              "attributeValue": 1
                            },
                            {
                              "attributeIndex": 8,
                              "attributeValue": 1
                            }
                          ]
                        }
                      ]
                    }
                  ]
                },
                {
                  "attributeIndex": 9,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 7,
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
                      "attributeIndex": 0,
                      "attributeValue": 2,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 4,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 11,
                              "attributeValue": 1
                            }
                          ]
                        }
                      ]
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
      "attributeIndex": 10,
      "attributeValue": 1,
      "inclusiveRuleset": [
        {
          "attributeIndex": 3,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 2,
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 2
                    },
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
                  "attributeIndex": 7,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 11,
                      "attributeValue": 1
                    }
                  ]
                }
              ]
            },
            {
              "attributeIndex": 1,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 7,
                      "attributeValue": 1,
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
            },
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 8,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
                          "attributeValue": 1
                        }
                      ]
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
                  "attributeIndex": 0,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 1,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 7,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 11,
                              "attributeValue": 1
                            }
                          ]
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
          "attributeIndex": 9,
          "attributeValue": 1,
          "inclusiveRuleset": [
            {
              "attributeIndex": 0,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
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
                    },
                    {
                      "attributeIndex": 2,
                      "attributeValue": 3,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
              "attributeValue": 2,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
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
              "attributeIndex": 3,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 2,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 5,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 7,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 11,
                              "attributeValue": 1
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "attributeIndex": 4,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
                          "attributeValue": 1
                        }
                      ]
                    }
                  ]
                },
                {
                  "attributeIndex": 0,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 3,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
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
                  "attributeIndex": 0,
                  "attributeValue": 2,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 4,
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
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 3,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 11,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 4,
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
                }
              ]
            },
            {
              "attributeIndex": 1,
              "attributeValue": 1,
              "inclusiveRuleset": [
                {
                  "attributeIndex": 4,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 2,
                      "attributeValue": 3,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 7,
                          "attributeValue": 1
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
                              "attributeValue": 1
                            }
                          ]
                        },
                        {
                          "attributeIndex": 2,
                          "attributeValue": 2,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 5,
                              "attributeValue": 1,
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
                  ]
                },
                {
                  "attributeIndex": 3,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 5,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 0,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 2,
                              "attributeValue": 3,
                              "inclusiveRuleset": [
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
                          "attributeValue": 2,
                          "inclusiveRuleset": [
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
                  ]
                },
                {
                  "attributeIndex": 11,
                  "attributeValue": 1,
                  "inclusiveRuleset": [
                    {
                      "attributeIndex": 0,
                      "attributeValue": 1,
                      "inclusiveRuleset": [
                        {
                          "attributeIndex": 3,
                          "attributeValue": 1,
                          "inclusiveRuleset": [
                            {
                              "attributeIndex": 5,
                              "attributeValue": 1,
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
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```
TOTAL NUMBER OF ADJUSTED LOW UNITS:     116
TOTAL NUMBER OF INCLUSIVE CLAUSES NEEDED:       307
