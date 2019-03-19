import json
import sys
import os
import pickle
import numpy as np
from extract_features import extract_features

T = 0.05

with open(sys.argv[1], 'r') as f:
    data = json.load(f)

modi_point = data['ModificationPoint']
ingredients = data['Ingredients']

features = extract_features(modi_point, ingredients)
X = []
for ingredient in features:
    X.append(features[ingredient])
X = np.array(X)

dir_path = os.path.dirname(os.path.realpath(__file__))
clf = pickle.load(open(os.path.join(dir_path, 'model_NN50d_1547464892.sav'), 'rb'))
proba = clf.predict_proba(X)

cands = [str(int(p[1] > T)) for p in proba]
"""
for i, c in enumerate(cands):
    if int(c) == 1:
        print(ingredients[i])
        print("===============================")
"""
with open(sys.argv[1].replace('Input', 'Output'), 'w') as f:
    f.write(','.join(cands))
